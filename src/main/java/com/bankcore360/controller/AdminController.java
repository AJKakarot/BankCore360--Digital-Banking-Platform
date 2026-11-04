package com.bankcore360.controller;

import com.bankcore360.dto.CreateUserRequest;
import com.bankcore360.dto.UpdateUserRoleRequest;
import com.bankcore360.dto.UserResponse;
import com.bankcore360.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.createPrivilegedUser(request);
    }

    @GetMapping("/users")
    public java.util.List<UserResponse> getAllUsers() {
        return authService.getAllUsers();
    }

    @PutMapping("/users/{id}/role")
    public UserResponse updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return authService.updateUserRole(id, request.getRole());
    }
}

