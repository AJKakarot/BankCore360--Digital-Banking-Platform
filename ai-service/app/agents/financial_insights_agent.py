"""
Financial Insights Agent for BankCore360.
Retrieves actual customer accounts and transaction ledger data via secure internal banking tools,
computes deterministic financial totals (inflow, outflow, category breakdowns),
and provides personalized, zero-hallucination financial explanations.
"""

import logging
from typing import Dict, Any, Optional
from spring_client import spring_client
from app.grok_client import ask_grok

logger = logging.getLogger("BankCore360.AI.FinancialInsightsAgent")

class FinancialInsightsAgent:

    async def execute(self, query: str, customer_username: Optional[str] = None) -> Dict[str, Any]:
        if not customer_username:
            return {
                "agent": "FinancialInsightsAgent",
                "answer": "To view your personalized financial insights and spending summaries, please log in to your BankCore360 customer account.",
                "sources": [],
                "data": None,
                "suggestedNextSteps": ["Log in to your BankCore360 customer dashboard"]
            }

        # 1. Fetch real accounts and balances
        accounts = await spring_client.get_customer_accounts(customer_username)
        # 2. Fetch deterministic spending summary
        spending = await spring_client.get_customer_spending_summary(customer_username)

        if not accounts and not spending:
            return {
                "agent": "FinancialInsightsAgent",
                "answer": f"No active bank accounts or transaction histories were found for customer profile '{customer_username}'. You can open an operative account via the Bank Accounts tab.",
                "sources": [],
                "data": None,
                "suggestedNextSteps": ["Open an account at /customer/dashboard"]
            }

        total_balance = sum([float(a.get("balance", 0.0)) for a in accounts])
        inflow = float(spending.get("totalInflow", 0.0)) if spending else 0.0
        outflow = float(spending.get("totalOutflow", 0.0)) if spending else 0.0
        net_savings = float(spending.get("netSavings", 0.0)) if spending else 0.0
        highest_cat = spending.get("highestSpendingCategory", "N/A") if spending else "N/A"
        categories = spending.get("categoryBreakdown", {}) if spending else {}

        # Format accounts summary table
        accounts_overview = "\n".join([
            f"• Account `{a.get('accountNumber')}` ({a.get('accountType')}): ₹{float(a.get('balance', 0.0)):,.2f} [{a.get('status')}]"
            for a in accounts
        ])

        categories_overview = "\n".join([
            f"• **{cat}**: ₹{float(amt):,.2f}"
            for cat, amt in categories.items() if float(amt) > 0
        ])

        deterministic_summary = (
            f"### 📊 Financial Overview for `{customer_username}`\n\n"
            f"**Total Available Balance:** ₹{total_balance:,.2f}\n"
            f"**Accounts:**\n{accounts_overview if accounts_overview else '• No operative accounts'}\n\n"
            f"#### 💰 Inflow & Outflow Summary:\n"
            f"• **Total Inflow (Credits):** ₹{inflow:,.2f}\n"
            f"• **Total Outflow (Debits):** ₹{outflow:,.2f}\n"
            f"• **Net Savings:** ₹{net_savings:,.2f}\n"
            f"• **Highest Spending Category:** `{highest_cat}`\n\n"
            f"#### 🏷️ Category Spending Breakdown:\n"
            f"{categories_overview if categories_overview else '• No category expenses recorded in current cycle.'}\n"
        )

        prompt = (
            "You are a Senior FinTech Wealth and Financial Health Analyst at BankCore360. "
            "Given the following exact, deterministic customer financial figures, provide an insightful, encouraging, "
            "and concise analysis. Highlight their savings rate and provide 2 actionable recommendations for liquidity management.\n"
            "CRITICAL: Do not invent or change any figures. Use strictly the provided numbers.\n\n"
            f"FINANCIAL DATA:\n{deterministic_summary}\n\n"
            f"USER QUERY: {query}"
        )

        try:
            llm_analysis = ask_grok(prompt)
            if not llm_analysis or len(llm_analysis) < 50:
                answer = deterministic_summary
            else:
                answer = f"{deterministic_summary}\n\n### 💡 Smart Financial Guidance:\n{llm_analysis}"
        except Exception:
            answer = deterministic_summary

        return {
            "agent": "FinancialInsightsAgent",
            "answer": answer,
            "sources": [
                {
                    "documentTitle": "Double-Entry Ledger Account Balances",
                    "filename": "PostgreSQL AccountBalance & LedgerEntries",
                    "similarityScore": 1.0,
                    "excerpt": f"Total Available: ₹{total_balance:,.2f}, Inflow: ₹{inflow:,.2f}, Outflow: ₹{outflow:,.2f}"
                }
            ],
            "data": {
                "totalBalance": total_balance,
                "totalInflow": inflow,
                "totalOutflow": outflow,
                "netSavings": net_savings,
                "highestCategory": highest_cat,
                "categories": categories
            },
            "suggestedNextSteps": [
                "Review ledger statements at /customer/dashboard",
                "Set automated recurring deposits to maximize quarterly interest"
            ]
        }

financial_insights_agent = FinancialInsightsAgent()
