"""
Fraud Explanation Agent for BankCore360.
Explains fraud risk assessments, risk scores (0-100), triggered velocity and anomaly rules,
and investigation statuses.
CRITICAL FINTECH INVARIANT:
The AI is strictly an explainer and observer.
It CANNOT approve, reject, clear flags, or modify any fraud detection decisions.
"""

import logging
from typing import Dict, Any, Optional
from spring_client import spring_client
from app.grok_client import ask_grok

logger = logging.getLogger("BankCore360.AI.FraudExplanationAgent")

class FraudExplanationAgent:

    async def execute(self, query: str, customer_username: Optional[str] = None) -> Dict[str, Any]:
        if not customer_username:
            return {
                "agent": "FraudExplanationAgent",
                "answer": "Please log in to your BankCore360 account to view your security and fraud risk profile.",
                "sources": [],
                "data": None,
                "suggestedNextSteps": ["Log in to view security profile"]
            }

        # Fetch customer fraud metadata from Spring Boot
        metadata = await spring_client.get_customer_fraud_metadata(customer_username)

        if not metadata:
            return {
                "agent": "FraudExplanationAgent",
                "answer": f"No fraud assessments or risk alerts are currently recorded for customer '{customer_username}'. Your account is in good security standing.",
                "sources": [],
                "data": None,
                "suggestedNextSteps": ["Enable 2FA / OTP under Security Settings for heightened protection"]
            }

        avg_score = metadata.get("averageRiskScore", 10)
        overall_level = metadata.get("overallRiskLevel", "LOW")
        active_alerts = metadata.get("activeAlertsCount", 0)
        assessments = metadata.get("recentAssessments", [])

        risk_color = "🟢" if avg_score <= 30 else "🟡" if avg_score <= 60 else "🟠" if avg_score <= 80 else "🔴"

        recent_assessments_text = ""
        if assessments:
            recent_assessments_text = "\n".join([
                f"• **Txn Ref:** `{a.get('transactionReference')}` | **Score:** {a.get('riskScore')}/100 ({a.get('riskLevel')}) | **Decision:** `{a.get('decision')}`\n"
                f"  *Triggered Rules:* {', '.join(a.get('triggeredRules', [])) if a.get('triggeredRules') else 'None (Normal heuristics)'}"
                for a in assessments[:3]
            ])
        else:
            recent_assessments_text = "• No suspicious activity or flagged transactions detected."

        deterministic_summary = (
            f"### 🛡️ Fraud & Security Risk Evaluation for `{customer_username}`\n\n"
            f"• **Overall Account Risk Score:** {risk_color} **{avg_score} / 100** ({overall_level} RISK)\n"
            f"• **Active Security Alerts for Review:** {active_alerts}\n\n"
            f"#### 🔍 Recent Pre-Settlement Evaluations:\n"
            f"{recent_assessments_text}\n\n"
            "> [!IMPORTANT]\n"
            "> **Compliance Notice:** The AI Assistant cannot alter, dismiss, or approve fraud alerts. "
            "All risk classifications are deterministically evaluated by the BankCore360 Real-Time Fraud Engine. "
            "Flagged transactions are routed directly to authorized Bank Compliance Officers for formal Maker-Checker review."
        )

        prompt = (
            "You are a Banking Risk and Cybersecurity Compliance Officer at BankCore360. "
            "Explain the following fraud risk assessment to the customer in a reassuring, professional tone. "
            "Explain how velocity checks and anomalous transaction amounts factor into the risk score without sharing internal security thresholds.\n"
            "MANDATORY: Remind the customer that AI cannot clear or modify fraud flags.\n\n"
            f"FRAUD EVALUATION DATA:\n{deterministic_summary}\n\n"
            f"CUSTOMER INQUIRY: {query}"
        )

        try:
            llm_text = ask_grok(prompt)
            if not llm_text or len(llm_text) < 40:
                answer = deterministic_summary
            else:
                answer = f"{deterministic_summary}\n\n### 🛡️ Risk Advisory & Explanations:\n{llm_text}"
        except Exception:
            answer = deterministic_summary

        return {
            "agent": "FraudExplanationAgent",
            "answer": answer,
            "sources": [
                {
                    "documentTitle": "BankCore360 Real-Time Fraud Engine Evaluation",
                    "filename": "Redis Velocity & PostgreSQL FraudAssessment",
                    "similarityScore": 1.0,
                    "excerpt": f"Risk Score: {avg_score}/100 ({overall_level}), Active Alerts: {active_alerts}"
                }
            ],
            "data": metadata,
            "suggestedNextSteps": [
                "Review active alerts at /admin/fraud if you hold staff privileges",
                "Contact Fraud Desk at 1800-360-SAFE if you suspect account compromise"
            ]
        }

fraud_explanation_agent = FraudExplanationAgent()

