package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 128)
    private String mfaSecret;

    @Column(name = "mfa_type", length = 32)
    private String mfaType = "OTP";

    @Column(name = "mfa_recovery_codes", length = 1024)
    private String mfaRecoveryCodes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AppUser() {
    }

    public AppUser(String username, String email, String password, UserRole role) {
        this(username, email, password, role, true);
    }

    public AppUser(String username, String email, String password, UserRole role, boolean enabled) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = LocalDateTime.now();
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public String getMfaType() {
        return mfaType;
    }

    public void setMfaType(String mfaType) {
        this.mfaType = mfaType;
    }

    public String getMfaRecoveryCodes() {
        return mfaRecoveryCodes;
    }

    public void setMfaRecoveryCodes(String mfaRecoveryCodes) {
        this.mfaRecoveryCodes = mfaRecoveryCodes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

