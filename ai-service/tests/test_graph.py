from app import graph as graph_module


def test_relevant_question_generates_answer(monkeypatch):
    calls = {"retrieve": 0, "generate": 0}

    def fake_retrieve(question, chunks, top_k=2, min_similarity=0.35):
        calls["retrieve"] += 1
        return [
            "1. PERSONAL LOAN PRODUCT POLICY\n"
            "Policy ID: POL-PL-2026-V1\n"
            "Minimum net monthly take-home salary of ₹15,000 for Tier-2/3 cities."
        ]

    def fake_grok(prompt):
        if "Does this context contain information relevant" in prompt:
            return "YES"
        calls["generate"] += 1
        return "Minimum net monthly salary is ₹15,000. [POL-PL-2026-V1, Section 1. PERSONAL LOAN PRODUCT POLICY]"

    monkeypatch.setattr(graph_module, "load_policy", lambda: "sample policy")
    monkeypatch.setattr(graph_module, "split_into_chunks", lambda text: ["sample"])
    monkeypatch.setattr(graph_module, "retrieve_chunks", fake_retrieve)
    monkeypatch.setattr(
        graph_module,
        "format_source",
        lambda chunk: {
            "policy_id": "POL-PL-2026-V1",
            "section": "1. PERSONAL LOAN PRODUCT POLICY",
            "text": chunk,
        },
    )
    monkeypatch.setattr(graph_module, "ask_grok", fake_grok)

    result = graph_module.run_policy_graph(
        "What is the minimum monthly income for a personal loan?"
    )

    assert "₹15,000" in result["answer"] or "minimum" in result["answer"].lower()
    assert result["sources"][0]["policy_id"] == "POL-PL-2026-V1"
    assert calls["retrieve"] == 1
    assert calls["generate"] == 1


def test_irrelevant_question_retries_once_then_falls_back(monkeypatch):
    calls = {"retrieve": 0, "rewrite": 0}

    def fake_retrieve(question, chunks, top_k=2, min_similarity=0.35):
        calls["retrieve"] += 1
        return []

    def fake_grok(prompt):
        if "Does this context contain information relevant" in prompt:
            return "NO"
        if "Rewrite the following" in prompt:
            calls["rewrite"] += 1
            return "BankCore360 spaceship loan rules"
        raise AssertionError("Answer generation should not run")

    monkeypatch.setattr(graph_module, "load_policy", lambda: "sample policy")
    monkeypatch.setattr(graph_module, "split_into_chunks", lambda text: ["sample"])
    monkeypatch.setattr(graph_module, "retrieve_chunks", fake_retrieve)
    monkeypatch.setattr(graph_module, "ask_grok", fake_grok)

    result = graph_module.run_policy_graph(
        "What is the policy for purchasing an interstellar starship?"
    )

    assert "could not find" in result["answer"].lower()
    assert result["sources"] == []
    assert calls["retrieve"] == 2
    assert calls["rewrite"] == 1


def test_rewritten_query_can_retrieve_relevant_policy(monkeypatch):
    calls = {"retrieve": 0, "generate": 0}

    def fake_retrieve(question, chunks, top_k=2, min_similarity=0.35):
        calls["retrieve"] += 1
        if calls["retrieve"] == 1:
            return []
        return [
            "3. EDUCATION LOAN PRODUCT POLICY\n"
            "Policy ID: POL-ED-2026-V1\n"
            "Mandatory parent/guardian co-borrower with income >= ₹25,000."
        ]

    def fake_grok(prompt):
        if "Does this context contain information relevant" in prompt:
            return "YES" if "Retrieved policy context" in prompt and calls["retrieve"] > 1 else "NO"
        if "Rewrite the following" in prompt:
            return "education loan co-borrower salary criteria"
        calls["generate"] += 1
        return "Co-borrower must earn at least ₹25,000 per month. [POL-ED-2026-V1, Section 3]"

    monkeypatch.setattr(graph_module, "load_policy", lambda: "sample policy")
    monkeypatch.setattr(graph_module, "split_into_chunks", lambda text: ["sample"])
    monkeypatch.setattr(graph_module, "retrieve_chunks", fake_retrieve)
    monkeypatch.setattr(
        graph_module,
        "format_source",
        lambda chunk: {
            "policy_id": "POL-ED-2026-V1",
            "section": "3. EDUCATION LOAN PRODUCT POLICY",
            "text": chunk,
        },
    )
    monkeypatch.setattr(graph_module, "ask_grok", fake_grok)

    result = graph_module.run_policy_graph(
        "Do I need a co-applicant for higher studies financing?"
    )

    assert "₹25,000" in result["answer"] or "co-borrower" in result["answer"].lower()
    assert len(result["sources"]) > 0
    assert calls["retrieve"] == 2
    assert calls["generate"] == 1
