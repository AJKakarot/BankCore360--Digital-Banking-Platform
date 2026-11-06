package com.bankcore360.dto;

import com.bankcore360.entity.UserRole;

public class LoginResponse {

    private String message;
    private Long id;
    private String username;
    private String email;
    private UserRole role;

    public LoginResponse() {
    }

    public LoginResponse(String message, Long id, String username, String email, UserRole role) {
        this.message = message;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
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
}

