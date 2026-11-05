import os
import io
from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Depends, Header, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from langsmith import traceable

from .graph import run_policy_graph
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

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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


@app.get("/")
def home():
    return {
        "message": "BankCore360 Loan Policy Assistant is running!"
    }


@app.get("/api/ai/health")
@app.get("/ai/health")
def health_check():
    return {
        "status": "UP",
        "service": "BankCore360 Loan Policy Assistant",
        "framework": "LangGraph",
        "grokModel": "xAI Grok / OpenAI-compatible",
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
@traceable(name="banking-policy-chat")
def chat_endpoint(request: ChatRequestDto):
    raw_query = request.message if request.message is not None else request.question
    if raw_query is None or not raw_query.strip():
        raise HTTPException(status_code=400, detail="Query message cannot be empty")

    q = raw_query.strip()
    result = run_policy_graph(q)
    answer = result.get("answer", "")
    sources = result.get("sources", [])

    return {
        "response": answer,
        "answer": answer,
        "intent": "POLICY_QUERY",
        "sources": [
            {
                "documentId": 1,
                "documentName": s.get("policy_id", "KYC-001"),
                "policyCategory": s.get("section", "Policy"),
                "version": "1.0",
                "pageNumber": 1,
                "similarityScore": 0.95,
                "excerpt": s.get("text", "")[:200]
            }
            for s in sources
        ],
        "relevantPolicies": [s.get("section", "Policy") for s in sources],
        "suggestedNextSteps": [
            "Review cited policy section in the Customer portal",
            "Contact BankCore360 branch support for underwriting details"
        ],
        "guardrailWarnings": [],
        "disclaimer": (
            "Educational AI Banking Assistant guidance. "
            "Final decisions rest solely with authorized human bank Checker and Manager officials."
        )
    }


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
