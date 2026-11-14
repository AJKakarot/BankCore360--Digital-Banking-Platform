# BankCore360 – Multi-Agent AI Banking Assistant Implementation Guide

**Date:** November 2026  
**Architecture:** LangGraph Supervisor Multi-Agent Architecture with 5 Specialist Banking Agents  
**Backend:** Java 21 / Spring Boot 3.4.3 + Python 3.14 / FastAPI 0.110  
**Data Isolation:** Customer-authenticated Internal Banking APIs + Memory Checkpointing  
**Frontend:** Next.js 16 (App Router) Customer AI Interface with 5 Quick Action Tabs  

---

## 1. Architectural Blueprint

```
                      ┌────────────────────────────────────────┐
                      │    Next.js Customer AI Interface       │
                      │       (/customer/ai-assistant)         │
                      └──────────────────┬─────────────────────┘
                                         │  HTTP /api/ai/assistant/chat
                                         │  [X-Authenticated-User: alice_cust]
                                         ▼
                      ┌────────────────────────────────────────┐
                      │          FastAPI AI Gateway            │
                      └──────────────────┬─────────────────────┘
                                         │
                                         ▼
                      ┌────────────────────────────────────────┐
                      │       LangGraph Supervisor Node        │
                      │         (Intent Classification)        │
                      └──────┬──────┬──────┬──────┬──────┬─────┘
                             │      │      │      │      │
            ┌────────────────┘      │      │      │      └────────────────┐
            ▼                       ▼      │      ▼                       ▼
┌──────────────────────┐ ┌──────────────┐  │ ┌───────────────┐ ┌─────────────────────┐
│  Banking Knowledge   │ │  Financial   │  │ │     Fraud     │ │   Loan Assistant    │
│        Agent         │ │Insights Agent│  │ │  Explanation  │ │        Agent        │
│    (Grounded RAG)    │ │(Deterministic│  │ │     Agent     │ │   (Reducing EMI,    │
└──────────┬───────────┘ │ Inflow/Out)  │  │ │(Risk Heuristic│ │ Catalog, Checklist) │
           │             └──────┬───────┘  │ │   Advisor)    │ └──────────┬──────────┘
           │                    │          │ └───────┬───────┘            │
           │                    │          ▼         │                    │
           │                    │ ┌───────────────┐  │                    │
           │                    │ │  Transaction  │  │                    │
           │                    │ │  Explanation  │  │                    │
           │                    │ │     Agent     │  │                    │
           │                    │ │ (Ledger Audit)│  │                    │
           │                    │ └───────┬───────┘  │                    │
           │                    │         │          │                    │
           ▼                    ▼         ▼          ▼                    ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│              Secure Banking Tools (spring_client.py)                             │
│     Header: X-Internal-Service-Token (Shared secret)                             │
│     Header: X-Authenticated-User (Verified customer identity)                    │
└──────────────────────────────────────┬───────────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│              Spring Boot Internal Service Controller                             │
│                 (InternalAiBankingController.java)                               │
│     • /api/internal/customer/accounts                                            │
│     • /api/internal/customer/spending-summary                                    │
│     • /api/internal/customer/transactions                                        │
│     • /api/internal/customer/transactions/{ref} (Ownership check)                │
│     • /api/internal/customer/fraud-metadata                                      │
│     • /api/internal/loans/products                                               │
└──────────────────────────────────────┬───────────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│             PostgreSQL 16 (Double-Entry Ledger, Fraud, Loans) & Redis 7          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Five Specialist Agents

### 1. Banking Knowledge Agent
- **File:** `ai-service/app/agents/banking_knowledge_agent.py`
- **Responsibilities:** Ingests and performs grounded semantic vector search over BankCore360 fee schedules, transfer policies (NEFT, RTGS, IMPS, Wire), ATM/POS limits, and dispute/reversal guidelines.
- **Guardrail:** Never invents charges. Returns explicit fallback when policies are not indexed.

### 2. Financial Insights Agent
- **File:** `ai-service/app/agents/financial_insights_agent.py`
- **Responsibilities:** Connects to `InternalAiBankingService` to extract real account balances, compute deterministic total inflow/outflow, identify top spending categories, and generate personalized savings advice.
- **Guardrail:** All financial numbers are computed deterministically. The LLM only interprets calculated metrics.

### 3. Transaction Explanation Agent
- **File:** `ai-service/app/agents/transaction_explanation_agent.py`
- **Responsibilities:** Explains individual transaction references, debit/credit direction, posted ledger entries, and reversal entries.
- **Guardrail:** Enforces strict customer ownership. Rejects attempts by Customer A to inspect Customer B's transactions.

### 4. Fraud Explanation Agent
- **File:** `ai-service/app/agents/fraud_explanation_agent.py`
- **Responsibilities:** Explains real-time fraud risk scores (0-100), triggered velocity rules, and suspicious anomaly patterns.
- **Guardrail:** **Authoritative Precedence:** The AI is strictly an observer/explainer. It CANNOT clear fraud flags, approve transactions, or modify fraud engine decisions.

### 5. Loan Assistant Agent
- **File:** `ai-service/app/agents/loan_assistant_agent.py`
- **Responsibilities:** Calculates reducing-balance monthly EMIs deterministically ($E = P \cdot r \cdot \frac{(1+r)^n}{(1+r)^n - 1}$), compares retail loan products, explains required KYC documentation, and tracks Maker-Checker application progress.
- **Guardrail:** Cannot sanction or approve loans. Reminds customers of Branch Manager authority.

---

## 3. LangGraph Supervisor Architecture

- **File:** `ai-service/app/multi_agent_graph.py`
- **State Definition:** `MultiAgentState` (TypedDict with query, customer username, thread ID, selected agent, intent, response, sources, data, suggested next steps).
- **Supervisor Routing:** Uses fast heuristic and semantic intent matching to route directly to the single specialist agent needed, avoiding multi-agent token waste.
- **Memory Checkpointing:** `MemorySaver` isolates conversation threads by customer username and conversation session.

---

## 4. Secure Spring Boot Service Integration

- **Controller:** [`InternalAiBankingController.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/controller/InternalAiBankingController.java)
- **Service:** [`InternalAiBankingService.java`](file:///Users/ajeet/AjeetFOLIO/BankCore360/src/main/java/com/bankcore360/service/InternalAiBankingService.java)
- **Security Invariant:**
  - `X-Internal-Service-Token` must match `bankcore360-internal-ai-token`. Missing/invalid token returns HTTP 401.
  - `X-Authenticated-User` is verified against `CustomerRepository` / `AppUserRepository`. Missing header returns HTTP 400.
  - Cross-customer access throws `AccessDeniedException` (HTTP 403 Forbidden).

---

## 5. Next.js Customer AI Interface

- **File:** [`frontend/src/app/customer/ai-assistant/page.tsx`](file:///Users/ajeet/AjeetFOLIO/BankCore360/frontend/src/app/customer/ai-assistant/page.tsx)
- **Features:**
  - 5 Quick Action Tabs: Financial Insights, Transaction Explainer, Fraud Clarification, Loan Assistant, Banking Knowledge.
  - Contextual suggested questions per tab.
  - Agent identification badges indicating which specialist responded.
  - Verified audit citations and clickable recommended next steps.
  - Conversation thread reset button.

