from app.ingestion import (
    load_policy,
    split_into_chunks,
    retrieve_chunks,
    format_source,
)


def test_education_loan_retrieves_correct_section():
    policy_text = load_policy()
    chunks = split_into_chunks(policy_text)

    results = retrieve_chunks(
        "What documents are required for an education loan?",
        chunks,
        top_k=1,
        min_similarity=0.35,
    )

    assert len(results) > 0
    assert any("EDUCATION" in chunk or "POL-ED" in chunk for chunk in results)


def test_personal_loan_retrieval():
    policy_text = load_policy()
    chunks = split_into_chunks(policy_text)

    results = retrieve_chunks(
        "What is the maximum tenure and amount for a personal loan?",
        chunks,
        top_k=1,
        min_similarity=0.35,
    )

    assert len(results) > 0
    assert any("PERSONAL" in chunk or "POL-PL" in chunk for chunk in results)


def test_eligibility_foir_retrieval():
    policy_text = load_policy()
    chunks = split_into_chunks(policy_text)

    results = retrieve_chunks(
        "What is the maximum allowable FOIR debt ratio?",
        chunks,
        top_k=1,
        min_similarity=0.35,
    )

    assert len(results) > 0
    assert any("FOIR" in chunk or "50" in chunk for chunk in results)


def test_unsupported_loan_question_returns_no_chunks():
    policy_text = load_policy()
    chunks = split_into_chunks(policy_text)

    results = retrieve_chunks(
        "What is the interplanetary asteroid mining credit policy?",
        chunks,
        top_k=1,
        min_similarity=0.85,
    )

    assert results == []


def test_zero_top_k_returns_empty_list():
    policy_text = load_policy()
    chunks = split_into_chunks(policy_text)

    results = retrieve_chunks(
        "What is the minimum income?",
        chunks,
        top_k=0,
    )

    assert results == []


def test_format_source_extracts_fields():
    chunk = "1. PERSONAL LOAN PRODUCT POLICY\nPolicy ID: POL-PL-2026-V1\nMinimum loan amount ₹25,000."
    src = format_source(chunk)
    assert src["policy_id"] == "POL-PL-2026-V1"
    assert "PERSONAL LOAN" in src["section"]
