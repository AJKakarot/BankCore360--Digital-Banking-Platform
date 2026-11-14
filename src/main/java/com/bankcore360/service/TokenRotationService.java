package com.bankcore360.service;

import com.bankcore360.dto.AuthTokenResponse;
import com.bankcore360.dto.UserResponse;
import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.RefreshToken;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Enterprise Refresh Token Rotation Service with automated reuse/theft detection.
 */
@Service
public class TokenRotationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRotationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 900; // 15 mins
    public static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final FintechMetricsService fintechMetricsService;
    private final com.bankcore360.security.JwtTokenProvider jwtTokenProvider;

    public TokenRotationService(RefreshTokenRepository refreshTokenRepository,
                                @org.springframework.beans.factory.annotation.Autowired(required = false)
                                FintechMetricsService fintechMetricsService,
                                @org.springframework.beans.factory.annotation.Autowired(required = false)
                                com.bankcore360.security.JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.fintechMetricsService = fintechMetricsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenRotationService(RefreshTokenRepository refreshTokenRepository,
                                FintechMetricsService fintechMetricsService) {
        this(refreshTokenRepository, fintechMetricsService, null);
    }

    /**
     * Issues an initial Access Token and Refresh Token pair upon successful login.
     */
    @Transactional
    public AuthTokenResponse issueTokenPair(AppUser user, UserResponse userResponse) {
        String tokenFamily = "FAM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String rawRefreshToken = generateSecureToken();
        String hashedToken = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                hashedToken,
                tokenFamily,
                LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        String accessToken = generateAccessToken(user);
        return new AuthTokenResponse(accessToken, rawRefreshToken, userResponse);
    }

    /**
     * Rotates a refresh token: invalidates old token, checks for reuse theft,
     * and issues a new access token + new refresh token in the same family.
     */
    @Transactional
    public AuthTokenResponse rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token cannot be blank");
        }

        String hashed = hashToken(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid or unrecognized refresh token"));

        // 1. REUSE DETECTION (Breach intercept):
        // If an already-revoked refresh token is presented, someone is replaying a stolen token!
        if (currentToken.isRevoked()) {
            log.error("🚨 CRITICAL SECURITY ALERT: Refresh token reuse detected for user {} in family {}. Possible credential theft!",
                    currentToken.getUser().getUsername(), currentToken.getTokenFamily());
            
            // Invalidate ALL tokens in this family immediately
            refreshTokenRepository.revokeAllInFamily(currentToken.getTokenFamily());
            
            if (fintechMetricsService != null) {
                fintechMetricsService.recordTokenReuseDetected(currentToken.getUser().getUsername());
            }

            throw new BadCredentialsException("Compromised token reuse detected. All active tokens in this session family have been permanently revoked.");
        }

        // 2. Expiration check:
        if (currentToken.isExpired()) {
            currentToken.setRevoked(true);
            refreshTokenRepository.save(currentToken);
            throw new BadCredentialsException("Refresh token has expired. Please re-authenticate.");
        }

        // 3. Mark current token as revoked
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // 4. Issue new refresh token within the same token family
        String newRawRefreshToken = generateSecureToken();
        String newHashedToken = hashToken(newRawRefreshToken);

        RefreshToken nextToken = new RefreshToken(
                currentToken.getUser(),
                newHashedToken,
                currentToken.getTokenFamily(),
                LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS)
        );
        refreshTokenRepository.save(nextToken);

        AppUser user = currentToken.getUser();
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        String newAccessToken = generateAccessToken(user);

        log.info("Successfully rotated refresh token for user {} in family {}", user.getUsername(), currentToken.getTokenFamily());
        return new AuthTokenResponse(newAccessToken, newRawRefreshToken, userResponse);
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hashed = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(hashed).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
        }
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    public String generateAccessToken(AppUser user) {
        if (jwtTokenProvider != null && user != null) {
            return jwtTokenProvider.generateAccessToken(user);
        }
        // Fallback bearer session token
        return "AT-" + (user != null ? user.getUsername() : "UNKNOWN") + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

