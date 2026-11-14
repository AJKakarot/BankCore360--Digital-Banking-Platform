"""
Comprehensive unit and integration tests for the LangGraph Multi-Agent AI Banking Assistant.
Tests:
- Supervisor intent classification and routing
- Five specialist agents (Knowledge, Financial Insights, Transactions, Fraud, Loan Assistant)
- Customer-level data isolation & authorization checks
- Deterministic reducing-balance EMI calculations
- Factual fallbacks on missing policies
- Thread-level isolation
"""

import pytest
from unittest.mock import AsyncMock, patch
from app.multi_agent_graph import run_multi_agent_assistant, supervisor_node, MultiAgentState
from app.agents import (
    banking_knowledge_agent,
    financial_insights_agent,
    transaction_explanation_agent,
    fraud_explanation_agent,
    loan_assistant_agent
)

# ====================================================================
# 1. SUPERVISOR INTENT CLASSIFICATION & ROUTING TESTS
# ====================================================================

def test_supervisor_routes_fraud_query():
    state: MultiAgentState = {
        "query": "Why was my transaction flagged as suspicious with a high risk score?",
        "customer_username": "alice_cust",
        "thread_id": "t1",
        "selected_agent": "",
        "intent": "",
        "response": "",
        "sources": [],
        "data": None,
        "suggested_next_steps": [],
        "error": None
    }
    result = supervisor_node(state)
    assert result["selected_agent"] == "fraud_explanation_node"
    assert result["intent"] == "FRAUD_EXPLANATION"

def test_supervisor_routes_transaction_query():
    state: MultiAgentState = {
        "query": "Can you explain transaction TXN-2026-ABCD1234 and why it was reversed?",
        "customer_username": "alice_cust",
        "thread_id": "t1",
        "selected_agent": "",
        "intent": "",
        "response": "",
        "sources": [],
        "data": None,
        "suggested_next_steps": [],
        "error": None
    }
    result = supervisor_node(state)
    assert result["selected_agent"] == "transaction_explanation_node"
    assert result["intent"] == "TRANSACTION_EXPLANATION"

def test_supervisor_routes_financial_insights_query():
    state: MultiAgentState = {
        "query": "How much did I spend this month and what is my highest spending category?",
        "customer_username": "alice_cust",
        "thread_id": "t1",
        "selected_agent": "",
        "intent": "",
        "response": "",
        "sources": [],
        "data": None,
        "suggested_next_steps": [],
        "error": None
    }
    result = supervisor_node(state)
    assert result["selected_agent"] == "financial_insights_node"
    assert result["intent"] == "FINANCIAL_INSIGHTS"

def test_supervisor_routes_loan_query():
    state: MultiAgentState = {
        "query": "Calculate my EMI for a personal loan of 500000 at 10.5% for 3 years",
        "customer_username": "alice_cust",
        "thread_id": "t1",
        "selected_agent": "",
        "intent": "",
        "response": "",
        "sources": [],
        "data": None,
        "suggested_next_steps": [],
        "error": None
    }
    result = supervisor_node(state)
    assert result["selected_agent"] == "loan_assistant_node"
    assert result["intent"] == "LOAN_ASSISTANT"

def test_supervisor_routes_general_policy_query():
    state: MultiAgentState = {
        "query": "What are the ATM withdrawal fees and wire transfer charges?",
        "customer_username": "alice_cust",
        "thread_id": "t1",
        "selected_agent": "",
        "intent": "",
        "response": "",
        "sources": [],
        "data": None,
        "suggested_next_steps": [],
        "error": None
    }
    result = supervisor_node(state)
    assert result["selected_agent"] == "banking_knowledge_node"
    assert result["intent"] == "BANKING_KNOWLEDGE"

# ====================================================================
# 2. DETERMINISTIC FINANCIAL CALCULATIONS
# ====================================================================

def test_deterministic_reducing_balance_emi_calculation():
    # Principal: 100,000, Annual Rate: 12%, Tenure: 12 months
    # Formula monthly rate r = 0.01
    # EMI = 100000 * 0.01 * (1.01)^12 / ((1.01)^12 - 1) = 8884.88
    emi = loan_assistant_agent.calculate_emi_deterministic(100000.0, 12.0, 12)
    assert emi == 8884.88

    # Edge cases
    assert loan_assistant_agent.calculate_emi_deterministic(0.0, 10.0, 12) == 0.0
    assert loan_assistant_agent.calculate_emi_deterministic(10000.0, 0.0, 10) == 1000.0

# ====================================================================
# 3. SPECIALIST AGENT ASYNC EXECUTION TESTS
# ====================================================================

@pytest.mark.anyio
async def test_banking_knowledge_retrieves_policy():
    res = await banking_knowledge_agent.execute("What is the ATM withdrawal limit?")
    assert res["agent"] == "BankingKnowledgeAgent"
    assert "ATM" in res["answer"] or "limit" in res["answer"].lower()
    assert len(res["sources"]) > 0

@pytest.mark.anyio
async def test_banking_knowledge_fallback_on_irrelevant_query():
    res = await banking_knowledge_agent.execute("How to bake chocolate cake with strawberries?")
    assert res["agent"] == "BankingKnowledgeAgent"
    assert "could not find sufficiently relevant information" in res["answer"].lower()

@pytest.mark.anyio
async def test_financial_insights_unauthenticated_guardrail():
    res = await financial_insights_agent.execute("Show my spending", customer_username=None)
    assert "log in" in res["answer"].lower()
    assert res["data"] is None

@pytest.mark.anyio
async def test_financial_insights_with_mocked_spring_data():
    mock_accounts = [{"accountNumber": "360100000101", "accountType": "SAVINGS", "balance": 150000.00, "status": "ACTIVE"}]
    mock_spending = {
        "totalInflow": 200000.00,
        "totalOutflow": 50000.00,
        "netSavings": 150000.00,
        "highestSpendingCategory": "TRANSFERS_OUT",
        "categoryBreakdown": {"TRANSFERS_OUT": 30000.00, "WITHDRAWALS": 20000.00}
    }

    with patch("app.agents.financial_insights_agent.spring_client.get_customer_accounts", new_callable=AsyncMock) as m_acc, \
         patch("app.agents.financial_insights_agent.spring_client.get_customer_spending_summary", new_callable=AsyncMock) as m_sp:
        m_acc.return_value = mock_accounts
        m_sp.return_value = mock_spending

        res = await financial_insights_agent.execute("What did I spend?", customer_username="alice_cust")
        assert res["agent"] == "FinancialInsightsAgent"
        assert "150,000.00" in res["answer"]
        assert res["data"]["totalInflow"] == 200000.00
        assert res["data"]["highestCategory"] == "TRANSFERS_OUT"

@pytest.mark.anyio
async def test_transaction_explanation_access_denied_guardrail():
    # Customer Alice tries to inspect transaction of Bob
    mock_denied = {"error": "ACCESS_DENIED", "message": "You do not have authorization"}

    with patch("app.agents.transaction_explanation_agent.spring_client.get_transaction_detail", new_callable=AsyncMock) as m_tx:
        m_tx.return_value = mock_denied

        res = await transaction_explanation_agent.execute("Explain transaction TXN-BOB-999", customer_username="alice_cust")
        assert res["agent"] == "TransactionExplanationAgent"
        assert "Authorization Restricted" in res["answer"]

@pytest.mark.anyio
async def test_fraud_explanation_preserves_score_and_disclaimer():
    mock_fraud = {
        "customerUsername": "alice_cust",
        "averageRiskScore": 72,
        "overallRiskLevel": "HIGH",
        "activeAlertsCount": 1,
        "recentAssessments": [
            {
                "transactionReference": "TXN-2026-X1Y2",
                "riskScore": 72,
                "riskLevel": "HIGH",
                "decision": "FLAGGED",
                "triggeredRules": ["HIGH_VELOCITY_CHECK", "LARGE_AMOUNT_THRESHOLD"]
            }
        ]
    }

    with patch("app.agents.fraud_explanation_agent.spring_client.get_customer_fraud_metadata", new_callable=AsyncMock) as m_frd:
        m_frd.return_value = mock_fraud

        res = await fraud_explanation_agent.execute("Why was my transaction flagged?", customer_username="alice_cust")
        assert res["agent"] == "FraudExplanationAgent"
        assert "72" in res["answer"]
        assert "HIGH RISK" in res["answer"]
        assert "AI Assistant cannot alter, dismiss, or approve fraud alerts" in res["answer"]

@pytest.mark.anyio
async def test_loan_assistant_calculates_emi_accurately():
    res = await loan_assistant_agent.execute("Calculate EMI for 500000 at 10% for 24 months")
    assert res["agent"] == "LoanAssistantAgent"
    assert "Reducing Balance EMI Calculation" in res["answer"]
    assert res["data"]["principal"] == 500000.0
    assert res["data"]["tenureMonths"] == 24
    assert res["data"]["monthlyEmi"] > 0

# ====================================================================
# 4. END-TO-END SUPERVISOR GRAPH EXECUTION
# ====================================================================

@pytest.mark.anyio
async def test_end_to_end_supervisor_graph():
    res = await run_multi_agent_assistant(
        query="What are the account maintenance charges?",
        customer_username="alice_cust",
        thread_id="test_thread_001"
    )
    assert res["selectedAgent"] == "banking_knowledge_node"
    assert res["intent"] == "BANKING_KNOWLEDGE"
    assert len(res["answer"]) > 20
    assert "Educational AI Banking Assistant guidance" in res["disclaimer"]
