package com.bankcore360.dto;

import java.time.LocalDateTime;

public class SessionResponse {

    private Long id;
    private String ipAddress;
    private String deviceOs;
    private String browser;
    private String userAgent;
    private boolean active;
    private boolean suspicious;
    private LocalDateTime lastActivityAt;
    private LocalDateTime loginAt;

    public SessionResponse() {
    }

    public SessionResponse(Long id, String ipAddress, String deviceOs, String browser,
                           String userAgent, boolean active, boolean suspicious,
                           LocalDateTime lastActivityAt, LocalDateTime loginAt) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.deviceOs = deviceOs;
        this.browser = browser;
        this.userAgent = userAgent;
        this.active = active;
        this.suspicious = suspicious;
        this.lastActivityAt = lastActivityAt;
        this.loginAt = loginAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
