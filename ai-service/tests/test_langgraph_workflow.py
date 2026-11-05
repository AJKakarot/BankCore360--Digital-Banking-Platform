"""
Tests for LangGraph StateGraph Workflow & Routing.
"""

import pytest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from graph.workflow import banking_workflow
from rag_pipeline import rag_pipeline

@pytest.fixture(autouse=True)
def ensure_policies_seeded():
    rag_pipeline.seed_sample_policies()

def test_workflow_eligibility_intent():
    res = banking_workflow.process("What is the minimum income to qualify for a loan?")
    assert res.intent == "ELIGIBILITY_QUERY"
    assert "₹15,000" in res.response or "minimum" in res.response.lower()
    assert len(res.sources) > 0
    assert "guaranteed loan sanction" in res.disclaimer.lower()

def test_workflow_documentation_intent():
    res = banking_workflow.process("Which documents do I need to upload for KYC?")
    assert res.intent == "DOCUMENT_QUERY"
    assert "PAN" in res.response
    assert len(res.suggestedNextSteps) >= 1

def test_workflow_emi_intent():
    res = banking_workflow.process("How is the monthly reducing balance EMI calculated?")
    assert res.intent == "EMI_QUERY"
    assert "reducing balance" in res.response.lower()

def test_workflow_application_status_intent():
    res = banking_workflow.process(
        message="What is the status of my loan application?",
        customer_username="dev_customer"
    )
    assert res.intent == "APPLICATION_STATUS_QUERY"
    assert "Maker-Checker-Manager" in res.response or "status" in res.response.lower()

def test_prompt_required_sample_questions():
    questions = [
        "What documents are required for an education loan?",
        "How can I apply for a personal loan?",
        "What is the loan eligibility criteria?",
        "What is the document verification process?",
        "What happens after Checker verification?",
        "What is the loan approval process?",
    ]
    for q in questions:
        res = banking_workflow.process(q)
        assert res.response is not None and len(res.response) > 20
        assert res.disclaimer is not None
        # Must return valid policy sources or explicit fallback
        assert (len(res.sources) > 0) or ("not explicitly covered" in res.response)


