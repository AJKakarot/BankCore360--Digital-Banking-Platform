package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.CreateUserRequest;
import com.bankcore360.dto.UpdateUserRoleRequest;
import com.bankcore360.dto.UserResponse;
import com.bankcore360.entity.UserRole;
import com.bankcore360.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/users with ADMIN role should return 201 and created privileged user")
    void shouldAllowAdminToProvisionUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("new_checker", "checker@bankcore360.com", "Password123!", UserRole.CHECKER);
        UserResponse response = new UserResponse(20L, "new_checker", "checker@bankcore360.com", UserRole.CHECKER, true, LocalDateTime.now());

        when(authService.createPrivilegedUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.username").value("new_checker"))
                .andExpect(jsonPath("$.role").value("CHECKER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("POST /api/admin/users with CUSTOMER role should return 403 Forbidden")
    void shouldForbidCustomerFromProvisioningUsers() throws Exception {
        CreateUserRequest request = new CreateUserRequest("bad_checker", "bad@bankcore360.com", "Password123!", UserRole.CHECKER);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("POST /api/admin/users with CHECKER role should return 403 Forbidden")
    void shouldForbidCheckerFromProvisioningUsers() throws Exception {
        CreateUserRequest request = new CreateUserRequest("bad_checker", "bad@bankcore360.com", "Password123!", UserRole.MANAGER);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /api/admin/users without authentication should return 401 Unauthorized")
    void shouldReturnUnauthorizedWhenAnonymousAccessAdminUsers() throws Exception {
        CreateUserRequest request = new CreateUserRequest("anon_checker", "anon@bankcore360.com", "Password123!", UserRole.CHECKER);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/users with ADMIN role should return 200 OK and list of users")
    void shouldAllowAdminToGetAllUsers() throws Exception {
        UserResponse response = new UserResponse(1L, "dev_customer", "customer@bankcore360.com", UserRole.CUSTOMER, true, LocalDateTime.now());
        when(authService.getAllUsers()).thenReturn(java.util.List.of(response));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("dev_customer"))
                .andExpect(jsonPath("$[0].role").value("CUSTOMER"));
    }

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/admin/users/{id}/role with ADMIN role should return 200 OK and updated user")
    void shouldAllowAdminToUpdateUserRole() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.CHECKER);
        UserResponse response = new UserResponse(10L, "new_user", "new@bankcore360.com", UserRole.CHECKER, true, LocalDateTime.now());
        when(authService.updateUserRole(10L, UserRole.CHECKER)).thenReturn(response);

        mockMvc.perform(put("/api/admin/users/10/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.role").value("CHECKER"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("PUT /api/admin/users/{id}/role with CUSTOMER role should return 403 Forbidden")
    void shouldForbidCustomerFromUpdatingUserRole() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.MANAGER);

        mockMvc.perform(put("/api/admin/users/10/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

