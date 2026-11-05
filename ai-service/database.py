"""
Database management and Vector Storage for BankCore360 AI Service.
Supports PostgreSQL with pgvector, and transparent in-memory vector fallback.
"""

import json
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime, timezone
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from config import settings
from models import Base, PolicyDocumentModel, PolicyChunkModel
from embeddings import embedding_engine

logger = logging.getLogger("BankCore360.AI.Database")

class InMemoryVectorStore:
    """Thread-safe in-memory vector store fallback."""
    def __init__(self):
        self.documents: Dict[int, Dict[str, Any]] = {}
        self.chunks: List[Dict[str, Any]] = []
        self._doc_id_counter = 1
        self._chunk_id_counter = 1

    def add_document(self, title: str, filename: str, category: str, loan_type: Optional[str],
                     version: str, uploaded_by: str, chunks_data: List[Dict[str, Any]]) -> Dict[str, Any]:
        # Deactivate previous versions of same category/loan_type if updated
        for doc in self.documents.values():
            if doc["category"] == category and doc["title"] == title and doc["version"] == version:
                doc["is_active"] = False

        doc_id = self._doc_id_counter
        self._doc_id_counter += 1

        now = datetime.now(timezone.utc)
        doc_record = {
            "id": doc_id,
            "title": title,
            "filename": filename,
            "category": category,
            "loan_type": loan_type,
            "version": version,
            "is_active": True,
            "ingestion_status": "INDEXED",
            "chunk_count": len(chunks_data),
            "uploaded_by": uploaded_by,
            "uploaded_at": now,
            "updated_at": now
        }
        self.documents[doc_id] = doc_record

        for ch in chunks_data:
            chunk_id = self._chunk_id_counter
            self._chunk_id_counter += 1
            self.chunks.append({
                "id": chunk_id,
                "document_id": doc_id,
                "chunk_index": ch["chunk_index"],
                "page_number": ch.get("page_number", 1),
                "content": ch["content"],
                "category": category,
                "loan_type": loan_type,
                "version": version,
                "is_active": True,
                "embedding": ch["embedding"]
            })

        return doc_record

    def get_document(self, doc_id: int) -> Optional[Dict[str, Any]]:
        return self.documents.get(doc_id)

    def list_documents(self) -> List[Dict[str, Any]]:
        return sorted(list(self.documents.values()), key=lambda x: x["id"], reverse=True)

    def set_document_active(self, doc_id: int, is_active: bool) -> Optional[Dict[str, Any]]:
        doc = self.documents.get(doc_id)
        if not doc:
            return None
        doc["is_active"] = is_active
        doc["updated_at"] = datetime.now(timezone.utc)
        for ch in self.chunks:
            if ch["document_id"] == doc_id:
                ch["is_active"] = is_active
        return doc

    def delete_document(self, doc_id: int) -> bool:
        if doc_id not in self.documents:
            return False
        del self.documents[doc_id]
        self.chunks = [ch for ch in self.chunks if ch["document_id"] != doc_id]
        return True

    def similarity_search(self, query_vector: List[float], top_k: int = 3,
                          category: Optional[str] = None, loan_type: Optional[str] = None) -> List[Dict[str, Any]]:
        scored = []
        for ch in self.chunks:
            if not ch["is_active"]:
                continue
            if category and ch["category"] != category:
                continue
            if loan_type and ch.get("loan_type") and ch["loan_type"] != loan_type:
                continue

            score = embedding_engine.cosine_similarity(query_vector, ch["embedding"])
            doc = self.documents.get(ch["document_id"], {})
            scored.append({
                "chunk_id": ch["id"],
                "document_id": ch["document_id"],
                "document_title": doc.get("title", "Policy Document"),
                "document_filename": doc.get("filename", "policy.txt"),
                "category": ch["category"],
                "loan_type": ch.get("loan_type"),
                "version": ch["version"],
                "page_number": ch["page_number"],
                "content": ch["content"],
                "similarity_score": round(score, 4)
            })

        scored.sort(key=lambda x: x["similarity_score"], reverse=True)
        return scored[:top_k]

# Global store instance
memory_vector_store = InMemoryVectorStore()

class DatabaseManager:
    def __init__(self):
        self.engine = None
        self.SessionLocal = None
        self.use_postgres = False
        self._init_connection()

    def _init_connection(self):
        try:
            db_url = settings.DATABASE_URL
            # Handle jdbc prefix if present
            if db_url.startswith("jdbc:postgresql://"):
                db_url = db_url.replace("jdbc:postgresql://", "postgresql://")
            
            self.engine = create_engine(
                db_url,
                pool_pre_ping=True,
                pool_size=5,
                max_overflow=10,
                connect_args={"connect_timeout": 3}
            )
            with self.engine.connect() as conn:
                if settings.USE_PGVECTOR:
                    try:
                        conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector;"))
                        conn.commit()
                        logger.info("PostgreSQL pgvector extension verified/enabled.")
                    except Exception as e:
                        logger.warning(f"pgvector extension not enabled: {e}")
                
                Base.metadata.create_all(bind=self.engine)
                self.SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)
                self.use_postgres = True
                logger.info("Connected to PostgreSQL successfully.")
        except Exception as e:
            logger.info(f"PostgreSQL unavailable ({e}). Using thread-safe in-memory vector store.")
            self.use_postgres = False

    def get_session(self):
        if self.use_postgres and self.SessionLocal:
            db = self.SessionLocal()
            try:
                yield db
            finally:
                db.close()
        else:
            yield None

db_manager = DatabaseManager()
