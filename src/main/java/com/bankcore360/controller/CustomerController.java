
package com.bankcore360.controller;

import com.bankcore360.dto.CustomerRequest;
import com.bankcore360.dto.CustomerResponse;
import com.bankcore360.dto.KycAuditLogResponse;
import com.bankcore360.dto.KycUpdateRequest;
import com.bankcore360.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        return customerService.createCustomer(customerRequest);
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    public CustomerResponse updateKycStatus(
            @PathVariable Long id,
            @Valid @RequestBody KycUpdateRequest request,
            Authentication authentication
    ) {
        return customerService.updateKycStatus(id, request, authentication);
    }

    @GetMapping("/{id}/kyc/audit")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    public List<KycAuditLogResponse> getKycAuditHistory(@PathVariable Long id) {
        return customerService.getKycAuditHistory(id);
    }
}

