"""
SQLAlchemy models for BankCore360 AI Policy Knowledge Base.
"""

from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey
from sqlalchemy.orm import declarative_base, relationship

Base = declarative_base()

class PolicyDocumentModel(Base):
    __tablename__ = "policy_documents"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(255), nullable=False)
    filename = Column(String(255), nullable=False)
    category = Column(String(100), nullable=False, index=True)
    loan_type = Column(String(50), nullable=True, index=True)
    version = Column(String(20), nullable=False, default="1.0")
    is_active = Column(Boolean, nullable=False, default=True, index=True)
    ingestion_status = Column(String(20), nullable=False, default="INDEXED")
    chunk_count = Column(Integer, nullable=False, default=0)
    uploaded_by = Column(String(100), nullable=False, default="ADMIN")
    uploaded_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=True, onupdate=datetime.utcnow)

    chunks = relationship("PolicyChunkModel", back_populates="document", cascade="all, delete-orphan")

class PolicyChunkModel(Base):
    __tablename__ = "policy_chunks"

    id = Column(Integer, primary_key=True, autoincrement=True)
    document_id = Column(Integer, ForeignKey("policy_documents.id", ondelete="CASCADE"), nullable=False, index=True)
    chunk_index = Column(Integer, nullable=False)
    page_number = Column(Integer, nullable=False, default=1)
    content = Column(Text, nullable=False)
    category = Column(String(100), nullable=False, index=True)
    loan_type = Column(String(50), nullable=True, index=True)
    version = Column(String(20), nullable=False, default="1.0")
    is_active = Column(Boolean, nullable=False, default=True, index=True)
    
    # Store embedding as pgvector or JSON/string
    embedding_json = Column(Text, nullable=True)

    document = relationship("PolicyDocumentModel", back_populates="chunks")

