package com.bankcore360.controller;

import com.bankcore360.dto.*;
import com.bankcore360.entity.AppUser;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.service.AuthService;
import com.bankcore360.service.SessionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & Advanced Security", description = "Endpoints for login, MFA verification, Refresh Token Rotation, and Session Management")
public class AuthController {

    private final AuthService authService;
    private final SessionManagementService sessionManagementService;
    private final AppUserRepository appUserRepository;

    public AuthController(AuthService authService,
                          @org.springframework.beans.factory.annotation.Autowired(required = false)
                          SessionManagementService sessionManagementService,
                          AppUserRepository appUserRepository) {
        this.authService = authService;
        this.sessionManagementService = sessionManagementService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new customer user", description = "Self-service onboarding for digital banking customers")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies credentials, checks MFA requirements, registers device session, and issues rotated token pair")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA OTP or Single-Use Recovery Code", description = "Completes 2nd factor authentication and issues access + refresh tokens")
    public LoginResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        return authService.verifyMfa(request, httpRequest);
    }

    @PostMapping("/mfa/enroll")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Enroll authenticated user in MFA", description = "Generates 8 single-use cryptographic recovery codes and activates OTP verification")
    public MfaEnrollResponse enrollMfa(Authentication authentication) {
        return authService.enrollMfa(authentication.getName());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate Refresh Token", description = "Rotates refresh token and issues new short-lived access token. Intercepts and revokes on token reuse theft.")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.rotateRefreshToken(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate session", description = "Revokes refresh token and deactivates active device sessions")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                       Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        authService.logout(request, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List active user sessions & devices", description = "Inspects active logins with IP, OS, browser, and suspicious activity flags")
    public List<SessionResponse> getActiveSessions(Authentication authentication) {
        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));
        return sessionManagementService.getActiveSessions(user.getId());
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke specific device session", description = "Terminates a specific active session by ID")
    public ResponseEntity<Void> revokeSession(@PathVariable Long id, Authentication authentication) {
        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));
        sessionManagementService.revokeSession(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/revoke-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke all sessions (Logout from all devices)", description = "Terminates all active sessions for the authenticated user")
    public ResponseEntity<Void> revokeAllSessions(Authentication authentication) {
        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));
        sessionManagementService.revokeAllSessions(user.getId());
        return ResponseEntity.noContent().build();
    }
}
