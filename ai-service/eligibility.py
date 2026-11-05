"""
Deterministic Financial Calculation Engine and Eligibility Assistant.
Computes reducing-balance amortization, FOIR / DTI ratios, and generates
grounded natural language explanations with mandatory compliance disclaimers.
"""

from typing import Dict, Any, List
from schemas import EligibilityExplanationRequest, EligibilityExplanationResponse, CalculationBreakdown

# Product Interest Rate defaults (Annual percentage rate)
PRODUCT_RATES = {
    "PERSONAL": 11.50,
    "HOME": 8.75,
    "EDUCATION": 9.50,
    "BUSINESS": 12.00
}

# Policy Limits
MINIMUM_NET_INCOME = 15000.0
MAXIMUM_FOIR_PERCENT = 50.0

def calculate_reducing_balance_emi(principal: float, annual_rate_percent: float, tenure_months: int) -> float:
    """
    Standard monthly reducing balance amortization formula:
    E = P * r * (1 + r)^n / ((1 + r)^n - 1)
    """
    if principal <= 0 or tenure_months <= 0:
        return 0.0
    if annual_rate_percent <= 0:
        return round(principal / tenure_months, 2)

    monthly_rate = (annual_rate_percent / 100.0) / 12.0
    r = monthly_rate
    n = tenure_months
    factor = (1.0 + r) ** n
    emi = principal * r * factor / (factor - 1.0)
    return round(emi, 2)

def evaluate_eligibility(req: EligibilityExplanationRequest) -> EligibilityExplanationResponse:
    """
    Deterministic calculation followed by natural language explanation.
    Never promises or guarantees loan approval.
    """
    loan_type_upper = req.loanType.upper()
    rate = PRODUCT_RATES.get(loan_type_upper, 11.50)
    principal = float(req.requestedAmount)
    tenure = int(req.desiredTenureMonths)
    income = float(req.monthlyIncome)
    existing_emi = float(req.existingMonthlyEmi)

    emi = calculate_reducing_balance_emi(principal, rate, tenure)
    total_repayment = round(emi * tenure, 2)
    total_interest = round(total_repayment - principal, 2)
    total_monthly_obligation = round(existing_emi + emi, 2)
    foir = round((total_monthly_obligation / income) * 100.0, 2)

    breakdown = CalculationBreakdown(
        principalAmount=principal,
        annualInterestRatePercent=rate,
        tenureMonths=tenure,
        monthlyEmi=emi,
        totalInterest=total_interest,
        totalRepayment=total_repayment,
        totalMonthlyObligation=total_monthly_obligation,
        foirPercent=foir,
        policyMaxFoirPercent=MAXIMUM_FOIR_PERCENT,
        minimumIncomeRequired=MINIMUM_NET_INCOME
    )

    # Determine Indicative Status
    recommendations: List[str] = []
    if income < MINIMUM_NET_INCOME:
        status = "INDICATIVE_INELIGIBLE"
        explanation = (
            f"Your stated net monthly income of ₹{income:,.2f} is below BankCore360's minimum threshold of ₹{MINIMUM_NET_INCOME:,.2f}. "
            "Under Retail Credit Policy Section 2.1, all borrowers must demonstrate regular take-home salary or business cashflow of at least ₹15,000. "
            "You may consider applying with an earning family co-applicant to pool household income."
        )
        recommendations.append("Add a co-borrower with verifiable monthly income to meet minimum criteria.")
        recommendations.append("Apply after completing at least 6 months of continuous salaried employment.")
    elif foir > MAXIMUM_FOIR_PERCENT:
        status = "HIGH_FOIR_WARNING"
        explanation = (
            f"Based on your requested loan of ₹{principal:,.2f} over {tenure} months at {rate:.2f}% p.a., your estimated monthly EMI would be ₹{emi:,.2f}. "
            f"Combined with your existing EMIs of ₹{existing_emi:,.2f}, your total monthly obligation is ₹{total_monthly_obligation:,.2f}. "
            f"This results in a Fixed Obligation to Income Ratio (FOIR) of {foir:.1f}%, which exceeds BankCore360's maximum allowable limit of {MAXIMUM_FOIR_PERCENT:.0f}%. "
            "To qualify, you can increase your tenure or request a lower loan amount."
        )
        recommendations.append(f"Increase loan tenure (e.g. from {tenure} to {min(tenure + 12, 60)} months) to reduce monthly EMI.")
        recommendations.append(f"Reduce requested loan amount to bring total FOIR below {MAXIMUM_FOIR_PERCENT:.0f}%.")
        recommendations.append("Prepay or close smaller existing debts before submitting this application.")
    else:
        status = "INDICATIVE_ELIGIBLE"
        explanation = (
            f"Your indicative loan eligibility assessment is favorable! "
            f"For a {req.loanType.title()} loan of ₹{principal:,.2f} at an indicative {rate:.2f}% p.a. over {tenure} months: "
            f"Estimated monthly reducing-balance EMI is ₹{emi:,.2f}. "
            f"Your total monthly obligation is ₹{total_monthly_obligation:,.2f}, representing a healthy FOIR of {foir:.1f}% "
            f"(well within BankCore360's {MAXIMUM_FOIR_PERCENT:.0f}% maximum policy threshold). "
            f"Total interest payable over the tenure is ₹{total_interest:,.2f}."
        )
        recommendations.append("Proceed to complete KYC verification in your Customer Portal.")
        recommendations.append("Prepare your recent 3 months payslips and 6 months bank statement for upload.")
        recommendations.append("Submit your application via POST /api/loans/apply.")

    return EligibilityExplanationResponse(
        indicativeStatus=status,
        calculations=breakdown,
        naturalLanguageExplanation=explanation,
        recommendations=recommendations
    )

