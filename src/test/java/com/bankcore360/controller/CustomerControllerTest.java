package com.bankcore360.controller;

import com.bankcore360.dto.CustomerRequest;
import com.bankcore360.dto.CustomerResponse;
import com.bankcore360.dto.KycAuditLogResponse;
import com.bankcore360.dto.KycUpdateRequest;
import com.bankcore360.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.bankcore360.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /api/customers with valid request should return 200 and created customer response")
    void shouldCreateCustomerWhenValid() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("John Doe");
        request.setEmail("john.doe@example.com");

        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john.doe@example.com", "PENDING");

        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/customers with blank name should return 400 Bad Request")
    void shouldRejectWhenFullNameIsBlank() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("");
        request.setEmail("valid@example.com");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for input data"));
    }

    @Test
    @DisplayName("POST /api/customers with invalid email should return 400 Bad Request")
    void shouldRejectWhenEmailIsInvalid() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("Valid Name");
        request.setEmail("invalid-email-format");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/customers with duplicate email should return 409 Conflict")
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("Duplicate User");
        request.setEmail("alice@bankcore360.com");

        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new com.bankcore360.exception.DuplicateCustomerException("Customer with email alice@bankcore360.com already exists"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/customers"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/customers should return list of customer responses")
    void shouldReturnAllCustomers() throws Exception {
        CustomerResponse customer = new CustomerResponse(2L, "Jane Doe", "jane@example.com", "PENDING");

        when(customerService.getAllCustomers()).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Jane Doe"))
                .andExpect(jsonPath("$[0].email").value("jane@example.com"))
                .andExpect(jsonPath("$[0].kycStatus").value("PENDING"));
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    @DisplayName("GET /api/customers without authentication should return 401 Unauthorized")
    void shouldReturnUnauthorizedWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/customers/{id}/kyc with CHECKER role should return 200 and updated customer")
    void shouldAllowCheckerToUpdateKycStatus() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.UNDER_REVIEW);
        CustomerResponse response = new CustomerResponse(1L, "Alice Smith", "alice@bankcore360.com", "UNDER_REVIEW");

        when(customerService.updateKycStatus(eq(1L), any(KycUpdateRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.kycStatus").value("UNDER_REVIEW"));
    }

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("PATCH /api/customers/{id}/kyc with MANAGER role should return 200 and updated customer")
    void shouldAllowManagerToUpdateKycStatus() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.VERIFIED);
        CustomerResponse response = new CustomerResponse(1L, "Alice Smith", "alice@bankcore360.com", "VERIFIED");

        when(customerService.updateKycStatus(eq(1L), any(KycUpdateRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("PATCH /api/customers/{id}/kyc with CUSTOMER role should return 403 Forbidden")
    void shouldForbidCustomerFromUpdatingKycStatus() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.UNDER_REVIEW);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    @DisplayName("PATCH /api/customers/{id}/kyc without authentication should return 401 Unauthorized")
    void shouldReturnUnauthorizedWhenUnauthenticatedOnKycPatch() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.UNDER_REVIEW);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/customers/{id}/kyc when customer not found should return 404 Not Found")
    void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.UNDER_REVIEW);

        when(customerService.updateKycStatus(eq(999L), any(KycUpdateRequest.class), any()))
                .thenThrow(new com.bankcore360.exception.CustomerNotFoundException("Customer with ID 999 not found"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/999/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer with ID 999 not found"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/customers/{id}/kyc when invalid transition should return 400 Bad Request")
    void shouldReturnBadRequestWhenInvalidTransition() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.VERIFIED);

        when(customerService.updateKycStatus(eq(1L), any(KycUpdateRequest.class), any()))
                .thenThrow(new com.bankcore360.exception.InvalidKycTransitionException("Cannot transition KYC status from PENDING directly to VERIFIED. Target status must be UNDER_REVIEW"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot transition KYC status from PENDING directly to VERIFIED. Target status must be UNDER_REVIEW"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/customers/{id}/kyc when reviewer attempts self-review should return 403 Forbidden")
    void shouldReturnForbiddenWhenReviewerAttemptsSelfReview() throws Exception {
        KycUpdateRequest request = new KycUpdateRequest(com.bankcore360.entity.KycStatus.UNDER_REVIEW);

        when(customerService.updateKycStatus(eq(1L), any(KycUpdateRequest.class), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Reviewers are not permitted to review or approve their own KYC status"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Reviewers are not permitted to review or approve their own KYC status"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/customers/{id}/kyc with invalid targetStatus value should return 400 Bad Request")
    void shouldReturnBadRequestWhenStatusValueIsInvalid() throws Exception {
        String invalidPayload = "{\"targetStatus\": \"INVALID_STATUS\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/customers/{id}/kyc with missing targetStatus should return 400 Bad Request")
    void shouldReturnBadRequestWhenTargetStatusIsMissing() throws Exception {
        String missingPayload = "{}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/customers/1/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/customers/{id}/kyc/audit with CHECKER role should return 200 and audit history")
    void shouldAllowCheckerToGetKycAuditHistory() throws Exception {
        KycAuditLogResponse log1 = new KycAuditLogResponse(
                10L, 1L, "PENDING", "UNDER_REVIEW", "dev_checker", LocalDateTime.now().minusMinutes(5)
        );

        when(customerService.getKycAuditHistory(1L)).thenReturn(List.of(log1));

        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value(1))
                .andExpect(jsonPath("$[0].previousStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].newStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[0].reviewerUsername").value("dev_checker"));
    }

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("GET /api/customers/{id}/kyc/audit with MANAGER role should return 200 and audit history")
    void shouldAllowManagerToGetKycAuditHistory() throws Exception {
        KycAuditLogResponse log = new KycAuditLogResponse(
                10L, 1L, "PENDING", "UNDER_REVIEW", "dev_manager", LocalDateTime.now()
        );

        when(customerService.getKycAuditHistory(1L)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewerUsername").value("dev_manager"));
    }

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("GET /api/customers/{id}/kyc/audit with ADMIN role should return 200 and audit history")
    void shouldAllowAdminToGetKycAuditHistory() throws Exception {
        KycAuditLogResponse log = new KycAuditLogResponse(
                10L, 1L, "UNDER_REVIEW", "VERIFIED", "dev_admin", LocalDateTime.now()
        );

        when(customerService.getKycAuditHistory(1L)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewerUsername").value("dev_admin"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/customers/{id}/kyc/audit with CUSTOMER role should return 403 Forbidden")
    void shouldForbidCustomerFromAccessingKycAuditHistory() throws Exception {
        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    @DisplayName("GET /api/customers/{id}/kyc/audit without authentication should return 401 Unauthorized")
    void shouldReturnUnauthorizedWhenUnauthenticatedOnKycAudit() throws Exception {
        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/customers/{id}/kyc/audit when customer not found should return 404 Not Found")
    void shouldReturnNotFoundWhenCustomerDoesNotExistForAudit() throws Exception {
        when(customerService.getKycAuditHistory(999L))
                .thenThrow(new com.bankcore360.exception.CustomerNotFoundException("Customer with ID 999 not found"));

        mockMvc.perform(get("/api/customers/999/kyc/audit"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer with ID 999 not found"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/customers/{id}/kyc/audit when history is empty should return 200 and empty list")
    void shouldReturnEmptyListWhenCustomerHasNoAuditHistory() throws Exception {
        when(customerService.getKycAuditHistory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/customers/{id}/kyc/audit with multiple records should return records in chronological order")
    void shouldReturnMultipleAuditRecordsInChronologicalOrder() throws Exception {
        LocalDateTime time1 = LocalDateTime.now().minusMinutes(10);
        LocalDateTime time2 = LocalDateTime.now().minusMinutes(5);

        KycAuditLogResponse log1 = new KycAuditLogResponse(
                10L, 1L, "PENDING", "UNDER_REVIEW", "dev_checker", time1
        );
        KycAuditLogResponse log2 = new KycAuditLogResponse(
                11L, 1L, "UNDER_REVIEW", "VERIFIED", "dev_manager", time2
        );

        when(customerService.getKycAuditHistory(1L)).thenReturn(List.of(log1, log2));

        mockMvc.perform(get("/api/customers/1/kyc/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value(1))
                .andExpect(jsonPath("$[0].previousStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].newStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[0].reviewerUsername").value("dev_checker"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[1].customerId").value(1))
                .andExpect(jsonPath("$[1].previousStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[1].newStatus").value("VERIFIED"))
                .andExpect(jsonPath("$[1].reviewerUsername").value("dev_manager"))
                .andExpect(jsonPath("$[1].createdAt").exists());
    }
}
