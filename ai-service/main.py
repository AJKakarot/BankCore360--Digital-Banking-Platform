"""
BankCore360 AI Banking Assistant Microservice.
Built with FastAPI, LangGraph, RAG, and Grok API by xAI.
Exposes the Banking Policy Assistant endpoints.
"""

from app.main import app

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
