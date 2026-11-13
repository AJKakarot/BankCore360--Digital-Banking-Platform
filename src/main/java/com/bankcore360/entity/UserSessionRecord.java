package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_sessions",
    indexes = {
        @Index(name = "idx_uss_user", columnList = "user_id"),
        @Index(name = "idx_uss_token", columnList = "session_token_hash", unique = true)
    }
)
public class UserSessionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "session_token_hash", nullable = false, unique = true, length = 128)
    private String sessionTokenHash;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_os", length = 64)
    private String deviceOs;

    @Column(name = "browser", length = 64)
    private String browser;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_suspicious", nullable = false)
    private boolean suspicious = false;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt = LocalDateTime.now();

    public UserSessionRecord() {
    }

    public UserSessionRecord(AppUser user, String sessionTokenHash, String ipAddress,
                             String userAgent, String deviceOs, String browser) {
        this.user = user;
        this.sessionTokenHash = sessionTokenHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceOs = deviceOs;
        this.browser = browser;
        this.active = true;
        this.suspicious = false;
        this.lastActivityAt = LocalDateTime.now();
        this.loginAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getSessionTokenHash() {
        return sessionTokenHash;
    }

    public void setSessionTokenHash(String sessionTokenHash) {
        this.sessionTokenHash = sessionTokenHash;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceOs() {
        return deviceOs;
    }

    public void setDeviceOs(String deviceOs) {
        this.deviceOs = deviceOs;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public void setSuspicious(boolean suspicious) {
        this.suspicious = suspicious;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }
}
