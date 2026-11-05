from fastapi.testclient import TestClient
from app.main import app
import app.main as main_module

client = TestClient(app)


def test_home_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    assert "BankCore360" in response.json()["message"]


def test_ask_returns_answer_and_sources(monkeypatch):
    def fake_run_policy_graph(question: str) -> dict:
        return {
            "question": question,
            "answer": (
                "Education loans require Confirmed Admission Offer Letter, fee structure, "
                "academic mark sheets, and co-borrower income proof. [POL-ED-2026-V1, Section 3. EDUCATION LOAN PRODUCT POLICY]"
            ),
            "sources": [
                {
                    "policy_id": "POL-ED-2026-V1",
                    "section": "3. EDUCATION LOAN PRODUCT POLICY",
                    "text": (
                        "Mandatory Documents: Confirmed Admission Offer Letter and fee structure, "
                        "10th/12th/degree mark sheets, co-borrower test PAN and Aadhaar, and 3 months salary slips."
                    ),
                }
            ],
        }

    monkeypatch.setattr(
        main_module,
        "run_policy_graph",
        fake_run_policy_graph,
    )

    response = client.post(
        "/ask",
        json={
            "question": "What documents are required for an education loan?"
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert "education" in data["answer"].lower() or "admission" in data["answer"].lower()
    assert len(data["sources"]) > 0
    assert data["sources"][0]["policy_id"] == "POL-ED-2026-V1"
    assert "EDUCATION LOAN" in data["sources"][0]["section"]


def test_unsupported_question_returns_empty_sources(monkeypatch):
    def fake_run_policy_graph(question: str) -> dict:
        return {
            "question": question,
            "answer": (
                "I could not find sufficiently relevant "
                "information in the BankCore360 loan policies."
            ),
            "sources": [],
        }

    monkeypatch.setattr(
        main_module,
        "run_policy_graph",
        fake_run_policy_graph,
    )

    response = client.post(
        "/ask",
        json={
            "question": "What is the policy for buying cryptocurrency with loan funds?"
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["sources"] == []
    assert "could not find" in data["answer"].lower()


def test_ask_empty_question_returns_422():
    response = client.post("/ask", json={"question": "   "})
    assert response.status_code == 422


def test_chat_endpoint_compatibility():
    response = client.post("/api/ai/chat", json={"message": "What is the minimum income criteria for personal loan?"})
    assert response.status_code == 200
    data = response.json()
    assert "response" in data
    assert "sources" in data
    assert "disclaimer" in data
