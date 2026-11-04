package com.bankcore360.controller;

import com.bankcore360.dto.LoanAnalyticsResponse;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanAuditLogResponse;
import com.bankcore360.service.AdminLoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/loans")
@Tag(name = "Admin Loan Operations", description = "Endpoints for system-wide loan analytics and comprehensive audit log inspection")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

    public AdminLoanController(AdminLoanService adminLoanService) {
        this.adminLoanService = adminLoanService;
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get platform-wide loan analytics", description = "Returns customer counts, application volumes, pipeline bottlenecks, and financial sums")
    public ResponseEntity<LoanAnalyticsResponse> getAnalytics() {
        LoanAnalyticsResponse analytics = adminLoanService.getAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get paginated system audit logs", description = "Returns immutable records of actions taken by customers, checkers, and managers")
    public ResponseEntity<Page<LoanAuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<LoanAuditLogResponse> logs = adminLoanService.getSystemAuditLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping({"", "/all"})
    @Operation(summary = "Get all platform loan applications", description = "Returns full portfolio of loan applications across all customers and states")
    public ResponseEntity<List<LoanApplicationResponse>> getAllApplications() {
        List<LoanApplicationResponse> applications = adminLoanService.getAllApplications();
        return ResponseEntity.ok(applications);
    }
}

