package com.bankcore360.dto;

public class CustomerResponse {

    private Long id;
    private String fullName;
    private String email;
    private String kycStatus;

    public CustomerResponse() {
    }

    public CustomerResponse(Long id, String fullName,
                            String email, String kycStatus) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.kycStatus = kycStatus;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getKycStatus() {
        return kycStatus;
    }
}