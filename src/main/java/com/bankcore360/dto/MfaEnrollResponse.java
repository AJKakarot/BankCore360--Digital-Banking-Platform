package com.bankcore360.dto;

import java.util.List;

public class MfaEnrollResponse {

    private boolean mfaEnabled;
    private String mfaType;
    private List<String> recoveryCodes;
    private String message;

    public MfaEnrollResponse() {
    }

    public MfaEnrollResponse(boolean mfaEnabled, String mfaType, List<String> recoveryCodes, String message) {
        this.mfaEnabled = mfaEnabled;
        this.mfaType = mfaType;
        this.recoveryCodes = recoveryCodes;
        this.message = message;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaType() {
        return mfaType;
    }

    public void setMfaType(String mfaType) {
        this.mfaType = mfaType;
    }

    public List<String> getRecoveryCodes() {
        return recoveryCodes;
    }

    public void setRecoveryCodes(List<String> recoveryCodes) {
        this.recoveryCodes = recoveryCodes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

