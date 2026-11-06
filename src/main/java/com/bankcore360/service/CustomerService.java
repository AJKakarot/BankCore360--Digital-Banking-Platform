
package com.bankcore360.service;

import com.bankcore360.dto.CustomerRequest;
import com.bankcore360.dto.CustomerResponse;
import com.bankcore360.dto.KycAuditLogResponse;
import com.bankcore360.dto.KycUpdateRequest;
import com.bankcore360.entity.Customer;
import com.bankcore360.entity.KycAuditLog;
import com.bankcore360.entity.KycStatus;
import com.bankcore360.exception.CustomerNotFoundException;
import com.bankcore360.exception.DuplicateCustomerException;
import com.bankcore360.exception.InvalidKycTransitionException;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.repository.KycAuditLogRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KycAuditLogRepository kycAuditLogRepository;

    public CustomerService(CustomerRepository customerRepository, KycAuditLogRepository kycAuditLogRepository) {
        this.customerRepository = customerRepository;
        this.kycAuditLogRepository = kycAuditLogRepository;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateCustomerException("A customer with this email cannot be registered.");
        }
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse updateKycStatus(Long id, KycUpdateRequest request, Authentication authentication) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + id + " not found"));

        Authentication auth = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Reviewers are not permitted to review or approve their own KYC status");
        }

        validateStatusTransition(customer.getKycStatus(), request.getTargetStatus());

        KycStatus previousStatus = customer.getKycStatus();
        KycStatus newStatus = request.getTargetStatus();

        customer.setKycStatus(newStatus);
        Customer savedCustomer = customerRepository.save(customer);

        String reviewerUsername = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "SYSTEM";

        KycAuditLog auditLog = new KycAuditLog(
                savedCustomer.getId() != null ? savedCustomer.getId() : id,
                previousStatus,
                newStatus,
                reviewerUsername,
                LocalDateTime.now()
        );
        kycAuditLogRepository.save(auditLog);

        return mapToResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<KycAuditLogResponse> getKycAuditHistory(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " not found");
        }

        return kycAuditLogRepository.findByCustomerIdOrderByCreatedAtAscIdAsc(customerId)
                .stream()
                .map(this::mapToAuditResponse)
                .toList();
    }

    private void validateStatusTransition(KycStatus currentStatus, KycStatus targetStatus) {
        if (targetStatus == null || targetStatus == KycStatus.PENDING) {
            throw new InvalidKycTransitionException(
                    "Target status must be UNDER_REVIEW, VERIFIED, or REJECTED"
            );
        }

        if (currentStatus == KycStatus.VERIFIED || currentStatus == KycStatus.REJECTED) {
            throw new InvalidKycTransitionException(
                    "Customer KYC is already in terminal state " + currentStatus + " and cannot be changed"
            );
        }

        if (currentStatus == KycStatus.PENDING) {
            if (targetStatus != KycStatus.UNDER_REVIEW) {
                throw new InvalidKycTransitionException(
                        "Cannot transition KYC status from PENDING directly to " + targetStatus + ". Target status must be UNDER_REVIEW"
                );
            }
        } else if (currentStatus == KycStatus.UNDER_REVIEW) {
            if (targetStatus != KycStatus.VERIFIED && targetStatus != KycStatus.REJECTED) {
                throw new InvalidKycTransitionException(
                        "Cannot transition KYC status from UNDER_REVIEW to " + targetStatus + ". Target status must be VERIFIED or REJECTED"
                );
            }
        }
    }

    private boolean isSamePerson(String principalName, String customerEmail) {
        if (principalName == null || customerEmail == null) {
            return false;
        }
        String p = principalName.trim().toLowerCase();
        String c = customerEmail.trim().toLowerCase();

        // 1. Exact match (e.g. principal is username or email identical to customer email)
        if (p.equals(c)) {
            return true;
        }

        // 2. Principal matches the local-part of the customer email (e.g. dev_checker vs dev_checker@example.com)
        int atIndex = c.indexOf('@');
        if (atIndex > 0) {
            String emailLocalPart = c.substring(0, atIndex);
            if (p.equals(emailLocalPart)) {
                return true;
            }
        }

        // 3. Customer email matches the local-part of principal if principal is an email address
        int pAtIndex = p.indexOf('@');
        if (pAtIndex > 0) {
            String principalLocalPart = p.substring(0, pAtIndex);
            if (principalLocalPart.equals(c)) {
                return true;
            }
            if (atIndex > 0 && principalLocalPart.equals(c.substring(0, atIndex))) {
                return true;
            }
        }

        return false;
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getKycStatus().name()
        );
    }

    private KycAuditLogResponse mapToAuditResponse(KycAuditLog auditLog) {
        return new KycAuditLogResponse(
                auditLog.getId(),
                auditLog.getCustomerId(),
                auditLog.getPreviousStatus().name(),
                auditLog.getNewStatus().name(),
                auditLog.getReviewerUsername(),
                auditLog.getCreatedAt()
        );
    }
}

