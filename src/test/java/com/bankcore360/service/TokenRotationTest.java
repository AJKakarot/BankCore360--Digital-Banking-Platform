package com.bankcore360.service;

import com.bankcore360.dto.AuthTokenResponse;
import com.bankcore360.dto.UserResponse;
import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.RefreshToken;
import com.bankcore360.entity.UserRole;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenRotationTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private FintechMetricsService fintechMetricsService;

    private TokenRotationService tokenRotationService;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        tokenRotationService = new TokenRotationService(refreshTokenRepository, fintechMetricsService);
        testUser = new AppUser("john_doe", "john@bank.com", "pass", UserRole.CUSTOMER);
        testUser.setId(101L);
    }

    @Test
    @DisplayName("Should successfully issue initial token pair on login")
    void testIssueTokenPair() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse userResponse = new UserResponse(101L, "john_doe", "john@bank.com", UserRole.CUSTOMER);
        AuthTokenResponse tokens = tokenRotationService.issueTokenPair(testUser, userResponse);

        assertNotNull(tokens);
        assertNotNull(tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        assertTrue(tokens.getAccessToken().startsWith("AT-john_doe-"));
        assertEquals(64, tokens.getRefreshToken().length()); // 32 hex bytes
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should rotate refresh token and issue new token pair within same family")
    void testRotateRefreshToken() {
        String rawToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String hashedToken = TokenRotationService.hashToken(rawToken);

        RefreshToken existing = new RefreshToken(testUser, hashedToken, "FAM-12345", LocalDateTime.now().plusDays(7));
        existing.setId(1L);

        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthTokenResponse rotated = tokenRotationService.rotateRefreshToken(rawToken);

        assertNotNull(rotated);
        assertNotNull(rotated.getAccessToken());
        assertNotNull(rotated.getRefreshToken());
        assertNotEquals(rawToken, rotated.getRefreshToken());
        assertTrue(existing.isRevoked()); // Old token revoked
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // 1 for revoking old, 1 for new
    }

    @Test
    @DisplayName("Should detect stolen token reuse and immediately revoke entire token family")
    void testCompromisedTokenReuseDetection() {
        String rawToken = "stolen_token_value_replayed_by_attacker";
        String hashedToken = TokenRotationService.hashToken(rawToken);

        // Token is ALREADY marked revoked (e.g. legitimate user already rotated it)
        RefreshToken compromisedToken = new RefreshToken(testUser, hashedToken, "FAM-CRITICAL-BREACH", LocalDateTime.now().plusDays(5));
        compromisedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(compromisedToken));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                tokenRotationService.rotateRefreshToken(rawToken)
        );

        assertTrue(ex.getMessage().contains("Compromised token reuse detected"));
        // Entire family revoked
        verify(refreshTokenRepository, times(1)).revokeAllInFamily("FAM-CRITICAL-BREACH");
        verify(fintechMetricsService, times(1)).recordTokenReuseDetected("john_doe");
    }
}

