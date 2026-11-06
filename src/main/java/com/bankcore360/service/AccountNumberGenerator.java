package com.bankcore360.service;

import com.bankcore360.entity.AccountType;
import com.bankcore360.repository.BankAccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final String BANK_PREFIX = "360";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_GENERATION_ATTEMPTS = 15;

    private final BankAccountRepository bankAccountRepository;

    public AccountNumberGenerator(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public String generateUniqueAccountNumber(AccountType accountType) {
        String typeCode = (accountType == AccountType.SAVINGS) ? "10" : "20";

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            // Generate 7-digit random number (1000000 to 9999999)
            int randomPart = 1000000 + RANDOM.nextInt(9000000);
            String candidateNumber = BANK_PREFIX + typeCode + randomPart;

            if (!bankAccountRepository.existsByAccountNumber(candidateNumber)) {
                return candidateNumber;
            }
        }

        // Fallback with timestamp suffix to guarantee uniqueness under extreme load
        long timestampSuffix = System.currentTimeMillis() % 10000000L;
        String fallback = BANK_PREFIX + typeCode + String.format("%07d", timestampSuffix);
        if (!bankAccountRepository.existsByAccountNumber(fallback)) {
            return fallback;
        }

        throw new IllegalStateException("Failed to generate a unique account number after multiple attempts");
    }
}

