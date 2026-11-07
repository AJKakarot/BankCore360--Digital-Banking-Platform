"""
Configuration settings for BankCore360 AI Service.
Reads environment variables with sensible defaults.
"""

import os
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="allow")
    # Service Information
    APP_NAME: str = "BankCore360 AI Banking Assistant"
    APP_VERSION: str = "2.0.0"
    DEBUG: bool = False
    
    # Port & Host
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    
    # Spring Boot Integration
    SPRING_BOOT_URL: str = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")
    SPRING_BOOT_SERVICE_TOKEN: str = os.getenv("SPRING_BOOT_SERVICE_TOKEN", "bankcore360-internal-ai-token")
    
    # Database / Vector Storage
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        os.getenv("DB_URL", "postgresql://postgres:postgres@localhost:5432/bankcore360")
    )
    USE_PGVECTOR: bool = os.getenv("USE_PGVECTOR", "true").lower() in ("true", "1", "yes")
    
    # AI & Embeddings Providers
    # "gemini", "openai", or "local" (deterministic vectorizer)
    EMBEDDING_PROVIDER: str = os.getenv("EMBEDDING_PROVIDER", "auto")
    LLM_PROVIDER: str = os.getenv("LLM_PROVIDER", "auto")
    
    # Grok API by xAI
    XAI_API_KEY: str = os.getenv("XAI_API_KEY", "")
    XAI_MODEL: str = os.getenv("XAI_MODEL", "grok-2-latest")
    XAI_BASE_URL: str = os.getenv("XAI_BASE_URL", "https://api.x.ai/v1")

    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    

    # RAG Settings
    CHUNK_SIZE: int = 600
    CHUNK_OVERLAP: int = 80
    SIMILARITY_TOP_K: int = 3
    RELEVANCE_THRESHOLD: float = 0.18
    EMBEDDING_DIMENSION: int = 384
    
    # Admin API Key
    ADMIN_API_KEY: str = os.getenv("ADMIN_API_KEY", "bankcore360-admin-secret-key")

settings = Settings()
