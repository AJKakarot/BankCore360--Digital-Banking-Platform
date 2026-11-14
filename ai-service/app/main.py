import os
import io
from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Depends, Header, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any

try:
    from langsmith import traceable
except ImportError:
    def traceable(*args, **kwargs):
        def decorator(f):
            return f
        return decorator

from .graph import run_policy_graph
from .multi_agent_graph import run_multi_agent_assistant
from database import memory_vector_store
from rag_pipeline import rag_pipeline
from eligibility import evaluate_eligibility
from document_intelligence import extract_document_information
from schemas import (
    EligibilityExplanationRequest,
    EligibilityExplanationResponse,
    DocumentExtractionRequest,
    DocumentExtractionResponse,
    PolicyDocumentResponse,
    PolicyDocumentDetailResponse,
    PolicyChunkResponse
)

app = FastAPI(
    title="BankCore360 Loan Policy Assistant",
    description="Official AI Assistant for BankCore360 Digital Loan Platform (Personal, Home, Education, Business loans)"
)

cors_origins = [o.strip() for o in os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8080").split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_origin_regex=r"https://.*\.vercel\.app|https://.*\.onrender\.com",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class QuestionRequest(BaseModel):
    question: str


class ChatRequestDto(BaseModel):
    message: Optional[str] = None
    question: Optional[str] = None
    category: Optional[str] = None
    loanType: Optional[str] = None
    customerUsername: Optional[str] = None
    applicationReference: Optional[str] = None
    threadId: Optional[str] = None


@app.get("/")
def home():
    return {
        "message": "BankCore360 Multi-Agent AI Banking Assistant is running!"
    }


@app.get("/api/ai/health")
@app.get("/ai/health")
def health_check():
    return {
        "status": "UP",
        "service": "BankCore360 Multi-Agent AI Banking Assistant",
        "framework": "LangGraph Supervisor",
        "specialistAgents": [
            "BankingKnowledgeAgent",
            "FinancialInsightsAgent",
            "TransactionExplanationAgent",
            "FraudExplanationAgent",
            "LoanAssistantAgent"
        ],
        "activePolicies": len([d for d in memory_vector_store.documents.values() if d.get("is_active")])
    }


@app.post("/ask")
@traceable(name="banking-policy-rag")
def ask_question(request: QuestionRequest):
    try:
        question = request.question.strip()

        if not question:
            raise HTTPException(
                status_code=422,
                detail="Question cannot be empty",
            )

        return run_policy_graph(question)

    except HTTPException:
        raise

    except Exception:
        raise HTTPException(
            status_code=502,
            detail="Policy assistant request failed",
        )


@app.post("/api/ai/chat")
@app.post("/api/ai/assistant/chat")
@traceable(name="banking-multi-agent-chat")
async def chat_endpoint(request: ChatRequestDto, x_authenticated_user: Optional[str] = Header(None)):
    raw_query = request.message if request.message is not None else request.question
    if raw_query is None or not raw_query.strip():
        raise HTTPException(status_code=400, detail="Query message cannot be empty")

    q = raw_query.strip()
    customer = x_authenticated_user or request.customerUsername

    return await run_multi_agent_assistant(
        query=q,
        customer_username=customer,
        thread_id=request.threadId
    )


# ====================================================
# 2. ELIGIBILITY & DOCUMENT EXTRACTION
# ====================================================

@app.post("/api/ai/loan/eligibility-explanation", response_model=EligibilityExplanationResponse, tags=["Calculations"])
def calculate_and_explain_eligibility(req: EligibilityExplanationRequest):
    return evaluate_eligibility(req)


@app.post("/api/ai/documents/extract", response_model=DocumentExtractionResponse, tags=["Document Intelligence"])
def extract_document(req: DocumentExtractionRequest):
    return extract_document_information(req)


# ====================================================
# 3. ADMIN POLICY MANAGEMENT
# ====================================================

@app.post("/api/ai/admin/policies/upload", response_model=PolicyDocumentResponse, tags=["Admin Policies"])
async def upload_policy_document(
    file: UploadFile = File(...),
    title: str = Form(...),
    policyCategory: str = Form(...),
    loanType: Optional[str] = Form(None),
    version: str = Form("1.0"),
    adminUsername: str = Form("ADMIN")
):
    contents = await file.read()
    try:
        doc = rag_pipeline.ingest_document(
            file_bytes=contents,
            filename=file.filename or "policy.txt",
            title=title,
            category=policyCategory,
            loan_type=loanType,
            version=version,
            uploaded_by=adminUsername
        )
        return PolicyDocumentResponse(
            id=doc["id"],
            title=doc["title"],
            filename=doc["filename"],
            category=doc["category"],
            loanType=doc.get("loan_type"),
            version=doc["version"],
            isActive=doc["is_active"],
            ingestionStatus=doc.get("ingestion_status", "INDEXED"),
            chunkCount=doc["chunk_count"],
            uploadedBy=doc["uploaded_by"],
            uploadedAt=doc["uploaded_at"]
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/api/ai/admin/policies/{doc_id}", response_model=PolicyDocumentDetailResponse, tags=["Admin Policies"])
def get_policy_document(doc_id: int):
    doc = memory_vector_store.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail=f"Policy document ID {doc_id} not found.")

    doc_chunks = [
        PolicyChunkResponse(
            chunkId=c["id"],
            documentId=c["document_id"],
            chunkIndex=c["chunk_index"],
            pageNumber=c.get("page_number", 1),
            content=c["content"],
            category=c.get("category", doc.get("category", "Policy")),
            version=c.get("version", doc.get("version", "1.0")),
            isActive=c["is_active"]
        )
        for c in memory_vector_store.chunks
        if c["document_id"] == doc_id
    ]

    return PolicyDocumentDetailResponse(
        id=doc["id"],
        title=doc["title"],
        filename=doc["filename"],
        category=doc["category"],
        loanType=doc.get("loan_type"),
        version=doc["version"],
        isActive=doc["is_active"],
        ingestionStatus=doc.get("ingestion_status", "INDEXED"),
        chunkCount=len(doc_chunks),
        uploadedBy=doc["uploaded_by"],
        uploadedAt=doc["uploaded_at"],
        chunks=doc_chunks
    )


@app.patch("/api/ai/admin/policies/{doc_id}/deactivate", response_model=PolicyDocumentResponse, tags=["Admin Policies"])
def deactivate_policy(doc_id: int):
    doc = memory_vector_store.set_document_active(doc_id, False)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    return PolicyDocumentResponse(
        id=doc["id"],
        title=doc["title"],
        filename=doc["filename"],
        category=doc["category"],
        loanType=doc.get("loan_type"),
        version=doc["version"],
        isActive=doc["is_active"],
        ingestionStatus=doc.get("ingestion_status", "INDEXED"),
        chunkCount=doc["chunk_count"],
        uploadedBy=doc["uploaded_by"],
        uploadedAt=doc["uploaded_at"]
    )


@app.patch("/api/ai/admin/policies/{doc_id}/publish", response_model=PolicyDocumentResponse, tags=["Admin Policies"])
def publish_policy(doc_id: int):
    doc = memory_vector_store.set_document_active(doc_id, True)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    return PolicyDocumentResponse(
        id=doc["id"],
        title=doc["title"],
        filename=doc["filename"],
        category=doc["category"],
        loanType=doc.get("loan_type"),
        version=doc["version"],
        isActive=doc["is_active"],
        ingestionStatus=doc.get("ingestion_status", "INDEXED"),
        chunkCount=doc["chunk_count"],
        uploadedBy=doc["uploaded_by"],
        uploadedAt=doc["uploaded_at"]
    )


@app.delete("/api/ai/admin/policies/{doc_id}", tags=["Admin Policies"])
def delete_policy(doc_id: int):
    deleted = memory_vector_store.delete_document(doc_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Document not found")
    return {"message": f"Policy {doc_id} successfully deleted."}
