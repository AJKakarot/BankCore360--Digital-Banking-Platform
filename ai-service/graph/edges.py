"""
LangGraph Conditional Routing Logic for BankCore360 Conversational Assistant.
"""

from graph.state import BankingChatState

def route_after_classifier(state: BankingChatState) -> str:
    """
    Routes between application status lookup and the RAG retrieval pipeline.
    """
    intent = state.get("intent", "GENERAL_BANKING")
    if intent == "APPLICATION_STATUS_QUERY":
        return "application_status_node"
    return "query_rewriter"

