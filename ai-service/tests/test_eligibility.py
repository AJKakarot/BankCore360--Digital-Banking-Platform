"""
Tests for Deterministic Financial Calculation Engine & Loan Eligibility.
"""

import pytest
import sys
from pathlib import Path

# Add ai-service to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from eligibility import calculate_reducing_balance_emi, evaluate_eligibility
from schemas import EligibilityExplanationRequest

def test_calculate_reducing_balance_emi_accuracy():
    # Standard: P = 100,000, Annual Rate = 12% (r = 1% per month), n = 12 months
    # Formula gives E = 100000 * 0.01 * (1.01)^12 / ((1.01)^12 - 1) ≈ 8884.88
    emi = calculate_reducing_balance_emi(100000.0, 12.0, 12)
    assert 8880.0 <= emi <= 8890.0
    assert emi == 8884.88

def test_calculate_reducing_balance_emi_zero_rate():
    # 0% interest rate: simply P / n
    emi = calculate_reducing_balance_emi(120000.0, 0.0, 12)
    assert emi == 10000.0

def test_calculate_reducing_balance_emi_zero_principal_or_tenure():
    assert calculate_reducing_balance_emi(0.0, 10.0, 12) == 0.0
    assert calculate_reducing_balance_emi(100000.0, 10.0, 0) == 0.0

def test_evaluate_eligibility_favorable():
    req = EligibilityExplanationRequest(
        loanType="PERSONAL",
        monthlyIncome=60000.0,
        existingMonthlyEmi=4000.0,
        requestedAmount=150000.0,
        desiredTenureMonths=24
    )
    res = evaluate_eligibility(req)
    assert res.indicativeStatus == "INDICATIVE_ELIGIBLE"
    assert res.calculations.foirPercent <= 50.0
    assert res.calculations.minimumIncomeRequired == 15000.0
    assert "favorable" in res.naturalLanguageExplanation.lower()
    assert "does not constitute a guaranteed loan sanction" in res.disclaimer.lower()

def test_evaluate_eligibility_below_minimum_income():
    req = EligibilityExplanationRequest(
        loanType="PERSONAL",
        monthlyIncome=12000.0,  # Below 15,000 threshold
        existingMonthlyEmi=0.0,
        requestedAmount=50000.0,
        desiredTenureMonths=12
    )
    res = evaluate_eligibility(req)
    assert res.indicativeStatus == "INDICATIVE_INELIGIBLE"
    assert "below bankcore360's minimum threshold" in res.naturalLanguageExplanation.lower()
    assert any("co-borrower" in r.lower() for r in res.recommendations)

def test_evaluate_eligibility_high_foir():
    req = EligibilityExplanationRequest(
        loanType="PERSONAL",
        monthlyIncome=30000.0,
        existingMonthlyEmi=12000.0,  # Already 40%
        requestedAmount=300000.0,    # New EMI will push FOIR > 50%
        desiredTenureMonths=12
    )
    res = evaluate_eligibility(req)
    assert res.indicativeStatus == "HIGH_FOIR_WARNING"
    assert res.calculations.foirPercent > 50.0
    assert "exceeds bankcore360's maximum allowable limit" in res.naturalLanguageExplanation.lower()
    assert any("increase loan tenure" in r.lower() for r in res.recommendations)

