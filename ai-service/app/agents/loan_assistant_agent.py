"""
Loan Assistant Agent for BankCore360.
Provides deterministic reducing-balance EMI calculations, loan eligibility explanations,
real product catalog comparisons, document guidance, and status tracking for Maker-Checker workflows.
Never independently approves or rejects loans.
"""

import re
import logging
from typing import Dict, Any, Optional
from spring_client import spring_client
from eligibility import evaluate_eligibility
from schemas import EligibilityExplanationRequest
from app.grok_client import ask_grok

logger = logging.getLogger("BankCore360.AI.LoanAssistantAgent")

class LoanAssistantAgent:

    def calculate_emi_deterministic(self, principal: float, rate_percent: float, tenure_months: int) -> float:
        if principal <= 0 or tenure_months <= 0:
            return 0.0
        r = (rate_percent / 12.0) / 100.0
        if r == 0:
            return principal / tenure_months
        emi = (principal * r * ((1 + r) ** tenure_months)) / (((1 + r) ** tenure_months) - 1)
        return round(emi, 2)

    async def execute(self, query: str, customer_username: Optional[str] = None) -> Dict[str, Any]:
        # 1. Fetch live loan product catalog from Spring Boot
        products = await spring_client.get_loan_products()

        # Check for status inquiry
        query_lower = query.lower()
        if any(w in query_lower for w in ["status", "track", "my application", "where is my loan"]):
            if customer_username:
                status_summary = await spring_client.get_status_summary(customer_username)
                if status_summary.get("hasApplications"):
                    ans = (
                        f"### 📋 Loan Application Status: `{status_summary.get('applicationReference')}`\n\n"
                        f"• **Product:** {status_summary.get('loanType')}\n"
                        f"• **Requested Amount:** ₹{status_summary.get('requestedAmount', 0):,.2f}\n"
                        f"• **Current Stage:** `{status_summary.get('status')}`\n\n"
                        f"**Explanation:** {status_summary.get('explanation')}\n\n"
                        f"**Next Action:** {status_summary.get('nextStep')}"
                    )
                    return {
                        "agent": "LoanAssistantAgent",
                        "answer": ans,
                        "sources": [{"documentTitle": "Spring Boot Loan Applications", "filename": "PostgreSQL loan_applications", "similarityScore": 1.0, "excerpt": ans}],
                        "data": status_summary,
                        "suggestedNextSteps": ["View full history at /customer/applications"]
                    }

        # Check for EMI calculation parameters in query (e.g. 500000, 10.5%, 36 months)
        amt_match = re.search(r'(?:₹|rs\.?|inr)?\s*(\d{4,8})', query_lower)
        tenure_match = re.search(r'(\d{1,3})\s*(?:months?|yrs?|years?)', query_lower)
        rate_match = re.search(r'(\d+(?:\.\d+)?)\s*%', query_lower)

        principal = float(amt_match.group(1)) if amt_match else 500000.0
        tenure_months = int(tenure_match.group(1)) if tenure_match else 36
        if "year" in query_lower or "yr" in query_lower:
            if tenure_match and int(tenure_match.group(1)) <= 30:
                tenure_months = int(tenure_match.group(1)) * 12

        rate = float(rate_match.group(1)) if rate_match else 10.5

        # Check if calculating EMI
        if any(w in query_lower for w in ["emi", "calculate", "repay", "installment"]):
            emi = self.calculate_emi_deterministic(principal, rate, tenure_months)
            total_repayment = round(emi * tenure_months, 2)
            total_interest = round(total_repayment - principal, 2)

            calc_summary = (
                f"### 🧮 Reducing Balance EMI Calculation\n\n"
                f"• **Principal Loan Amount:** ₹{principal:,.2f}\n"
                f"• **Annual Interest Rate:** {rate:.2f}% p.a.\n"
                f"• **Tenure:** {tenure_months} months ({(tenure_months/12):.1f} years)\n"
                f"• **Estimated Monthly EMI:** **₹{emi:,.2f}**\n"
                f"• **Total Interest Payable:** ₹{total_interest:,.2f}\n"
                f"• **Total Repayment (P + I):** ₹{total_repayment:,.2f}\n\n"
                f"> [!TIP]\n"
                f"> BankCore360 uses the standardized **Reducing Balance Amortization Method**. "
                f"Each month's interest is assessed solely on the remaining unpaid principal balance, "
                f"substantially lowering your total cost of borrowing compared to flat rate interest."
            )

            return {
                "agent": "LoanAssistantAgent",
                "answer": calc_summary,
                "sources": [{"documentTitle": "BankCore360 Reducing Balance Formula", "filename": "LoanEligibilityService.java", "similarityScore": 1.0, "excerpt": calc_summary}],
                "data": {
                    "principal": principal,
                    "annualRate": rate,
                    "tenureMonths": tenure_months,
                    "monthlyEmi": emi,
                    "totalInterest": total_interest,
                    "totalRepayment": total_repayment
                },
                "suggestedNextSteps": [
                    "Simulate customized borrowing limits at /api/loans/calculator",
                    "Apply online via /customer/apply"
                ]
            }

        # Otherwise, provide retail loan catalog comparison
        product_rows = "\n".join([
            f"• **{p.get('name')} (`{p.get('loanType')}`):** Min ₹{float(p.get('minAmount', 0)):,.0f} to Max ₹{float(p.get('maxAmount', 0)):,.0f} | "
            f"Rates from {p.get('baseInterestRate')}% p.a. | Tenure: {p.get('minTenureMonths')}-{p.get('maxTenureMonths')} mos\n"
            f"  *{p.get('description')}*"
            for p in products
        ])

        catalog_text = (
            f"### 🏦 BankCore360 Retail & Commercial Loan Products\n\n"
            f"{product_rows}\n\n"
            f"#### 🛡️ Maker-Checker-Manager Approval Flow:\n"
            f"1. **Customer (Maker):** Submits application & KYC proofs.\n"
            f"2. **Checker Officer:** Verifies document integrity & ₹15,000 min income.\n"
            f"3. **Branch Manager (Approver):** Conducts FOIR credit check (max 50%) and sanctions atomic disbursement."
        )

        return {
            "agent": "LoanAssistantAgent",
            "answer": catalog_text,
            "sources": [{"documentTitle": "BankCore360 Configured Loan Matrix", "filename": "InternalAiBankingController.java", "similarityScore": 1.0, "excerpt": catalog_text}],
            "data": products,
            "suggestedNextSteps": [
                "Calculate your exact EMI using the prompt: 'Calculate EMI for ₹5,00,000 at 10.5% for 3 years'",
                "Check document requirements: 'What documents are required for a personal loan?'"
            ]
        }

loan_assistant_agent = LoanAssistantAgent()

