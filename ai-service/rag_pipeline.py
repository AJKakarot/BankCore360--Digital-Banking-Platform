"""
RAG Pipeline for BankCore360 AI Banking Assistant.
Handles document validation, text extraction (PDF/TXT), chunking,
embedding generation, vector storage, and grounded similarity search.
"""

import io
import os
import re
import logging
from typing import List, Dict, Any, Optional, Tuple
from pathlib import Path
from pypdf import PdfReader

from config import settings
from embeddings import embedding_engine
from database import memory_vector_store

logger = logging.getLogger("BankCore360.AI.RAG")

ALLOWED_EXTENSIONS = {".pdf", ".txt", ".md"}
MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024  # 10 MB

class TextChunk:
    def __init__(self, content: str, page_number: int, chunk_index: int):
        self.content = content
        self.page_number = page_number
        self.chunk_index = chunk_index

class RagPipeline:
    def __init__(self):
        self.chunk_size = settings.CHUNK_SIZE
        self.chunk_overlap = settings.CHUNK_OVERLAP
        self.similarity_top_k = settings.SIMILARITY_TOP_K

    def validate_file(self, filename: str, file_size: int) -> Tuple[bool, Optional[str]]:
        ext = Path(filename).suffix.lower()
        if ext not in ALLOWED_EXTENSIONS:
            return False, f"Unsupported file extension '{ext}'. Only {', '.join(ALLOWED_EXTENSIONS)} are allowed."
        if file_size > MAX_FILE_SIZE_BYTES:
            return False, f"File size ({file_size / (1024*1024):.1f} MB) exceeds maximum allowed size of 10 MB."
        if file_size <= 0:
            return False, "File is empty."
        return True, None

    def extract_text(self, file_bytes: bytes, filename: str) -> List[Tuple[int, str]]:
        """
        Extracts text from PDF or text files.
        Returns list of (page_number, page_text) tuples.
        """
        ext = Path(filename).suffix.lower()
        pages: List[Tuple[int, str]] = []

        if ext == ".pdf":
            try:
                reader = PdfReader(io.BytesIO(file_bytes))
                for page_idx, page in enumerate(reader.pages):
                    raw_text = page.extract_text() or ""
                    cleaned = self.clean_text(raw_text)
                    if cleaned:
                        pages.append((page_idx + 1, cleaned))
            except Exception as e:
                logger.error(f"PDF extraction error for {filename}: {e}")
                raise ValueError(f"Failed to extract text from PDF: {e}")
        else:
            try:
                text_content = file_bytes.decode("utf-8")
            except UnicodeDecodeError:
                text_content = file_bytes.decode("latin-1", errors="replace")
            cleaned = self.clean_text(text_content)
            if cleaned:
                pages.append((1, cleaned))

        return pages

    def clean_text(self, text: str) -> str:
        if not text:
            return ""
        # Remove null bytes and non-printable control characters (except newline, tab)
        text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f-\x9f]", "", text)
        # Normalize multiple spaces and linebreaks
        text = re.sub(r"[ \t]+", " ", text)
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip()

    def split_into_chunks(self, pages: List[Tuple[int, str]]) -> List[TextChunk]:
        """
        Splits pages into overlapping semantic chunks, preserving page references.
        """
        chunks: List[TextChunk] = []
        chunk_idx = 0

        for page_num, text in pages:
            # Split by double newline (paragraphs) first, then sentences if necessary
            paragraphs = text.split("\n\n")
            current_chunk = ""

            for p in paragraphs:
                p = p.strip()
                if not p:
                    continue

                if len(current_chunk) + len(p) + 2 <= self.chunk_size:
                    current_chunk = f"{current_chunk}\n\n{p}" if current_chunk else p
                else:
                    if current_chunk:
                        chunks.append(TextChunk(current_chunk.strip(), page_num, chunk_idx))
                        chunk_idx += 1
                        # Retain overlap from end of current_chunk
                        overlap_start = max(0, len(current_chunk) - self.chunk_overlap)
                        current_chunk = current_chunk[overlap_start:].strip() + "\n\n" + p
                    else:
                        # Paragraph itself is larger than chunk_size, split by sentences
                        sentences = re.split(r"(?<=[.!?]) +", p)
                        for s in sentences:
                            if len(current_chunk) + len(s) + 1 <= self.chunk_size:
                                current_chunk = f"{current_chunk} {s}" if current_chunk else s
                            else:
                                if current_chunk:
                                    chunks.append(TextChunk(current_chunk.strip(), page_num, chunk_idx))
                                    chunk_idx += 1
                                current_chunk = s

            if current_chunk.strip():
                chunks.append(TextChunk(current_chunk.strip(), page_num, chunk_idx))
                chunk_idx += 1

        return chunks

    def ingest_document(self, file_bytes: bytes, filename: str, title: str,
                        category: str, loan_type: Optional[str] = None,
                        version: str = "1.0", uploaded_by: str = "ADMIN") -> Dict[str, Any]:
        """
        End-to-end ingestion pipeline:
        Validate -> Extract -> Clean -> Chunk -> Embed -> Store in Vector Store.
        """
        is_valid, err_msg = self.validate_file(filename, len(file_bytes))
        if not is_valid:
            raise ValueError(err_msg)

        pages = self.extract_text(file_bytes, filename)
        if not pages:
            raise ValueError(f"No readable text could be extracted from {filename}.")

        chunks = self.split_into_chunks(pages)
        if not chunks:
            raise ValueError(f"Document contains no content to index.")

        # Generate embeddings
        texts = [c.content for c in chunks]
        embeddings = embedding_engine.embed_documents(texts)

        chunks_data = []
        for i, (chunk, emb) in enumerate(zip(chunks, embeddings)):
            chunks_data.append({
                "chunk_index": i,
                "page_number": chunk.page_number,
                "content": chunk.content,
                "embedding": emb
            })

        # Store in vector store
        doc_record = memory_vector_store.add_document(
            title=title,
            filename=filename,
            category=category,
            loan_type=loan_type,
            version=version,
            uploaded_by=uploaded_by,
            chunks_data=chunks_data
        )

        logger.info(f"Ingested document '{title}' (ID {doc_record['id']}) with {len(chunks_data)} chunks.")
        return doc_record

    def retrieve(self, query: str, top_k: int = 3,
                 category: Optional[str] = None, loan_type: Optional[str] = None) -> List[Dict[str, Any]]:
        """
        Retrieves relevant policy chunks for a customer question.
        Uses embedding cosine similarity search against active policy chunks.
        """
        query_vector = embedding_engine.embed_query(query)
        results = memory_vector_store.similarity_search(
            query_vector=query_vector,
            top_k=top_k,
            category=category,
            loan_type=loan_type
        )
        return results

    def seed_sample_policies(self):
        """
        Seeds sample policies from sample_policies/ on startup if vector store is empty.
        """
        sample_dir = Path(__file__).resolve().parent / "sample_policies"
        if not sample_dir.exists():
            return

        policy_meta = [
            ("personal_loan_policy.txt", "BankCore360 Personal Loan Policy", "Personal Loan Policy", "PERSONAL"),
            ("home_loan_policy.txt", "BankCore360 Home Loan Policy", "Home Loan Policy", "HOME"),
            ("education_loan_policy.txt", "BankCore360 Education Loan Policy", "Education Loan Policy", "EDUCATION"),
            ("business_loan_policy.txt", "BankCore360 Business Loan Policy", "Business Loan Policy", "BUSINESS"),
            ("kyc_and_document_policy.txt", "BankCore360 KYC & Document Checklist", "KYC and Document Policy", None),
            ("interest_rate_policy.txt", "BankCore360 Pricing & Interest Rates", "Interest Rate Policy", None),
            ("repayment_and_emi_policy.txt", "BankCore360 Repayment & Amortization Policy", "Repayment and EMI Policy", None),
            ("loan_eligibility_policy.txt", "BankCore360 Credit Underwriting & FOIR Rules", "Loan Eligibility Policy", None),
        ]

        for fname, title, category, ltype in policy_meta:
            fpath = sample_dir / fname
            if fpath.exists():
                try:
                    with open(fpath, "rb") as f:
                        file_bytes = f.read()
                    self.ingest_document(
                        file_bytes=file_bytes,
                        filename=fname,
                        title=title,
                        category=category,
                        loan_type=ltype,
                        version="1.0",
                        uploaded_by="SYSTEM_SEED"
                    )
                except Exception as e:
                    logger.warning(f"Could not seed {fname}: {e}")

# Global instance
rag_pipeline = RagPipeline()

