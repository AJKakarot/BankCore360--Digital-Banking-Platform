# BankCore360 AI Banking Assistant Microservice

This module is an independent Python FastAPI service integrating LangGraph, LangChain, and RAG knowledge retrieval for the BankCore360 Educational Digital Banking Platform.

## Features
1. **Loan Policy Q&A via RAG**: Instant answers to borrower queries based on BankCore360 lending guidelines.
2. **Eligibility Pre-screening**: Explains minimum salary (₹15,000) and maximum debt-to-income (50% FOIR) rules.
3. **Document Sanity Pre-checking**: Detects missing or inconsistent fields on simulated test documents to assist human Checkers.
4. **Reducing Balance vs Flat Rate EMI Explanations**: Demonstrates the mathematical difference and interest savings.
5. **Maker-Checker Governance Safe Design**: The AI cannot independently approve, reject, or disburse loans. All financial decisions are reserved for human bank staff.

## Running the Service
```bash
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --port 8000 --reload
```

Interactive API documentation will be available at: `http://localhost:8000/docs`

> Note: The core Spring Boot application includes a resilient client (`AiAssistantService.java`) with built-in fallback logic, ensuring the banking platform remains fully operational even when this AI service is offline.

