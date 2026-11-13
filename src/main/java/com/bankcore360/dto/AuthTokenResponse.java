package com.bankcore360.dto;

public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn = 900; // 15 minutes in seconds
    private boolean mfaRequired = false;
    private String mfaTicket;
    private UserResponse user;

    public AuthTokenResponse() {
    }

    public AuthTokenResponse(String accessToken, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = 900;
        this.mfaRequired = false;
        this.user = user;
    }

    public static AuthTokenResponse mfaRequired(String mfaTicket, UserResponse user) {
        AuthTokenResponse resp = new AuthTokenResponse();
        resp.setMfaRequired(true);
        resp.setMfaTicket(mfaTicket);
        resp.setUser(user);
        return resp;
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

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
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

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
