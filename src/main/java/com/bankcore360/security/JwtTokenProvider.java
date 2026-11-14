package com.bankcore360.security;

import com.bankcore360.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Asymmetric RSA-256 JWT Token Provider.
 * Signs access tokens using an RSA private key and verifies incoming requests
 * using the corresponding RSA public key.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ISSUER = "BankCore360";
    private static final String AUDIENCE = "BankCore360-Client";

    @Value("${security.jwt.access-token-validity-seconds:900}")
    private long accessTokenValiditySeconds;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    public void initKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            log.info("Initialized in-memory 2048-bit RSA keypair for RS256 JWT access tokens");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA keypair generation failed", e);
        }
    }

    /**
     * Generates an asymmetric RSA-256 signed JWT access token for an authenticated user.
     */
    public String generateAccessToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenValiditySeconds, ChronoUnit.SECONDS);

        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole() != null ? user.getRole().name() : "CUSTOMER")
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Validates an RSA-256 signed token and extracts claims. Returns null if invalid or expired.
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token verification failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts username from valid RSA-signed JWT.
     */
    public String extractUsername(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extracts role from valid RSA-signed JWT.
     */
    public String extractRole(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }
}
