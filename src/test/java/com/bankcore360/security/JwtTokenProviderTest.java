package com.bankcore360.security;

import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValiditySeconds", 900L);
        jwtTokenProvider.initKeys();
    }

    @Test
    @DisplayName("Should generate valid RSA-256 signed JWT with correct claims")
    void shouldGenerateValidRsaJwt() {
        AppUser user = new AppUser("testcustomer", "customer@bank.com", "hash", UserRole.CUSTOMER);
        user.setId(42L);

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // standard header.payload.signature

        Claims claims = jwtTokenProvider.validateAndGetClaims(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testcustomer");
        assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("email", String.class)).isEqualTo("customer@bank.com");
        assertThat(claims.getIssuer()).isEqualTo("BankCore360");
    }

    @Test
    @DisplayName("Should reject tampered or corrupted JWT tokens")
    void shouldRejectTamperedToken() {
        AppUser user = new AppUser("testcustomer", "customer@bank.com", "hash", UserRole.CUSTOMER);
        user.setId(42L);
        String token = jwtTokenProvider.generateAccessToken(user);

        // Tamper with the token signature
        String tamperedToken = token.substring(0, token.length() - 5) + "ABCDE";

        Claims claims = jwtTokenProvider.validateAndGetClaims(tamperedToken);
        assertThat(claims).isNull();
    }

    @Test
    @DisplayName("Should extract username and role accurately")
    void shouldExtractUsernameAndRole() {
        AppUser user = new AppUser("manager1", "manager@bank.com", "hash", UserRole.MANAGER);
        user.setId(10L);

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("manager1");
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo("MANAGER");
    }
}
