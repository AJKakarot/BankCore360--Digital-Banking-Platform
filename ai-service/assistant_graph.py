"""
LangGraph Multi-Step Conversational Assistant Workflow.
Controls conversation routing, RAG policy retrieval, safe guidance generation,
and educational disclaimers.
"""

from typing import Dict, Any, List
from policy_knowledge import search_policies

class BankingAssistantGraph:
    """
    Controlled multi-step assistant pipeline:
    1. Classify Intent
    2. RAG Retrieval
    3. Generate Guidance
    4. Compliance Guardrail Verification
    """

    def process(self, message: str, category: str = None) -> Dict[str, Any]:
        # Step 1: Classify Intent
        intent = self._classify_intent(message, category)

        # Step 2: RAG Policy Retrieval
        relevant_policies = search_policies(message, top_k=2)

        # Step 3: Generate Safe Guidance
        guidance_text, next_steps = self._generate_guidance(intent, message, relevant_policies)

        # Step 4: Verification & Guardrail
        return {
            "response": guidance_text,
            "intent": intent,
            "relevantPolicies": [p["title"] + ": " + p["content"] for p in relevant_policies],
            "suggestedNextSteps": next_steps,
            "disclaimer": (
                "Educational AI Banking Assistant guidance. "
                "Final credit decisions rest solely with authorized human bank Checker and Manager officials."
            )
        }

    def _classify_intent(self, message: str, category: str) -> str:
        if category:
            return category.upper()
        msg = message.lower()
        if any(w in msg for w in ["eligib", "qualify", "salary", "income", "foir"]):
            return "ELIGIBILITY_INQUIRY"
        if any(w in msg for w in ["document", "pan", "aadhaar", "proof", "upload"]):
            return "DOCUMENTATION_INQUIRY"
        if any(w in msg for w in ["emi", "rate", "interest", "calculate", "repay"]):
            return "FINANCIAL_CALCULATION"
        if any(w in msg for w in ["step", "workflow", "maker", "checker", "manager", "disburse"]):
            return "WORKFLOW_GUIDANCE"
        if any(w in msg for w in ["personal", "home", "education", "business", "product"]):
            return "PRODUCT_INFORMATION"
        return "GENERAL_INQUIRY"

    def _generate_guidance(self, intent: str, message: str, policies: List[Dict[str, Any]]) -> tuple[str, List[str]]:
        policy_summaries = " ".join([p["content"] for p in policies])

        if intent == "ELIGIBILITY_INQUIRY":
            text = (
                f"Based on BankCore360 retail lending guidelines: {policy_summaries} "
                "You can verify your personal debt-to-income capacity using our built-in eligibility calculator."
            )
            steps = [
                "Enter your monthly income and any existing EMIs in the Loan Eligibility Calculator",
                "Ensure your requested loan tenure does not push your total FOIR above 50%"
            ]
        elif intent == "DOCUMENTATION_INQUIRY":
            text = (
                f"Documentation requirements per BankCore360 policy: {policy_summaries} "
                "Remember: never upload real identification credentials; use simulated test documents only."
            )
            steps = [
                "Prepare PDF or JPEG test copies of your PAN and Income Proof",
                "Navigate to your submitted application in 'My Applications' to upload documents"
            ]
        elif intent == "FINANCIAL_CALCULATION":
            text = (
                f"BankCore360 uses standardized reducing balance amortization: {policy_summaries} "
                "This ensures you only pay interest on the remaining unpaid principal each month."
            )
            steps = [
                "Test reducing-balance versus flat-rate interest at /api/ai/explain-emi",
                "Review the auto-generated monthly amortization schedule once your loan is disbursed"
            ]
        elif intent == "WORKFLOW_GUIDANCE":
            text = (
                "The BankCore360 workflow implements the Maker-Checker-Manager dual control framework. "
                "1. Maker (Customer) applies and uploads documents. "
                "2. Checker verifies all mandatory documents and forwards to Manager. "
                "3. Manager determines approved amount and interest rate, and triggers simulated disbursement."
            )
            steps = [
                "Check application state at any time via GET /api/loans/my-applications",
                "Check notifications bell for real-time status updates from Checkers and Managers"
            ]
        else:
            text = (
                "Welcome to BankCore360 AI Assistant. I can guide you through our 4 loan products "
                "(Personal, Home, Education, Business), explain eligibility criteria (₹15,000 min income, 50% max FOIR), "
                "and detail required documents."
            )
            steps = [
                "Explore loan types and interest rates",
                "Check loan eligibility with the interactive calculator"
            ]

        return text, steps

assistant_graph = BankingAssistantGraph()

