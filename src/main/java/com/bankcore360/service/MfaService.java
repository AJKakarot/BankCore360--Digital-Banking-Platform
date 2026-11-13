package com.bankcore360.service;

import com.bankcore360.dto.MfaEnrollResponse;
import com.bankcore360.entity.AppUser;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Factor Authentication Service supporting Time-Limited OTPs
 * and Single-Use Cryptographic Recovery Codes.
 */
@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String OTP_REDIS_PREFIX = "bankcore360:mfa:otp:";
    public static final String TICKET_REDIS_PREFIX = "bankcore360:mfa:ticket:";
    public static final Duration OTP_VALIDITY = Duration.ofMinutes(5);

    // Fallback in-memory cache if Redis is temporarily offline
    private final Map<String, String> inMemoryOtpCache = new ConcurrentHashMap<>();
    private final Map<String, Long> inMemoryExpiryCache = new ConcurrentHashMap<>();

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final FintechMetricsService fintechMetricsService;

    public MfaService(AppUserRepository appUserRepository,
                      PasswordEncoder passwordEncoder,
                      @org.springframework.beans.factory.annotation.Autowired(required = false)
                      StringRedisTemplate stringRedisTemplate,
                      @org.springframework.beans.factory.annotation.Autowired(required = false)
                      FintechMetricsService fintechMetricsService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.fintechMetricsService = fintechMetricsService;
    }

    /**
     * Enrolls a user in MFA and generates 8 single-use recovery codes.
     */
    @Transactional
    public MfaEnrollResponse enrollMfa(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found: " + username));

        List<String> plainRecoveryCodes = new ArrayList<>();
        List<String> hashedRecoveryCodes = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            String code = "RC-" + generateAlphanumeric(4) + "-" + generateAlphanumeric(4);
            plainRecoveryCodes.add(code);
            hashedRecoveryCodes.add(passwordEncoder.encode(code));
        }

        user.setMfaEnabled(true);
        user.setMfaType("OTP");
        user.setMfaSecret(TokenRotationService.generateSecureToken());
        user.setMfaRecoveryCodes(String.join(",", hashedRecoveryCodes));
        appUserRepository.save(user);

        log.info("MFA successfully enrolled for user: {}", username);
        return new MfaEnrollResponse(
                true,
                "OTP",
                plainRecoveryCodes,
                "MFA successfully enabled. Store your recovery codes in a secure vault. Each recovery code can only be used once."
        );
    }

    /**
     * Generates and stores a 6-digit OTP for second-factor authentication.
     */
    public String generateAndStoreOtp(String username) {
        int otpInt = 100000 + SECURE_RANDOM.nextInt(900000);
        String otp = String.valueOf(otpInt);

        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(OTP_REDIS_PREFIX + username, otp, OTP_VALIDITY);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for OTP storage, using local in-memory fallback: {}", e.getMessage());
            inMemoryOtpCache.put(username, otp);
            inMemoryExpiryCache.put(username, System.currentTimeMillis() + OTP_VALIDITY.toMillis());
        }

        log.info("Generated 6-digit MFA OTP for user: {} (Valid for 5 mins)", username);
        return otp;
    }

    /**
     * Creates a temporary MFA session ticket (expires in 5 mins).
     */
    public String createMfaTicket(String username) {
        String ticket = "MFA-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(TICKET_REDIS_PREFIX + ticket, username, OTP_VALIDITY);
            }
        } catch (Exception e) {
            inMemoryOtpCache.put(ticket, username);
            inMemoryExpiryCache.put(ticket, System.currentTimeMillis() + OTP_VALIDITY.toMillis());
        }
        return ticket;
    }

    /**
     * Verifies an OTP or Recovery Code.
     */
    @Transactional
    public AppUser verifyMfa(String username, String ticket, String code, boolean isRecoveryCode) {
        // Validate ticket
        String ticketUser = getUsernameForTicket(ticket);
        if (ticketUser == null || !ticketUser.equalsIgnoreCase(username)) {
            if (fintechMetricsService != null) fintechMetricsService.recordMfaAttempt("TICKET", false);
            throw new BadCredentialsException("Invalid or expired MFA verification session ticket");
        }

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found: " + username));

        if (!user.isMfaEnabled()) {
            return user;
        }

        if (isRecoveryCode) {
            // Validate and burn single-use recovery code
            boolean verified = verifyAndBurnRecoveryCode(user, code.trim());
            if (!verified) {
                if (fintechMetricsService != null) fintechMetricsService.recordMfaAttempt("RECOVERY", false);
                throw new BadCredentialsException("Invalid or previously used recovery code");
            }
            if (fintechMetricsService != null) fintechMetricsService.recordMfaAttempt("RECOVERY", true);
            log.info("User {} successfully authenticated using a single-use recovery code", username);
            return user;
        }

        // Validate regular 6-digit OTP
        String storedOtp = getStoredOtp(username);
        if (storedOtp == null || !storedOtp.equals(code.trim())) {
            if (fintechMetricsService != null) fintechMetricsService.recordMfaAttempt("OTP", false);
            throw new BadCredentialsException("Invalid or expired 6-digit MFA OTP");
        }

        // Consume OTP
        clearStoredOtp(username);
        if (fintechMetricsService != null) fintechMetricsService.recordMfaAttempt("OTP", true);
        log.info("User {} successfully passed MFA OTP verification", username);
        return user;
    }

    private boolean verifyAndBurnRecoveryCode(AppUser user, String providedCode) {
        String storedRaw = user.getMfaRecoveryCodes();
        if (storedRaw == null || storedRaw.isBlank()) {
            return false;
        }

        List<String> hashedCodes = new ArrayList<>(Arrays.asList(storedRaw.split(",")));
        int matchedIdx = -1;

        for (int i = 0; i < hashedCodes.size(); i++) {
            if (passwordEncoder.matches(providedCode, hashedCodes.get(i))) {
                matchedIdx = i;
                break;
            }
        }

        if (matchedIdx >= 0) {
            // Remove the burned recovery code so it can never be used again
            hashedCodes.remove(matchedIdx);
            user.setMfaRecoveryCodes(String.join(",", hashedCodes));
            appUserRepository.save(user);
            return true;
        }

        return false;
    }

    private String getUsernameForTicket(String ticket) {
        try {
            if (stringRedisTemplate != null) {
                return stringRedisTemplate.opsForValue().get(TICKET_REDIS_PREFIX + ticket);
            }
        } catch (Exception ignored) {}

        Long expiry = inMemoryExpiryCache.get(ticket);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            return inMemoryOtpCache.get(ticket);
        }
        return null;
    }

    private String getStoredOtp(String username) {
        try {
            if (stringRedisTemplate != null) {
                return stringRedisTemplate.opsForValue().get(OTP_REDIS_PREFIX + username);
            }
        } catch (Exception ignored) {}

        Long expiry = inMemoryExpiryCache.get(username);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            return inMemoryOtpCache.get(username);
        }
        return null;
    }

    private void clearStoredOtp(String username) {
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete(OTP_REDIS_PREFIX + username);
            }
        } catch (Exception ignored) {}
        inMemoryOtpCache.remove(username);
        inMemoryExpiryCache.remove(username);
    }

    private static String generateAlphanumeric(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
