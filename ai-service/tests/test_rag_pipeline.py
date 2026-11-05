"""
Tests for RAG Ingestion, Chunking, Embeddings, and Similarity Search.
"""

import pytest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from rag_pipeline import rag_pipeline
from database import memory_vector_store

def test_file_validation():
    # Valid files
    assert rag_pipeline.validate_file("policy.pdf", 1024)[0] is True
    assert rag_pipeline.validate_file("policy.txt", 1024)[0] is True
    assert rag_pipeline.validate_file("policy.md", 1024)[0] is True

    # Invalid extension
    is_valid, err = rag_pipeline.validate_file("malicious.exe", 1024)
    assert is_valid is False
    assert "Unsupported file extension" in err

    # Oversized file (> 10MB)
    is_valid, err = rag_pipeline.validate_file("huge.pdf", 11 * 1024 * 1024)
    assert is_valid is False
    assert "exceeds maximum allowed size" in err

def test_text_cleaning_and_chunking():
    raw_text = "Section 1: Lending.\n\n\n\nSection 2: Minimum income is 15000.\t\tMax FOIR is 50%.\0"
    cleaned = rag_pipeline.clean_text(raw_text)
    assert "\0" not in cleaned
    assert "\n\n\n" not in cleaned

    pages = [(1, "Paragraph 1 about BankCore360 loans.\n\nParagraph 2 about eligibility rules.")]
    chunks = rag_pipeline.split_into_chunks(pages)
    assert len(chunks) >= 1
    assert chunks[0].page_number == 1

def test_document_ingestion_and_retrieval():
    doc_content = (
        "BANKCORE360 TEST SPECIAL POLICY\n\n"
        "1. Eligible borrowers must maintain an active savings account.\n\n"
        "2. The maximum special promotional loan amount is ₹8,00,000.\n\n"
        "3. Special promotional interest rate is fixed at 9.99% per annum."
    ).encode("utf-8")

    doc = rag_pipeline.ingest_document(
        file_bytes=doc_content,
        filename="test_promo.txt",
        title="Test Promo Policy",
        category="Personal Loan Policy",
        loan_type="PERSONAL",
        version="1.0",
        uploaded_by="TEST_ADMIN"
    )

    assert doc["id"] > 0
    assert doc["chunk_count"] >= 1

    # Retrieve by query
    results = rag_pipeline.retrieve("What is the promotional interest rate?")
    assert len(results) > 0
    top = results[0]
    assert top["similarity_score"] > 0.0

def test_metadata_filtering():
    # Retrieve with category filter
    results = rag_pipeline.retrieve(
        query="interest rate",
        category="Personal Loan Policy"
    )
    for r in results:
        assert r["category"] == "Personal Loan Policy"

