package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.LoginRequest;
import com.bankcore360.dto.LoginResponse;
import com.bankcore360.dto.RegisterRequest;
import com.bankcore360.dto.UserResponse;
import com.bankcore360.entity.UserRole;
import com.bankcore360.exception.DuplicateUserException;
import com.bankcore360.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private com.bankcore360.service.SessionManagementService sessionManagementService;

    @MockitoBean
    private com.bankcore360.repository.AppUserRepository appUserRepository;

    @Test
    @DisplayName("POST /api/auth/register with valid payload should return 201 and safe UserResponse")
    void shouldRegisterCustomerSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("alice_cust", "alice@example.com", "SecurePassword123!");
        UserResponse response = new UserResponse(10L, "alice_cust", "alice@example.com", UserRole.CUSTOMER, true, LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("alice_cust"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate username should return 409 Conflict")
    void shouldReturnConflictOnDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("existing_user", "unique@example.com", "SecurePassword123!");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateUserException("Username 'existing_user' is already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username 'existing_user' is already taken"));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email should return 409 Conflict")
    void shouldReturnConflictOnDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("new_user", "duplicate@example.com", "SecurePassword123!");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateUserException("Email 'duplicate@example.com' is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email 'duplicate@example.com' is already registered"));
    }

    @Test
    @DisplayName("POST /api/auth/register with attempt to supply ADMIN role should ignore role and register as CUSTOMER")
    void shouldIgnoreRoleFieldOnRegistrationAndAssignCustomer() throws Exception {
        String payloadWithAdminAttempt = """
                {
                    "username": "attacker",
                    "email": "attacker@example.com",
                    "password": "Password123!",
                    "role": "ADMIN"
                }
                """;

        UserResponse customerResponse = new UserResponse(11L, "attacker", "attacker@example.com", UserRole.CUSTOMER, true, LocalDateTime.now());
        when(authService.register(any(RegisterRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithAdminAttempt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid input should return 400 Bad Request")
    void shouldReturnBadRequestOnInvalidRegisterInput() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for input data"));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials should return 200 and LoginResponse")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("dev_checker", "devPassword123!");
        LoginResponse response = new LoginResponse("Login successful", 2L, "dev_checker", "dev_checker@bankcore360.com", UserRole.CHECKER);

        when(authService.login(any(LoginRequest.class), any())).thenReturn(response);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("dev_checker"))
                .andExpect(jsonPath("$.email").value("dev_checker@bankcore360.com"))
                .andExpect(jsonPath("$.role").value("CHECKER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid password should return 401 Unauthorized")
    void shouldReturnUnauthorizedOnInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("dev_checker", "wrong-password");

        when(authService.login(any(LoginRequest.class), any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}

