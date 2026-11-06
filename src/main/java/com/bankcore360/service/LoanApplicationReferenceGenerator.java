package com.bankcore360.service;

import com.bankcore360.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Year;

@Service
public class LoanApplicationReferenceGenerator {

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int RANDOM_PART_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();
    private final LoanApplicationRepository loanApplicationRepository;

    public LoanApplicationReferenceGenerator(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    public String generateUniqueReference() {
        int currentYear = Year.now().getValue();
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("LN-").append(currentYear).append("-");
            for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
                sb.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
            }
            String candidate = sb.toString();
            if (!loanApplicationRepository.existsByApplicationReference(candidate)) {
                return candidate;
            }
        }
        // Fallback with timestamp
        return "LN-" + currentYear + "-" + System.currentTimeMillis();
    }
}

