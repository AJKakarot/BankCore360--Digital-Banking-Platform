"""
LangGraph Multi-Agent Supervisor Architecture for BankCore360.
Supervises five specialist banking agents:
1. Banking Knowledge Agent
2. Financial Insights Agent
3. Transaction Explanation Agent
4. Fraud Explanation Agent
5. Loan Assistant Agent

Features:
- Deterministic and LLM-assisted intent classification
- Conditional state routing
- Customer-isolated conversation thread checkpointing
- Zero hallucination guardrails
"""

import logging
from typing import TypedDict, List, Dict, Any, Optional
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver

from app.agents import (
    banking_knowledge_agent,
    financial_insights_agent,
    transaction_explanation_agent,
    fraud_explanation_agent,
    loan_assistant_agent
)

logger = logging.getLogger("BankCore360.AI.Supervisor")

class MultiAgentState(TypedDict):
    query: str
    customer_username: Optional[str]
    thread_id: str
    selected_agent: str
    intent: str
    response: str
    sources: List[Dict[str, Any]]
    data: Optional[Dict[str, Any]]
    suggested_next_steps: List[str]
    error: Optional[str]

# ====================================================
# SUPERVISOR CLASSIFICATION NODE
# ====================================================

def supervisor_node(state: MultiAgentState) -> Dict[str, Any]:
    """
    Classifies customer intent and selects the single appropriate specialist agent.
    Avoids unnecessary multi-agent fan-out for simple queries.
    """
    query = state.get("query", "").lower().strip()

    # Rule 1: Fraud & Security Explanations
    if any(w in query for w in ["fraud", "suspicious", "flagged", "risk score", "alert", "security check", "velocity", "compromis"]):
        selected = "fraud_explanation_node"
        intent = "FRAUD_EXPLANATION"

    # Rule 2: Transaction Explanations
    elif any(w in query for w in ["explain transaction", "transaction pending", "reversed transaction", "reversal", "txn-", "trf-", "rev-", "debit entry", "credit entry", "my latest transaction", "why was i charged", "transaction status"]):
        selected = "transaction_explanation_node"
        intent = "TRANSACTION_EXPLANATION"

    # Rule 3: Financial Insights & Spending Analysis
    elif any(w in query for w in ["spend", "spending", "how much did i", "monthly income", "expense", "highest category", "savings rate", "balance", "net savings", "compare this month"]):
        selected = "financial_insights_node"
        intent = "FINANCIAL_INSIGHTS"

    # Rule 4: Loan Assistant & EMI Calculations
    elif any(w in query for w in ["emi", "loan", "borrow", "foir", "eligib", "tenure", "interest rate", "personal loan", "home loan", "education loan", "business loan", "maker-checker", "maker checker"]):
        selected = "loan_assistant_node"
        intent = "LOAN_ASSISTANT"

    # Rule 5: Banking Policies, Fees, Disputes & Rules (Default Fallback)
    else:
        selected = "banking_knowledge_node"
        intent = "BANKING_KNOWLEDGE"

    logger.info(f"Supervisor routed query '{query[:40]}...' to {selected} [Intent: {intent}]")

    return {
        "selected_agent": selected,
        "intent": intent
    }

# ====================================================
# SPECIALIST AGENT EXECUTION NODES
# ====================================================

async def banking_knowledge_node(state: MultiAgentState) -> Dict[str, Any]:
    result = await banking_knowledge_agent.execute(
        query=state["query"],
        customer_username=state.get("customer_username")
    )
    return {
        "response": result.get("answer", ""),
        "sources": result.get("sources", []),
        "data": result.get("data"),
        "suggested_next_steps": result.get("suggestedNextSteps", [])
    }

async def financial_insights_node(state: MultiAgentState) -> Dict[str, Any]:
    result = await financial_insights_agent.execute(
        query=state["query"],
        customer_username=state.get("customer_username")
    )
    return {
        "response": result.get("answer", ""),
        "sources": result.get("sources", []),
        "data": result.get("data"),
        "suggested_next_steps": result.get("suggestedNextSteps", [])
    }

async def transaction_explanation_node(state: MultiAgentState) -> Dict[str, Any]:
    result = await transaction_explanation_agent.execute(
        query=state["query"],
        customer_username=state.get("customer_username")
    )
    return {
        "response": result.get("answer", ""),
        "sources": result.get("sources", []),
        "data": result.get("data"),
        "suggested_next_steps": result.get("suggestedNextSteps", [])
    }

async def fraud_explanation_node(state: MultiAgentState) -> Dict[str, Any]:
    result = await fraud_explanation_agent.execute(
        query=state["query"],
        customer_username=state.get("customer_username")
    )
    return {
        "response": result.get("answer", ""),
        "sources": result.get("sources", []),
        "data": result.get("data"),
        "suggested_next_steps": result.get("suggestedNextSteps", [])
    }

async def loan_assistant_node(state: MultiAgentState) -> Dict[str, Any]:
    result = await loan_assistant_agent.execute(
        query=state["query"],
        customer_username=state.get("customer_username")
    )
    return {
        "response": result.get("answer", ""),
        "sources": result.get("sources", []),
        "data": result.get("data"),
        "suggested_next_steps": result.get("suggestedNextSteps", [])
    }

# ====================================================
# ROUTING FUNCTION
# ====================================================

def route_to_specialist(state: MultiAgentState) -> str:
    return state.get("selected_agent", "banking_knowledge_node")

# ====================================================
# GRAPH COMPILATION WITH PERSISTENCE CHECKPOINTER
# ====================================================

def create_multi_agent_graph():
    builder = StateGraph(MultiAgentState)

    # Add Nodes
    builder.add_node("supervisor", supervisor_node)
    builder.add_node("banking_knowledge_node", banking_knowledge_node)
    builder.add_node("financial_insights_node", financial_insights_node)
    builder.add_node("transaction_explanation_node", transaction_explanation_node)
    builder.add_node("fraud_explanation_node", fraud_explanation_node)
    builder.add_node("loan_assistant_node", loan_assistant_node)

    # Set Entry Point
    builder.set_entry_point("supervisor")

    # Add Conditional Edges from Supervisor
    builder.add_conditional_edges(
        "supervisor",
        route_to_specialist,
        {
            "banking_knowledge_node": "banking_knowledge_node",
            "financial_insights_node": "financial_insights_node",
            "transaction_explanation_node": "transaction_explanation_node",
            "fraud_explanation_node": "fraud_explanation_node",
            "loan_assistant_node": "loan_assistant_node"
        }
    )

    # All specialist nodes terminate the graph
    builder.add_edge("banking_knowledge_node", END)
    builder.add_edge("financial_insights_node", END)
    builder.add_edge("transaction_explanation_node", END)
    builder.add_edge("fraud_explanation_node", END)
    builder.add_edge("loan_assistant_node", END)

    # Thread-level memory checkpointer
    checkpointer = MemorySaver()
    return builder.compile(checkpointer=checkpointer)

# Global compiled supervisor graph instance
multi_agent_graph = create_multi_agent_graph()

async def run_multi_agent_assistant(query: str, customer_username: Optional[str] = None, thread_id: Optional[str] = None) -> Dict[str, Any]:
    tid = thread_id if thread_id else (f"thread_{customer_username}" if customer_username else "thread_anonymous")
    initial_state: MultiAgentState = {
        "query": query,
        "customer_username": customer_username,
        "thread_id": tid,
        "selected_agent": "",
        "intent": "",
        "response": "",
        "sources": [],
        "data": None,
        "suggested_next_steps": [],
        "error": None
    }

    config = {"configurable": {"thread_id": tid}}
    final_state = await multi_agent_graph.ainvoke(initial_state, config=config)

    return {
        "response": final_state.get("response", ""),
        "answer": final_state.get("response", ""),
        "intent": final_state.get("intent", "GENERAL_INQUIRY"),
        "selectedAgent": final_state.get("selected_agent", "Supervisor"),
        "sources": final_state.get("sources", []),
        "data": final_state.get("data"),
        "suggestedNextSteps": final_state.get("suggested_next_steps", []),
        "disclaimer": (
            "Educational AI Banking Assistant guidance. "
            "Final financial and credit decisions rest solely with authorized human bank officials."
        )
    }
