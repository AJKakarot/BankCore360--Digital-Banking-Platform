from typing import TypedDict
from langgraph.graph import StateGraph, START, END
from langsmith import traceable

from .ingestion import (
    load_policy,
    split_into_chunks,
    retrieve_chunks,
    format_source,
)
from .grok_client import ask_grok


class PolicyState(TypedDict):
    question: str
    chunks: list[str]
    sources: list[dict]
    attempts: int
    answer: str


MAX_ATTEMPTS = 2

FALLBACK_ANSWER = (
    "I could not find sufficiently relevant information "
    "in the BankCore360 loan policies."
)


def retrieve_node(state: PolicyState) -> dict:
    """Retrieve BankCore360 loan policy sections for the question."""
    policy_text = load_policy()
    all_chunks = split_into_chunks(policy_text)

    retrieved = retrieve_chunks(
        state["question"],
        all_chunks,
        top_k=1,
        min_similarity=0.35,
    )

    return {
        "chunks": retrieved,
        "sources": [format_source(chunk) for chunk in retrieved],
        "attempts": state.get("attempts", 0) + 1,
    }


def grade_node(state: PolicyState) -> dict:
    """Grade whether retrieved loan policy context is relevant."""
    if not state.get("chunks"):
        return {"answer": "NO"}

    prompt = f"""
You are checking relevance for the BankCore360 Loan Policy Assistant.

Question:
{state["question"]}

Retrieved policy context:
{state["chunks"]}

Does this context contain information relevant to answering the loan question?
Reply with exactly one word: YES or NO.
"""

    result = ask_grok(prompt).strip().upper()
    return {"answer": "YES" if result.startswith("YES") else "NO"}


def route_after_grade(state: PolicyState) -> str:
    """Choose answer generation, retry, or safe fallback."""
    if state.get("chunks") and state.get("answer") == "YES":
        return "generate"

    if state.get("attempts", 0) < MAX_ATTEMPTS:
        return "rewrite"

    return "fallback"


def rewrite_node(state: PolicyState) -> dict:
    """Rewrite the loan query before the single allowed retry."""
    prompt = f"""
Rewrite the following loan question to improve retrieval against BankCore360 policy documents.
Preserve the user's intent. Do not answer the question or invent loan terms.

Original question:
{state["question"]}

Return only the rewritten question.
"""

    rewritten = ask_grok(prompt).strip()

    return {
        "question": rewritten or state["question"],
        "answer": "",
    }


def generate_node(state: PolicyState) -> dict:
    """Generate an answer grounded in retrieved BankCore360 loan context."""
    context = "\n\n".join(state["chunks"])

    prompt = f"""
You are the official AI Banking Assistant for BankCore360 Digital Loan Management Platform.

Answer using ONLY the supplied policy context.
Do not invent loan terms, interest rates, or eligibility rules.
If the context is insufficient, clearly say so.
Cite factual claims using the policy ID and section.

Policy context:
{context}

Source metadata:
{state["sources"]}

Question:
{state["question"]}
"""

    return {"answer": ask_grok(prompt)}


def fallback_node(state: PolicyState) -> dict:
    """Return a safe response when retrieval remains irrelevant."""
    return {
        "answer": FALLBACK_ANSWER,
        "sources": [],
    }


builder = StateGraph(PolicyState)

builder.add_node("retrieve", retrieve_node)
builder.add_node("grade", grade_node)
builder.add_node("rewrite", rewrite_node)
builder.add_node("generate", generate_node)
builder.add_node("fallback", fallback_node)

builder.add_edge(START, "retrieve")
builder.add_edge("retrieve", "grade")

builder.add_conditional_edges(
    "grade",
    route_after_grade,
    {
        "generate": "generate",
        "rewrite": "rewrite",
        "fallback": "fallback",
    },
)

builder.add_edge("rewrite", "retrieve")
builder.add_edge("generate", END)
builder.add_edge("fallback", END)

policy_graph = builder.compile()


@traceable(name="bankcore360-loan-policy-langgraph")
def run_policy_graph(question: str) -> dict:
    """Execute the loan policy graph and return the API response."""
    result = policy_graph.invoke(
        {
            "question": question,
            "chunks": [],
            "sources": [],
            "attempts": 0,
            "answer": "",
        }
    )

    return {
        "question": question,
        "answer": result["answer"],
        "sources": result["sources"],
    }
