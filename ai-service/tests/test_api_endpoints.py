"""
Tests for FastAPI HTTP Endpoints (Admin Policy Management & Customer Assistant).
"""

import io
import pytest
import sys
from pathlib import Path
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from main import app
from database import memory_vector_store
from rag_pipeline import rag_pipeline

@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c

def test_health_endpoint(client):
    res = client.get("/api/ai/health")
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "UP"
    assert "BankCore360" in data["service"]

def test_chat_endpoint(client):
    payload = {
        "message": "What is the minimum monthly salary requirement?",
        "category": "Loan Eligibility Policy"
    }
    res = client.post("/api/ai/chat", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert "response" in data
    assert "intent" in data
    assert "disclaimer" in data

def test_chat_endpoint_empty_message(client):
    res = client.post("/api/ai/chat", json={"message": "   "})
    assert res.status_code == 400

def test_loan_eligibility_explanation_endpoint(client):
    payload = {
        "loanType": "PERSONAL",
        "monthlyIncome": 50000.0,
        "existingMonthlyEmi": 5000.0,
        "requestedAmount": 200000.0,
        "desiredTenureMonths": 24
    }
    res = client.post("/api/ai/loan/eligibility-explanation", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert "indicativeStatus" in data
    assert data["calculations"]["monthlyEmi"] > 0
    assert data["calculations"]["foirPercent"] > 0

def test_document_extraction_endpoint(client):
    payload = {
        "documentType": "PAN",
        "simulatedDocumentText": "INCOME TAX DEPARTMENT ABCDE9999Z NAME: RAHUL VERMA DOB: 01/01/1995",
        "applicantName": "Rahul Verma"
    }
    res = client.post("/api/ai/documents/extract", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["isSyntheticallyValid"] is True
    assert "panNumber" in data["extractedFields"]

def test_admin_policy_upload_and_lifecycle(client):
    policy_text = (
        "BANKCORE360 TEMPORARY POLICY\n\n"
        "This is an administrative test policy document.\n"
        "Minimum score requirement is 680."
    ).encode("utf-8")

    # 1. Upload
    files = {"file": ("test_policy.txt", io.BytesIO(policy_text), "text/plain")}
    data = {
        "title": "Admin Test Policy",
        "policyCategory": "Personal Loan Policy",
        "loanType": "PERSONAL",
        "version": "1.0",
        "adminUsername": "ADMIN_TEST"
    }
    res = client.post("/api/ai/admin/policies/upload", files=files, data=data)
    assert res.status_code == 200
    doc = res.json()
    doc_id = doc["id"]
    assert doc["title"] == "Admin Test Policy"
    assert doc["isActive"] is True

    # 2. Get Details
    res = client.get(f"/api/ai/admin/policies/{doc_id}")
    assert res.status_code == 200
    assert len(res.json()["chunks"]) >= 1

    # 3. Deactivate
    res = client.patch(f"/api/ai/admin/policies/{doc_id}/deactivate")
    assert res.status_code == 200
    assert res.json()["isActive"] is False

    # 4. Re-publish
    res = client.patch(f"/api/ai/admin/policies/{doc_id}/publish")
    assert res.status_code == 200
    assert res.json()["isActive"] is True

    # 5. Delete
    res = client.delete(f"/api/ai/admin/policies/{doc_id}")
    assert res.status_code == 200

    # 6. Verify 404 after deletion
    res = client.get(f"/api/ai/admin/policies/{doc_id}")
    assert res.status_code == 404

