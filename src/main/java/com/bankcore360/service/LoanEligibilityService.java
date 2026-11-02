package com.bankcore360.service;

import com.bankcore360.dto.LoanEligibilityRequest;
import com.bankcore360.dto.LoanEligibilityResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Service providing financial reducing-balance EMI and loan eligibility calculations.
 *
 * Formula:
 * E = P * r * (1 + r)^n / ((1 + r)^n - 1)
 *
 * Where:
 * E = Monthly EMI
 * P = Principal loan amount
 * r = Monthly interest rate (Annual rate / 12 / 100)
 * n = Tenure in months
 */
@Service
public class LoanEligibilityService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal MAX_FOIR_PERCENT = new BigDecimal("50.00"); // 50% max Debt-to-Income / FOIR
    private static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("15000.00");

    public LoanEligibilityResponse calculateEligibility(LoanEligibilityRequest request) {
        BigDecimal principal = request.getRequestedAmount();
        int tenureMonths = request.getTenureMonths();
        BigDecimal annualRate = request.getAnnualInterestRate();
        BigDecimal monthlyIncome = request.getMonthlyIncome();
        BigDecimal existingEmi = request.getExistingMonthlyEmi() != null
                ? request.getExistingMonthlyEmi()
                : BigDecimal.ZERO;

        // Calculate monthly EMI
        BigDecimal monthlyEmi = calculateEmi(principal, annualRate, tenureMonths);
        BigDecimal totalRepayment = monthlyEmi.multiply(BigDecimal.valueOf(tenureMonths))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalRepayment.subtract(principal)
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate FOIR / Debt-to-Income Ratio
        BigDecimal totalMonthlyCommitment = monthlyEmi.add(existingEmi);
        BigDecimal foir = totalMonthlyCommitment
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);

        // Determine eligibility
        boolean meetsIncome = monthlyIncome.compareTo(MIN_MONTHLY_INCOME) >= 0;
        boolean meetsFoir = foir.compareTo(MAX_FOIR_PERCENT) <= 0;
        boolean eligible = meetsIncome && meetsFoir;

        // Calculate Maximum Eligible Loan Amount based on 50% max FOIR
        BigDecimal maxPermittedMonthlyEmi = monthlyIncome.multiply(new BigDecimal("0.50"))
                .subtract(existingEmi);

        BigDecimal maxEligibleLoanAmount = BigDecimal.ZERO;
        if (maxPermittedMonthlyEmi.compareTo(BigDecimal.ZERO) > 0) {
            maxEligibleLoanAmount = calculateMaxLoanAmount(maxPermittedMonthlyEmi, annualRate, tenureMonths);
        }

        String statusMessage;
        if (eligible) {
            statusMessage = "Indicative Eligible: Estimated monthly obligation is within the acceptable 50% FOIR threshold.";
        } else if (!meetsIncome) {
            statusMessage = "Indicative Ineligible: Minimum monthly income requirement of ₹15,000 is not met.";
        } else {
            statusMessage = "Indicative Ineligible: Total monthly EMI commitments (" + foir + "%) exceed the 50% FOIR threshold.";
        }

        return new LoanEligibilityResponse(
                monthlyEmi,
                totalInterest,
                totalRepayment,
                foir,
                eligible,
                maxEligibleLoanAmount,
                statusMessage
        );
    }

    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }

        // Monthly interest rate: r = annualRate / 12 / 100
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), MC);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        // factor = (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusR.pow(tenureMonths, MC);

        // numerator = P * r * factor
        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(factor, MC);

        // denominator = factor - 1
        BigDecimal denominator = factor.subtract(BigDecimal.ONE, MC);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMaxLoanAmount(BigDecimal maxEmi, BigDecimal annualRate, int tenureMonths) {
        if (maxEmi.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), MC);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return maxEmi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusR.pow(tenureMonths, MC);

        // P = E * (factor - 1) / (r * factor)
        BigDecimal numerator = maxEmi.multiply(factor.subtract(BigDecimal.ONE, MC), MC);
        BigDecimal denominator = monthlyRate.multiply(factor, MC);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}

