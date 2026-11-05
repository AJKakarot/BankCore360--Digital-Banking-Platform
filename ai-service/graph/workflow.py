"""
Compiled LangGraph Workflow for BankCore360 Conversational Assistant.
"""

from typing import Optional, Dict, Any
from langgraph.graph import StateGraph, END

from graph.state import BankingChatState
from graph.nodes import (
    query_classifier,
    query_rewriter,
    retriever,
    context_validator,
    ai_response_generator,
    response_validator,
    final_response,
    application_status_node,
    EDUCATIONAL_DISCLAIMER
)
from graph.edges import route_after_classifier
from schemas import ChatResponse, SourceCitation

def create_banking_graph():
    workflow = StateGraph(BankingChatState)

    # Add Nodes
    workflow.add_node("query_classifier", query_classifier)
    workflow.add_node("application_status_node", application_status_node)
    workflow.add_node("query_rewriter", query_rewriter)
    workflow.add_node("retriever", retriever)
    workflow.add_node("context_validator", context_validator)
    workflow.add_node("ai_response_generator", ai_response_generator)
    workflow.add_node("response_validator", response_validator)
    workflow.add_node("final_response", final_response)

    # Set Entry Point
    workflow.set_entry_point("query_classifier")

    # Conditional Routing from Classifier
    workflow.add_conditional_edges(
        "query_classifier",
        route_after_classifier,
        {
            "application_status_node": "application_status_node",
            "query_rewriter": "query_rewriter"
        }
    )

    # Application Status Branch -> final_response
    workflow.add_edge("application_status_node", "final_response")

    # RAG Retrieval Pipeline Edges
    workflow.add_edge("query_rewriter", "retriever")
    workflow.add_edge("retriever", "context_validator")
    workflow.add_edge("context_validator", "ai_response_generator")
    workflow.add_edge("ai_response_generator", "response_validator")
    workflow.add_edge("response_validator", "final_response")
    workflow.add_edge("final_response", END)

    return workflow.compile()

class BankingAssistantWorkflow:
    def __init__(self):
        self.app = create_banking_graph()

    def process(self, message: str, category: Optional[str] = None,
                loan_type: Optional[str] = None,
                customer_username: Optional[str] = None,
                application_reference: Optional[str] = None) -> ChatResponse:
        initial_state: BankingChatState = {
            "message": message,
            "category": category,
            "loan_type": loan_type,
            "customer_username": customer_username,
            "application_reference": application_reference,
            "intent": "",
            "rewritten_query": "",
            "retrieved_chunks": [],
            "is_context_sufficient": False,
            "raw_answer": "",
            "validated_answer": "",
            "sources": [],
            "relevant_policies": [],
            "suggested_next_steps": [],
            "guardrail_warnings": [],
            "disclaimer": EDUCATIONAL_DISCLAIMER
        }

        final_state = self.app.invoke(initial_state)

        # Convert sources to SourceCitation Pydantic objects
        sources_dto = [
            SourceCitation(
                documentId=s.get("documentId", 1),
                documentName=s.get("documentName", "Policy"),
                policyCategory=s.get("policyCategory", "Lending"),
                version=s.get("version", "1.0"),
                pageNumber=s.get("pageNumber", 1),
                similarityScore=float(s.get("similarityScore", 0.0)),
                excerpt=s.get("excerpt", "")
            )
            for s in final_state.get("sources", [])
        ]

        return ChatResponse(
            response=final_state.get("validated_answer") or final_state.get("raw_answer", ""),
            intent=final_state.get("intent", "GENERAL_BANKING"),
            sources=sources_dto,
            relevantPolicies=final_state.get("relevant_policies", []),
            suggestedNextSteps=final_state.get("suggested_next_steps", []),
            guardrailWarnings=final_state.get("guardrail_warnings", []),
            disclaimer=final_state.get("disclaimer", EDUCATIONAL_DISCLAIMER)
        )

# Global singleton instance
banking_workflow = BankingAssistantWorkflow()

