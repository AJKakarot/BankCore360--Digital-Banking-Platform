from app.evaluator import evaluate_answer


def test_evaluator_validates_grounded_answer(monkeypatch):
    monkeypatch.setattr(
        "app.evaluator.ask_grok",
        lambda prompt: '{"correct": true, "grounded": true, "unsupported_claims": [], "reason": "Accurately grounded in BankCore360 loan policy"}'
    )

    eval_result = evaluate_answer(
        question="What is the maximum allowable FOIR for loans?",
        policy_context="5. Credit Underwriting Policy: Maximum allowable FOIR threshold is strictly 50.0%.",
        answer="The maximum allowable FOIR threshold is strictly 50.0% per BankCore360 credit underwriting guidelines."
    )

    assert eval_result["correct"] is True
    assert eval_result["grounded"] is True
    assert eval_result["unsupported_claims"] == []
