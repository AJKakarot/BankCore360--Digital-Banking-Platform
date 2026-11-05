"""
LangGraph Typed State for BankCore360 Conversational Assistant.
"""

from typing import TypedDict, Optional, List, Dict, Any

class BankingChatState(TypedDict, total=False):
    # Inputs
    message: str
    category: Optional[str]
    loan_type: Optional[str]
    customer_username: Optional[str]
    application_reference: Optional[str]

    # Pipeline Processing State
    intent: str
    rewritten_query: str
    retrieved_chunks: List[Dict[str, Any]]
    is_context_sufficient: bool
    raw_answer: str
    validated_answer: str

    # Outputs
    sources: List[Dict[str, Any]]
    relevant_policies: List[str]
    suggested_next_steps: List[str]
    guardrail_warnings: List[str]
    disclaimer: str

