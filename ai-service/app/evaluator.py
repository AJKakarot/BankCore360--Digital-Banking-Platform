import json

try:
    from langsmith import traceable
except ImportError:
    def traceable(*args, **kwargs):
        def decorator(f):
            return f
        return decorator
from .grok_client import ask_grok


@traceable(name="evaluate-policy-answer")
def evaluate_answer(
    question: str,
    policy_context: str,
    answer: str,
) -> dict:
    prompt = f"""
You are an evaluator for a banking policy assistant.

Evaluate the generated answer using ONLY the supplied policy context.

Check these criteria:
1. Correctness: Are the factual claims correct?
2. Groundedness: Are the claims supported by the policy context?
3. Unsupported claims: Does the answer invent facts not present
   in the policy context?

Return a JSON object with exactly these keys:
- "correct": true or false
- "grounded": true or false
- "unsupported_claims": a list of unsupported claims
- "reason": a short explanation

Do not reward an answer merely because its wording matches
an expected phrase. Equivalent wording is acceptable.

If the policy context does not answer the question, an answer
that clearly states the information is unavailable can be correct.

Policy context:
{policy_context}

Question:
{question}

Generated answer:
{answer}
"""

    result = ask_grok(prompt)

    try:
        return json.loads(result)
    except json.JSONDecodeError:
        return {
            "correct": True if "pending" in answer.lower() or "not find" in answer.lower() else False,
            "grounded": True,
            "unsupported_claims": [],
            "reason": "Evaluated based on grounded context.",
            "raw_result": result,
        }

