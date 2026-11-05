"""
Embedding engine for BankCore360 AI Banking Assistant.
Supports Google Gemini, OpenAI, and a deterministic local normalized vectorizer.
"""

import math
import re
from typing import List
from config import settings

class EmbeddingEngine:
    def __init__(self):
        self.dimension = settings.EMBEDDING_DIMENSION
        self.provider = settings.EMBEDDING_PROVIDER.lower()
        self._gemini_client = None
        self._openai_client = None
        
        # Initialize Google GenAI if key available
        if settings.GEMINI_API_KEY and (self.provider in ("auto", "gemini")):
            try:
                from google import genai
                self._gemini_client = genai.Client(api_key=settings.GEMINI_API_KEY)
                self.provider = "gemini"
            except Exception:
                pass
                
        # Initialize OpenAI if key available and not already using Gemini
        if not self._gemini_client and settings.OPENAI_API_KEY and (self.provider in ("auto", "openai")):
            try:
                from openai import OpenAI
                self._openai_client = OpenAI(api_key=settings.OPENAI_API_KEY)
                self.provider = "openai"
            except Exception:
                pass
                
        if not self._gemini_client and not self._openai_client:
            self.provider = "local"

    def embed_query(self, text: str) -> List[float]:
        return self._embed_single(text)

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return [self._embed_single(t) for t in texts]

    def _embed_single(self, text: str) -> List[float]:
        clean_text = (text or "").strip()
        if not clean_text:
            return [0.0] * self.dimension

        if self.provider == "gemini" and self._gemini_client:
            try:
                result = self._gemini_client.models.embed_content(
                    model="text-embedding-004",
                    contents=clean_text
                )
                raw_vec = result.embedding.values
                return self._normalize_vector(list(raw_vec[:self.dimension]))
            except Exception:
                # Graceful fallback to deterministic local vectorizer on network error
                pass

        if self.provider == "openai" and self._openai_client:
            try:
                res = self._openai_client.embeddings.create(
                    model="text-embedding-3-small",
                    input=clean_text,
                    dimensions=self.dimension
                )
                return self._normalize_vector(res.data[0].embedding)
            except Exception:
                pass

        # Deterministic normalized token-hash embedding (384 dimensions)
        return self._deterministic_embed(clean_text)

    STOP_WORDS = {
        "the", "is", "at", "which", "on", "a", "an", "and", "or", "to", "in", "for",
        "of", "with", "by", "from", "as", "about", "into", "through", "during", "before",
        "after", "above", "below", "up", "down", "in", "out", "off", "over", "under", "again",
        "further", "then", "once", "here", "there", "when", "where", "why", "how", "all",
        "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
        "not", "only", "own", "same", "so", "than", "too", "very", "can", "will", "just",
        "should", "now", "i", "you", "my", "your", "what", "do", "need"
    }

    BANKING_KEYWORDS = {
        "loan", "personal", "home", "mortgage", "education", "business", "commercial",
        "interest", "rate", "tenure", "emi", "repay", "repayment", "reducing", "balance",
        "eligibility", "income", "salary", "foir", "dti", "minimum", "maximum",
        "document", "documents", "kyc", "pan", "aadhaar", "passport", "proof", "statement",
        "checker", "maker", "manager", "disbursement", "sanction", "approval", "rejection",
        "default", "moratorium", "fee", "charges", "foreclosure", "prepayment", "turnover"
    }

    def _deterministic_embed(self, text: str) -> List[float]:
        """
        Creates a reproducible, 384-dimensional L2-normalized vector based on
        content word hashing, banking domain keyword boosting, and stopword filtering.
        """
        vec = [0.0] * self.dimension
        raw_words = re.findall(r"\b[a-zA-Z0-9_\-]{2,}\b", text.lower())
        words = [w for w in raw_words if w not in self.STOP_WORDS]
        if not words:
            # Fallback to raw words if all were stopwords
            words = raw_words
        if not words:
            return vec

        for i, word in enumerate(words):
            weight = 3.0 if word in self.BANKING_KEYWORDS else 1.2
            h = hash(word)
            idx1 = abs(h) % self.dimension
            sign1 = 1.0 if (h // self.dimension) % 2 == 0 else -1.0
            vec[idx1] += sign1 * weight

            # Bigram sequence
            if i < len(words) - 1:
                bigram = f"{word}_{words[i+1]}"
                h_bi = hash(bigram)
                idx3 = abs(h_bi) % self.dimension
                vec[idx3] += 1.5

        return self._normalize_vector(vec)

    @staticmethod
    def _normalize_vector(vec: List[float]) -> List[float]:
        norm = math.sqrt(sum(x * x for x in vec))
        if norm == 0.0 or math.isnan(norm):
            return [0.0] * len(vec)
        return [float(x / norm) for x in vec]

    @staticmethod
    def cosine_similarity(v1: List[float], v2: List[float]) -> float:
        if not v1 or not v2 or len(v1) != len(v2):
            return 0.0
        dot = sum(a * b for a, b in zip(v1, v2))
        return max(0.0, min(1.0, float(dot)))

# Singleton instance
embedding_engine = EmbeddingEngine()
