package com.bankcore360.service;

import com.bankcore360.dto.LoanEligibilityRequest;
import com.bankcore360.dto.LoanEligibilityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEligibilityServiceTest {

    private LoanEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService = new LoanEligibilityService();
    }

    @Test
    @DisplayName("Should accurately calculate reducing-balance monthly EMI")
    void shouldCalculateAccurateReducingBalanceEmi() {
        // Loan of 100,000 at 12% p.a. for 12 months
        // Expected standard EMI = ~8884.88
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("12.00");
        int tenure = 12;

        BigDecimal emi = eligibilityService.calculateEmi(principal, annualRate, tenure);

        assertThat(emi).isEqualByComparingTo(new BigDecimal("8884.88"));
    }

    @Test
    @DisplayName("Should assess applicant as eligible when income >= 15000 and FOIR <= 50%")
    void shouldAssessApplicantAsEligible() {
        LoanEligibilityRequest request = new LoanEligibilityRequest(
                new BigDecimal("50000.00"), // monthly income
                new BigDecimal("0.00"),     // existing EMI
                new BigDecimal("100000.00"),// loan amount
                24,                         // tenure months
                new BigDecimal("10.50")     // annual interest rate
        );

        LoanEligibilityResponse response = eligibilityService.calculateEligibility(request);

        assertThat(response.isEligible()).isTrue();
        assertThat(response.getEstimatedMonthlyEmi()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.getMonthlyEmiToIncomeRatio()).isLessThan(new BigDecimal("50.00"));
        assertThat(response.getMaxEligibleLoanAmount()).isGreaterThan(request.getRequestedAmount());
        assertThat(response.getStatusMessage()).contains("Indicative Eligible");
        assertThat(response.getDisclaimer()).isNotEmpty();
    }

    @Test
    @DisplayName("Should assess applicant as ineligible when monthly income < 15,000")
    void shouldAssessIneligibleWhenIncomeBelowMinimum() {
        LoanEligibilityRequest request = new LoanEligibilityRequest(
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                12,
                new BigDecimal("10.50")
        );

        LoanEligibilityResponse response = eligibilityService.calculateEligibility(request);

        assertThat(response.isEligible()).isFalse();
        assertThat(response.getStatusMessage()).contains("Minimum monthly income requirement of ₹15,000 is not met");
    }

    @Test
    @DisplayName("Should assess applicant as ineligible when total commitments exceed 50% FOIR")
    void shouldAssessIneligibleWhenFoirExceedsFiftyPercent() {
        LoanEligibilityRequest request = new LoanEligibilityRequest(
                new BigDecimal("30000.00"),
                new BigDecimal("10000.00"), // Existing EMI already takes 33%
                new BigDecimal("200000.00"),// New loan EMI ~9200 pushes total to ~64%
                24,
                new BigDecimal("10.50")
        );

        LoanEligibilityResponse response = eligibilityService.calculateEligibility(request);

        assertThat(response.isEligible()).isFalse();
        assertThat(response.getMonthlyEmiToIncomeRatio()).isGreaterThan(new BigDecimal("50.00"));
        assertThat(response.getStatusMessage()).contains("exceed the 50% FOIR threshold");
    }
}

