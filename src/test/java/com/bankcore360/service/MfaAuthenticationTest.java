package com.bankcore360.service;

import com.bankcore360.dto.MfaEnrollResponse;
import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.UserRole;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MfaAuthenticationTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FintechMetricsService fintechMetricsService;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private MfaService mfaService;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        mfaService = new MfaService(
                appUserRepository,
                passwordEncoder,
                stringRedisTemplate,
                fintechMetricsService
        );

        testUser = new AppUser("test_customer", "customer@bank.com", "encodedPass", UserRole.CUSTOMER);
        testUser.setId(10L);
    }

    @Test
    @DisplayName("Should enroll in MFA and generate 8 cryptographically hashed recovery codes")
    void testEnrollMfa() {
        when(appUserRepository.findByUsername("test_customer")).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        MfaEnrollResponse response = mfaService.enrollMfa("test_customer");

        assertNotNull(response);
        assertTrue(response.isMfaEnabled());
        assertEquals("OTP", response.getMfaType());
        assertEquals(8, response.getRecoveryCodes().size());
        assertTrue(testUser.isMfaEnabled());
        assertNotNull(testUser.getMfaRecoveryCodes());
        verify(appUserRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should successfully verify valid 6-digit OTP")
    void testVerifyValidOtp() {
        testUser.setMfaEnabled(true);
        when(appUserRepository.findByUsername("test_customer")).thenReturn(Optional.of(testUser));
        when(valueOperations.get(MfaService.TICKET_REDIS_PREFIX + "MFA-TICKET-123")).thenReturn("test_customer");
        when(valueOperations.get(MfaService.OTP_REDIS_PREFIX + "test_customer")).thenReturn("849201");

        AppUser verified = mfaService.verifyMfa("test_customer", "MFA-TICKET-123", "849201", false);

        assertNotNull(verified);
        assertEquals("test_customer", verified.getUsername());
        verify(valueOperations, times(1)).get(MfaService.OTP_REDIS_PREFIX + "test_customer");
    }

    @Test
    @DisplayName("Should reject invalid OTP with BadCredentialsException")
    void testRejectInvalidOtp() {
        testUser.setMfaEnabled(true);
        when(appUserRepository.findByUsername("test_customer")).thenReturn(Optional.of(testUser));
        when(valueOperations.get(MfaService.TICKET_REDIS_PREFIX + "MFA-TICKET-123")).thenReturn("test_customer");
        when(valueOperations.get(MfaService.OTP_REDIS_PREFIX + "test_customer")).thenReturn("849201");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                mfaService.verifyMfa("test_customer", "MFA-TICKET-123", "999999", false)
        );

        assertTrue(ex.getMessage().contains("Invalid or expired"));
    }

    @Test
    @DisplayName("Should authenticate and burn single-use recovery code")
    void testVerifyAndBurnRecoveryCode() {
        String plainCode = "RC-ABCD-1234";
        String hashedCode = passwordEncoder.encode(plainCode);

        testUser.setMfaEnabled(true);
        testUser.setMfaRecoveryCodes(hashedCode);

        when(appUserRepository.findByUsername("test_customer")).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
        when(valueOperations.get(MfaService.TICKET_REDIS_PREFIX + "MFA-TICKET-999")).thenReturn("test_customer");

        AppUser verified = mfaService.verifyMfa("test_customer", "MFA-TICKET-999", plainCode, true);

        assertNotNull(verified);
        // Ensure burned recovery code is removed
        assertEquals("", testUser.getMfaRecoveryCodes());
        verify(appUserRepository, times(1)).save(testUser);
    }
}

