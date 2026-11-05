import os
import re
from pathlib import Path
from typing import List, Dict, Any, Optional
from langsmith import traceable

# Base directories
BASE_DIR = Path(__file__).resolve().parent.parent
POLICY_PATH = BASE_DIR / "data" / "sample_policies" / "bankcore360_loan_policies.txt"
if not POLICY_PATH.exists():
    POLICY_PATH = BASE_DIR / "sample_policies" / "bankcore360_loan_policies.txt"
if not POLICY_PATH.exists():
    POLICY_PATH = BASE_DIR / "sample_policies" / "personal_loan_policy.txt"

# In-memory cache for policy section embeddings
_cached_chunks = None
_cached_embeddings = None


def load_policy(policy_path: Optional[Path] = None) -> str:
    """Load BankCore360 loan policy master from disk."""
    target_path = policy_path or POLICY_PATH
    if not target_path.exists():
        # Search in sample_policies directory
        sample_dir = BASE_DIR / "sample_policies"
        if sample_dir.exists():
            txt_files = list(sample_dir.glob("*.txt"))
            if txt_files:
                target_path = txt_files[0]

    if not target_path.exists():
        raise FileNotFoundError(f"Policy file not found: {target_path}")

    return target_path.read_text(encoding="utf-8")


def split_into_chunks(text: str) -> List[str]:
    """Split policy text while keeping headings with sections."""
    parts = re.split(r"(?m)(?=^\d+\.\s+)", text)
    return [part.strip() for part in parts if part.strip()]


def format_source(chunk: str) -> Dict[str, str]:
    """Extract policy ID, section heading, and source text."""
    policy_id_match = re.search(r"Policy ID:\s*([A-Za-z0-9-]+)", chunk)
    section_match = re.search(r"(?m)^(\d+\.\s+.+)$", chunk)

    policy_id = policy_id_match.group(1) if policy_id_match else "POL-LOAN-2026"
    section = section_match.group(1).strip() if section_match else "General Loan Policy"
    source_text = chunk

    if section_match:
        source_text = (
            chunk[:section_match.start()] + chunk[section_match.end():]
        ).strip()

    return {
        "policy_id": policy_id,
        "section": section,
        "text": source_text
    }


def _cosine_similarity(vec1: List[float], vec2: List[float]) -> float:
    dot = sum(a * b for a, b in zip(vec1, vec2))
    norm1 = sum(a * a for a in vec1) ** 0.5
    norm2 = sum(b * b for b in vec2) ** 0.5
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return dot / (norm1 * norm2)


def _encode_text(text: str) -> List[float]:
    """Encode text to normalized vector using embedding engine."""
    from embeddings import embedding_engine
    return embedding_engine.embed_query(text)


@traceable(name="retrieve-policy-chunks")
def retrieve_chunks(
    question: str,
    chunks: List[str],
    top_k: int = 2,
    min_similarity: float = 0.35
) -> List[str]:
    """Retrieve relevant loan policy sections using semantic search."""
    global _cached_chunks, _cached_embeddings

    if not question.strip() or not chunks or top_k <= 0:
        return []

    eligible_chunks = [
        chunk for chunk in chunks
        if re.search(r"(?m)^\d+\.\s+", chunk)
    ]

    if not eligible_chunks:
        return []

    current_chunks = tuple(eligible_chunks)
    if current_chunks != _cached_chunks:
        _cached_embeddings = [_encode_text(c) for c in eligible_chunks]
        _cached_chunks = current_chunks

    q_vec = _encode_text(question)

    # Keywords mapping for domain boosting across BankCore360 loan types
    q_lower = question.lower()
    scored = []
    for idx, (chunk, c_vec) in enumerate(zip(eligible_chunks, _cached_embeddings)):
        score = _cosine_similarity(q_vec, c_vec)
        c_lower = chunk.lower()

        # Keyword matching boosts
        keywords = ["education", "personal", "home", "business", "eligibility", "foir", "salary", "checker", "manager", "verification", "reducing balance", "emi", "repay", "kyc", "document"]
        matched_q = [k for k in keywords if k in q_lower]
        matched_both = [k for k in matched_q if k in c_lower]

        if matched_both:
            score = max(score, 0.40 + 0.15 * len(matched_both))

        if score >= min_similarity:
            scored.append((score, idx, chunk))

    scored.sort(key=lambda x: x[0], reverse=True)
    return [chunk for score, idx, chunk in scored[:top_k]]
