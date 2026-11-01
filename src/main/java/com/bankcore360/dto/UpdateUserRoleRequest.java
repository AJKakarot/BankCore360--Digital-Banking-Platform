package com.bankcore360.dto;

import com.bankcore360.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRoleRequest {

    @NotNull(message = "Role must not be null")
    private UserRole role;

    public UpdateUserRoleRequest() {
    }

    public UpdateUserRoleRequest(UserRole role) {
        this.role = role;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}

