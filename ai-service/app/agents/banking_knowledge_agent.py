"""
Banking Knowledge Agent for BankCore360.
Answers general banking inquiries, account features, fees, limits, dispute procedures,
and loan policies using grounded RAG semantic retrieval over verified bank documentation.
Never invents banking rules. Returns explicit fallback when policies are not indexed.
"""

import logging
from typing import Dict, Any, List, Optional
from database import memory_vector_store
from embeddings import embedding_engine
from app.grok_client import ask_grok
from pathlib import Path

logger = logging.getLogger("BankCore360.AI.BankingKnowledgeAgent")

FALLBACK_MESSAGE = (
    "I could not find sufficiently relevant information in BankCore360 banking policies. "
    "Please refer to the official BankCore360 schedule of charges or contact 24x7 customer support at 1800-360-BANK."
)

class BankingKnowledgeAgent:
    def __init__(self):
        self._ensure_sample_policies_indexed()

    def _ensure_sample_policies_indexed(self):
        """Indexes default policy documents if vector store is empty."""
        if len(memory_vector_store.chunks) > 0:
            return

        policies_dir = Path(__file__).resolve().parent.parent.parent / "sample_policies"
        if not policies_dir.exists():
            return

        for path in policies_dir.glob("*.txt"):
            try:
                content = path.read_text(encoding="utf-8")
                # Split content by paragraphs or headers
                sections = [s.strip() for s in content.split("\n\n") if len(s.strip()) > 100]
                chunks_data = []
                for idx, sec in enumerate(sections):
                    vec = embedding_engine.embed_text(sec)
                    chunks_data.append({
                        "chunk_index": idx,
                        "page_number": 1,
                        "content": sec,
                        "embedding": vec
                    })
                memory_vector_store.add_document(
                    title=path.stem.replace("_", " ").title(),
                    filename=path.name,
                    category="BANKING_POLICY",
                    loan_type=None,
                    version="2026.1",
                    uploaded_by="SYSTEM",
                    chunks_data=chunks_data
                )
            except Exception as e:
                logger.warning(f"Failed to auto-index {path.name}: {e}")

    async def execute(self, query: str, customer_username: Optional[str] = None) -> Dict[str, Any]:
        """
        Executes grounded similarity search and synthesizes verified policy guidance.
        """
        self._ensure_sample_policies_indexed()
        query_vec = embedding_engine.embed_text(query)
        matches = memory_vector_store.similarity_search(query_vec, top_k=3)

        # Filter matches by relevance threshold
        valid_matches = [m for m in matches if m.get("similarity_score", 0) >= 0.22]

        if not valid_matches:
            return {
                "agent": "BankingKnowledgeAgent",
                "answer": FALLBACK_MESSAGE,
                "sources": [],
                "relevantPolicies": [],
                "suggestedNextSteps": [
                    "Review BankCore360 official fee schedule on the web portal",
                    "Contact branch customer care officer for manual assistance"
                ]
            }

        context_text = "\n\n".join([f"[{m['document_title']}] {m['content']}" for m in valid_matches])
        prompt = (
            "You are the BankCore360 Official Banking Policy Assistant. Answer the customer query strictly "
            "using ONLY the following verified bank policy excerpts. Do not fabricate charges, limits, or rules.\n\n"
            f"CUSTOMER QUERY: {query}\n\n"
            f"VERIFIED POLICIES:\n{context_text}\n\n"
            "Format your answer with clear markdown bullet points and highlight exact fee amounts or limits."
        )

        try:
            llm_answer = ask_grok(prompt)
            if not llm_answer or "could not" in llm_answer.lower() and len(llm_answer) < 50:
                answer = valid_matches[0]["content"]
            else:
                answer = llm_answer
        except Exception:
            answer = valid_matches[0]["content"]

        sources = [
            {
                "documentTitle": m.get("document_title"),
                "filename": m.get("document_filename"),
                "similarityScore": m.get("similarity_score"),
                "excerpt": m.get("content", "")[:220]
            }
            for m in valid_matches
        ]

        return {
            "agent": "BankingKnowledgeAgent",
            "answer": answer,
            "sources": sources,
            "relevantPolicies": [m.get("document_title") for m in valid_matches],
            "suggestedNextSteps": [
                "Verify detailed terms in the BankCore360 Customer Portal (/customer/policies)",
                "Contact branch support for fee waiver or dispute requests"
            ]
        }

banking_knowledge_agent = BankingKnowledgeAgent()
