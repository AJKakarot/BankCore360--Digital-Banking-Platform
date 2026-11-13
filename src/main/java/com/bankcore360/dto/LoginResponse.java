package com.bankcore360.dto;

import com.bankcore360.entity.UserRole;

public class LoginResponse {

    private String message;
    private Long id;
    private String username;
    private String email;
    private UserRole role;

    // Advanced Security Additions
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private boolean mfaRequired = false;
    private String mfaTicket;

    public LoginResponse() {
    }

    public LoginResponse(String message, Long id, String username, String email, UserRole role) {
        this.message = message;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public LoginResponse(String message, Long id, String username, String email, UserRole role,
                         String accessToken, String refreshToken) {
        this.message = message;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.mfaRequired = false;
    }

    public static LoginResponse mfaRequired(String mfaTicket, Long id, String username, String email, UserRole role) {
        LoginResponse resp = new LoginResponse("MFA verification required", id, username, email, role);
        resp.setMfaRequired(true);
        resp.setMfaTicket(mfaTicket);
        return resp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getMfaTicket() {
        return mfaTicket;
    }

    public void setMfaTicket(String mfaTicket) {
        this.mfaTicket = mfaTicket;
    }
}
