package com.bankcore360.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaVerifyRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "MFA Ticket is required")
    private String mfaTicket;

    @NotBlank(message = "OTP or recovery code is required")
    private String code;

    private boolean isRecoveryCode = false;

    public MfaVerifyRequest() {
    }

    public MfaVerifyRequest(String username, String mfaTicket, String code, boolean isRecoveryCode) {
        this.username = username;
        this.mfaTicket = mfaTicket;
        this.code = code;
        this.isRecoveryCode = isRecoveryCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMfaTicket() {
        return mfaTicket;
    }

    public void setMfaTicket(String mfaTicket) {
        this.mfaTicket = mfaTicket;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isRecoveryCode() {
        return isRecoveryCode;
    }

    public void setRecoveryCode(boolean recoveryCode) {
        isRecoveryCode = recoveryCode;
    }
}

