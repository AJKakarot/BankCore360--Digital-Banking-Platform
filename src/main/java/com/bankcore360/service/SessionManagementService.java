package com.bankcore360.service;

import com.bankcore360.dto.SessionResponse;
import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.UserSessionRecord;
import com.bankcore360.repository.UserSessionRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enterprise Device & Session Management with heuristic suspicious login detection.
 */
@Service
public class SessionManagementService {

    private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

    private final UserSessionRecordRepository sessionRepository;

    public SessionManagementService(UserSessionRecordRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public UserSessionRecord registerSession(AppUser user, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
        String os = parseOperatingSystem(userAgent);
        String browser = parseBrowser(userAgent);

        // Suspicious login heuristic: has this user logged in from this IP before?
        boolean knownIp = sessionRepository.existsByUserIdAndIpAddress(user.getId(), ip);
        boolean suspicious = !knownIp && sessionRepository.findByUserId(user.getId()).size() > 0;

        String sessionToken = "SES-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String sessionTokenHash = TokenRotationService.hashToken(sessionToken);

        UserSessionRecord session = new UserSessionRecord(
                user,
                sessionTokenHash,
                ip,
                userAgent != null && userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent,
                os,
                browser
        );
        session.setSuspicious(suspicious);

        if (suspicious) {
            log.warn("⚠️ Suspicious login detected for user {} from new IP: {}, OS: {}, Browser: {}",
                    user.getUsername(), ip, os, browser);
        }

        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndActiveTrueOrderByLastActivityAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(Long sessionId, Long userId) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            if (s.getUser().getId().equals(userId)) {
                s.setActive(false);
                sessionRepository.save(s);
                log.info("Session {} revoked for user {}", sessionId, userId);
            }
        });
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        sessionRepository.revokeAllForUser(userId);
        log.info("All active sessions revoked for user {}", userId);
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    public static String parseOperatingSystem(String userAgent) {
        if (userAgent == null) return "Unknown OS";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mac") || ua.contains("darwin") || ua.contains("os x")) return "macOS";
        if (ua.contains("win")) return "Windows";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "Unknown OS";
    }

    public static String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown Browser";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg")) return "Microsoft Edge";
        if (ua.contains("chrome") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("postman")) return "Postman";
        return "Web Browser";
    }

    private SessionResponse mapToResponse(UserSessionRecord s) {
        return new SessionResponse(
                s.getId(),
                s.getIpAddress(),
                s.getDeviceOs(),
                s.getBrowser(),
                s.getUserAgent(),
                s.isActive(),
                s.isSuspicious(),
                s.getLastActivityAt(),
                s.getLoginAt()
        );
    }
}

