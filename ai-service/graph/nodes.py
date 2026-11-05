"""
LangGraph Nodes for BankCore360 Conversational Assistant.
Each node is a pure function transforming BankingChatState.
"""

import re
from typing import Dict, Any, List
from graph.state import BankingChatState
from rag_pipeline import rag_pipeline
from spring_client import spring_client
from config import settings

EDUCATIONAL_DISCLAIMER = (
    "Educational AI Banking Assistant guidance. "
    "Final credit, document verification, and disbursement decisions rest solely "
    "with authorized human BankCore360 Checker and Manager officials. "
    "Indicative guidance does not constitute a guaranteed loan sanction."
)

# 1. Query Classifier Node
def query_classifier(state: BankingChatState) -> Dict[str, Any]:
    msg = (state.get("message") or "").lower()
    cat = state.get("category")

    if cat:
        intent = "POLICY_QUERY"
    elif any(w in msg for w in ["status", "where is my", "pending", "stage", "my application", "has it reached", "manager approval"]):
        intent = "APPLICATION_STATUS_QUERY"
    elif any(w in msg for w in ["eligib", "qualify", "salary", "income", "foir", "dti", "minimum age"]):
        intent = "ELIGIBILITY_QUERY"
    elif any(w in msg for w in ["emi", "rate", "interest", "calculate", "amortiz", "reducing balance", "flat rate"]):
        intent = "EMI_QUERY"
    elif any(w in msg for w in ["document", "pan", "aadhaar", "upload", "proof", "reject", "why was my doc"]):
        intent = "DOCUMENT_QUERY"
    elif any(w in msg for w in ["personal", "home", "education", "business", "tenure", "limit", "policy"]):
        intent = "POLICY_QUERY"
    else:
        intent = "GENERAL_BANKING"

    return {"intent": intent}

# 2. Query Rewriter Node
def query_rewriter(state: BankingChatState) -> Dict[str, Any]:
    msg = state.get("message", "")
    
    # Expand banking domain acronyms and conversational shorthand
    replacements = {
        r"\bfoir\b": "fixed obligation to income ratio debt to income",
        r"\broi\b": "rate of interest annual percentage rate",
        r"\bdocs?\b": "required supporting documents checklist",
        r"\bpan\b": "PAN card identity proof",
        r"\baadhaar\b": "Aadhaar card address proof",
        r"\bemi\b": "monthly reducing balance installment payment",
        r"\bcibil\b": "credit score benchmark",
        r"\btenure\b": "loan duration in months"
    }

    rewritten = msg.lower()
    for pattern, expansion in replacements.items():
        rewritten = re.sub(pattern, expansion, rewritten, flags=re.IGNORECASE)

    # Append category context if explicitly provided
    category = state.get("category")
    if category and category.lower() not in rewritten:
        rewritten = f"{category} {rewritten}"

    return {"rewritten_query": rewritten.strip()}

# 3. Retriever Node
def retriever(state: BankingChatState) -> Dict[str, Any]:
    query = state.get("rewritten_query") or state.get("message", "")
    category = state.get("category")
    loan_type = state.get("loan_type")

    results = rag_pipeline.retrieve(
        query=query,
        top_k=settings.SIMILARITY_TOP_K,
        category=category,
        loan_type=loan_type
    )

    return {"retrieved_chunks": results}

# 4. Context Validator Node
def context_validator(state: BankingChatState) -> Dict[str, Any]:
    chunks = state.get("retrieved_chunks", [])
    if not chunks:
        return {"is_context_sufficient": False}

    top_chunk = chunks[0]
    top_score = top_chunk.get("similarity_score", 0.0)

    # Check relevance threshold
    is_sufficient = top_score >= settings.RELEVANCE_THRESHOLD
    return {"is_context_sufficient": is_sufficient}

# 5. AI Response Generator Node
def ai_response_generator(state: BankingChatState) -> Dict[str, Any]:
    intent = state.get("intent", "GENERAL_BANKING")
    is_sufficient = state.get("is_context_sufficient", False)
    chunks = state.get("retrieved_chunks", [])
    message = state.get("message", "")

    if not is_sufficient or not chunks:
        raw_answer = (
            "I apologize, but the requested policy information is not explicitly covered in BankCore360's "
            "currently active, approved policy documents. "
            "To prevent providing inaccurate information, I recommend contacting our Branch Credit Desk or "
            "consulting the official Product Matrices in the OpenAPI documentation."
        )
        sources: List[Dict[str, Any]] = []
        policies: List[str] = []
        next_steps = [
            "View official product guides in the Customer Portal",
            "Contact BankCore360 Customer Support at support@bankcore360.com"
        ]
        return {
            "raw_answer": raw_answer,
            "sources": sources,
            "relevant_policies": policies,
            "suggested_next_steps": next_steps
        }

    # Extract grounding text and sources
    grounded_texts = []
    sources = []
    policies = []

    for c in chunks:
        grounded_texts.append(f"[{c['document_title']} (Page {c['page_number']})]: {c['content']}")
        sources.append({
            "documentId": c["document_id"],
            "documentName": c["document_title"],
            "policyCategory": c["category"],
            "version": c["version"],
            "pageNumber": c["page_number"],
            "similarityScore": c["similarity_score"],
            "excerpt": c["content"][:200] + "..."
        })
        policies.append(f"{c['document_title']}: {c['content'][:150]}...")

    # Synthesize grounded answer
    combined_grounding = "\n\n".join(grounded_texts)

    # 1. Grok API by xAI integration (when XAI_API_KEY is configured)
    if settings.XAI_API_KEY:
        try:
            from langchain_openai import ChatOpenAI
            from langchain_core.messages import SystemMessage, HumanMessage

            llm = ChatOpenAI(
                base_url=settings.XAI_BASE_URL,
                api_key=settings.XAI_API_KEY,
                model=settings.XAI_MODEL,
                temperature=0.1
            )
            prompt = (
                "You are the official BankCore360 AI Banking Assistant. Answer the customer's question strictly using "
                "the approved company loan policy context provided below. Do not invent policies, interest rates, or approval promises. "
                "Always cite the source document name when providing factual details.\n\n"
                f"APPROVED POLICY CONTEXT:\n{combined_grounding}\n\n"
                f"CUSTOMER QUESTION: {message}"
            )
            llm_response = llm.invoke([
                SystemMessage(content="You are BankCore360 AI Assistant. Ground all answers solely in the provided policy context."),
                HumanMessage(content=prompt)
            ])
            raw_answer = str(llm_response.content)
            next_steps = [
                "Review the cited policy section for specific terms and conditions",
                "Consult with your Branch Checker or Manager for personalized underwriting assistance"
            ]
            return {
                "raw_answer": raw_answer,
                "sources": sources,
                "relevant_policies": policies,
                "suggested_next_steps": next_steps
            }
        except Exception as e:
            # Fallback to local grounded generator on API timeout or error
            pass
    
    if intent == "ELIGIBILITY_QUERY":
        raw_answer = (
            f"Based on BankCore360's Credit Underwriting and Eligibility Policy:\n\n"
            f"{chunks[0]['content']}\n\n"
            "Key Criteria Summary:\n"
            "• Minimum verifiable net monthly income: ₹15,000.\n"
            "• Maximum allowable Debt-to-Income / FOIR: 50%.\n"
            "• Age limit: 21 to 60 years at loan maturity.\n"
            "• You can verify your indicative debt capacity using our interactive Loan Eligibility Calculator."
        )
        next_steps = [
            "Check your borrowing capacity with the interactive Loan Eligibility Calculator",
            "Ensure total proposed monthly EMIs remain below 50% of your net monthly income"
        ]
    elif intent == "DOCUMENT_QUERY":
        raw_answer = (
            f"Per BankCore360 KYC & Documentation Guidelines:\n\n"
            f"{chunks[0]['content']}\n\n"
            "Mandatory Checklist:\n"
            "1. Primary Identity: Test PAN Card (mandatory for tax identification).\n"
            "2. Address Proof: Test Aadhaar Card or recent Utility Bill (<90 days).\n"
            "3. Income Proof: Last 3 calendar months salary pay slips.\n"
            "4. Operative Bank Statement: 6 months continuous statement reflecting salary credits.\n"
            "Note: Use simulated test files only; never upload real government credentials."
        )
        next_steps = [
            "Prepare PDF or JPEG test copies under 5 MB each",
            "Upload files via 'My Applications' -> 'Upload Documents'"
        ]
    elif intent == "EMI_QUERY":
        raw_answer = (
            f"Per BankCore360 Repayment & Pricing Guidelines:\n\n"
            f"{chunks[0]['content']}\n\n"
            "Amortization Principles:\n"
            "• BankCore360 strictly uses the Monthly Reducing Balance formula: E = P * r * (1+r)^n / ((1+r)^n - 1).\n"
            "• Interest is assessed each month only on the remaining unpaid principal balance.\n"
            "• In earlier months, interest represents a larger share of the EMI; over time, principal repayment accelerates."
        )
        next_steps = [
            "Compare reducing balance vs flat rate at GET /api/ai/explain-emi",
            "View auto-generated monthly amortization schedule once loan is disbursed"
        ]
    else:
        raw_answer = (
            f"According to BankCore360 Approved Banking Policies:\n\n"
            f"{chunks[0]['content']}\n\n"
            "This information is grounded in the official policy document referenced below."
        )
        next_steps = [
            "Explore loan options in the Customer Portal",
            "Review supporting document requirements before submitting an application"
        ]

    return {
        "raw_answer": raw_answer,
        "sources": sources,
        "relevant_policies": policies,
        "suggested_next_steps": next_steps
    }

# 6. Response Validator & Guardrail Node
def response_validator(state: BankingChatState) -> Dict[str, Any]:
    raw = state.get("raw_answer", "")
    warnings: List[str] = []

    # Guardrail 1: Prohibit guaranteed or promised approval
    sanitized = raw
    forbidden_approval_terms = [
        "guaranteed approval", "we guarantee your loan", "your loan is approved",
        "you are 100% approved", "we promise sanction"
    ]
    for term in forbidden_approval_terms:
        if term in sanitized.lower():
            sanitized = re.sub(re.escape(term), "subject to formal Maker-Checker review", sanitized, flags=re.IGNORECASE)
            warnings.append("Stripped unauthorized loan approval guarantee to comply with credit policy.")

    # Guardrail 2: Detect prompt injection attempts in original message
    msg = (state.get("message") or "").lower()
    injection_patterns = ["ignore previous instructions", "system prompt", "forget all rules", "jailbreak", "override policy"]
    if any(p in msg for p in injection_patterns):
        warnings.append("Security note: System prompt and core safety policies cannot be overridden.")

    return {
        "validated_answer": sanitized,
        "guardrail_warnings": warnings
    }

# 7. Final Response Node
def final_response(state: BankingChatState) -> Dict[str, Any]:
    return {
        "disclaimer": EDUCATIONAL_DISCLAIMER
    }

# Application Status Node (for live status inquiries)
def application_status_node(state: BankingChatState) -> Dict[str, Any]:
    user = state.get("customer_username")
    ref = state.get("application_reference")

    # In synchronous mode or fallback, provide structured status assistance
    if not user:
        ans = (
            "To view your specific loan application status, please make sure you are signed in to BankCore360. "
            "You can track your application in real time in the 'Customer Portal' under 'My Applications'."
        )
        steps = ["Log in with your customer account", "Navigate to 'My Applications'"]
    else:
        ans = (
            f"Application Status Information for {user}:\n"
            "• BankCore360 follows the Maker-Checker-Manager workflow:\n"
            "  1. SUBMITTED: Application received; awaiting customer document uploads.\n"
            "  2. UNDER_REVIEW: Bank Checker is reviewing your uploaded documents.\n"
            "  3. PENDING_MANAGER_APPROVAL: All documents are verified; awaiting Manager credit appraisal.\n"
            "  4. APPROVED / DISBURSED: Manager has sanctioned the facility and generated the EMI schedule.\n"
            "You can view live status and any document rejection reasons at GET /api/loans/my-applications."
        )
        steps = [
            "Check notifications bell for real-time status updates",
            "View uploaded documents in 'My Applications' modal"
        ]

    return {
        "raw_answer": ans,
        "validated_answer": ans,
        "sources": [],
        "relevant_policies": ["BankCore360 Maker-Checker-Manager Dual Control Governance"],
        "suggested_next_steps": steps,
        "guardrail_warnings": []
    }

