"""
Transaction Explanation Agent for BankCore360.
Explains specific customer transactions, double-entry debit/credit ledger postings,
pending or failed stages, and transaction reversals with verified customer ownership.
Never invents transaction context.
"""

import re
import logging
from typing import Dict, Any, Optional
from spring_client import spring_client
from app.grok_client import ask_grok

logger = logging.getLogger("BankCore360.AI.TransactionExplanationAgent")

class TransactionExplanationAgent:

    async def execute(self, query: str, customer_username: Optional[str] = None) -> Dict[str, Any]:
        if not customer_username:
            return {
                "agent": "TransactionExplanationAgent",
                "answer": "Please log in to your BankCore360 account to inspect and explain your transaction records.",
                "sources": [],
                "data": None,
                "suggestedNextSteps": ["Log in to view transactions"]
            }

        # Check if query references a specific transaction ID or reference (e.g. TXN-..., TRF-..., or number)
        txn_ref_match = re.search(r'\b(TXN-[A-Za-z0-9\-]+|TRF-[A-Za-z0-9\-]+|REV-[A-Za-z0-9\-]+|\d{1,8})\b', query)

        target_txn = None
        if txn_ref_match:
            ref_or_id = txn_ref_match.group(1)
            target_txn = await spring_client.get_transaction_detail(customer_username, ref_or_id)
            if target_txn and target_txn.get("error") == "ACCESS_DENIED":
                return {
                    "agent": "TransactionExplanationAgent",
                    "answer": f"⚠️ **Authorization Restricted:** You do not have ownership rights to view transaction `{ref_or_id}`. In accordance with BankCore360 security policies, customers may only inspect transactions associated with their own accounts.",
                    "sources": [],
                    "data": None,
                    "suggestedNextSteps": ["Verify transaction ID from your account statement"]
                }

        # If no specific transaction was given or found by ref, fetch recent customer transactions
        txns = await spring_client.get_customer_transactions(customer_username)
        if not target_txn and txns:
            target_txn = txns[0]  # Most recent transaction

        if not target_txn:
            return {
                "agent": "TransactionExplanationAgent",
                "answer": f"No transaction history was found for account '{customer_username}'. When you initiate transfers, deposits, or withdrawals, immutable double-entry records will appear here.",
                "sources": [],
                "data": None,
                "suggestedNextSteps": ["Initiate a deposit or transfer from your customer dashboard"]
            }

        ref = target_txn.get("transactionReference", "N/A")
        amt = float(target_txn.get("amount", 0.0))
        direction = target_txn.get("direction", "DEBIT")
        status = target_txn.get("status", "POSTED")
        txn_type = target_txn.get("type", "TRANSFER")
        acc_num = target_txn.get("accountNumber", "N/A")
        is_reversed = target_txn.get("reversed", False)
        timestamp = target_txn.get("timestamp", "Recent")

        explanation_lines = [
            f"### 🧾 Transaction Breakdown: `{ref}`\n",
            f"• **Amount:** ₹{amt:,.2f}",
            f"• **Direction:** {direction} ({'Funds deducted from' if direction == 'DEBIT' else 'Funds credited to'} account `{acc_num}`)",
            f"• **Transaction Type:** `{txn_type}`",
            f"• **Status:** `{status}`",
            f"• **Posting Date:** {timestamp}",
            f"• **Reversal Flag:** {'⚠️ REVERSED (Compensating entry posted)' if is_reversed else '✅ Settled & Irrevocable'}"
        ]

        if is_reversed:
            explanation_lines.append(
                "\n> [!NOTE]\n> This transaction has been formally reversed. In our double-entry ledger system, "
                "original entries are never erased; rather, an inverse compensating entry was posted to neutralize the balance impact."
            )

        fact_text = "\n".join(explanation_lines)

        prompt = (
            "You are a Banking Ledger Support Specialist at BankCore360. "
            "Explain the following factual transaction to the customer in simple, transparent terms. "
            "Clarify why it is marked as debit or credit and what its current status signifies. "
            "Never invent parties, amounts, or merchant names not present in the facts.\n\n"
            f"TRANSACTION DATA:\n{fact_text}\n\n"
            f"CUSTOMER QUESTION: {query}"
        )

        try:
            llm_text = ask_grok(prompt)
            if not llm_text or len(llm_text) < 40:
                answer = fact_text
            else:
                answer = f"{fact_text}\n\n### 💬 Plain English Explanation:\n{llm_text}"
        except Exception:
            answer = fact_text

        return {
            "agent": "TransactionExplanationAgent",
            "answer": answer,
            "sources": [
                {
                    "documentTitle": f"Double-Entry Ledger Transaction {ref}",
                    "filename": "PostgreSQL LedgerTransaction",
                    "similarityScore": 1.0,
                    "excerpt": f"Ref: {ref}, Amount: ₹{amt:,.2f}, Type: {txn_type}, Status: {status}"
                }
            ],
            "data": target_txn,
            "suggestedNextSteps": [
                f"Download PDF receipt for reference {ref}",
                "Contact customer support if this entry requires dispute investigation"
            ]
        }

transaction_explanation_agent = TransactionExplanationAgent()
