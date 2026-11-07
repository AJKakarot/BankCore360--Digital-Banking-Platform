"""
Pydantic v2 schemas for BankCore360 AI Banking Assistant.
"""

from enum import Enum
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field

# Policy Categories
class PolicyCategory(str, Enum):
    PERSONAL_LOAN = "Personal Loan Policy"
    HOME_LOAN = "Home Loan Policy"
    EDUCATION_LOAN = "Education Loan Policy"
    BUSINESS_LOAN = "Business Loan Policy"
    KYC_AND_DOCUMENT = "KYC and Document Policy"
    INTEREST_RATE = "Interest Rate Policy"
    REPAYMENT_AND_EMI = "Repayment and EMI Policy"
    LOAN_ELIGIBILITY = "Loan Eligibility Policy"

class LoanType(str, Enum):
    PERSONAL = "PERSONAL"
    HOME = "HOME"
    EDUCATION = "EDUCATION"
    BUSINESS = "BUSINESS"

class IngestionStatus(str, Enum):
    PENDING = "PENDING"
    INDEXED = "INDEXED"
    FAILED = "FAILED"

# --- Policy Admin Schemas ---
class PolicyChunkResponse(BaseModel):
    chunkId: int
    chunkIndex: int
    pageNumber: int
    content: str
    category: str
    loanType: Optional[str] = None
    version: str
    isActive: bool

class PolicyDocumentResponse(BaseModel):
    id: int
    title: str
    filename: str
    category: str
    loanType: Optional[str] = None
    version: str
    isActive: bool
    ingestionStatus: str
    chunkCount: int
    uploadedBy: str
    uploadedAt: datetime
    updatedAt: Optional[datetime] = None

class PolicyDocumentDetailResponse(PolicyDocumentResponse):
    chunks: List[PolicyChunkResponse] = []

class PolicyPublishRequest(BaseModel):
    adminUsername: Optional[str] = "ADMIN"

# --- Chat & RAG Schemas ---
class SourceCitation(BaseModel):
    documentId: int
    documentName: str
    policyCategory: str
    version: str
    pageNumber: int
    similarityScore: float
    excerpt: str

class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000, description="Customer question or inquiry")
    category: Optional[str] = Field(None, description="Optional policy category filter")
    loanType: Optional[str] = Field(None, description="Optional loan type filter (PERSONAL, HOME, etc.)")
    customerUsername: Optional[str] = Field(None, description="Authenticated customer username")
    applicationReference: Optional[str] = Field(None, description="Optional loan application reference (e.g. LN-2026-AB12CD)")

class ChatResponse(BaseModel):
    response: str
    intent: str
    sources: List[SourceCitation] = []
    relevantPolicies: List[str] = []
    suggestedNextSteps: List[str] = []
    guardrailWarnings: List[str] = []
    disclaimer: str = (
        "Educational AI Banking Assistant guidance. "
        "Final credit and document decisions rest solely with authorized human BankCore360 Checker and Manager officials. "
        "Indicative advice does not constitute a guaranteed loan sanction."
    )

# --- Loan Eligibility Assistant Schemas ---
class EligibilityExplanationRequest(BaseModel):
    loanType: str = Field(..., description="PERSONAL, HOME, EDUCATION, BUSINESS")
    monthlyIncome: float = Field(..., gt=0, description="Monthly net income in INR")
    existingMonthlyEmi: float = Field(default=0.0, ge=0, description="Total current monthly EMIs in INR")
    requestedAmount: float = Field(..., gt=0, description="Requested principal amount in INR")
    desiredTenureMonths: int = Field(..., gt=0, le=360, description="Requested tenure in months")
    customerUsername: Optional[str] = None

class CalculationBreakdown(BaseModel):
    principalAmount: float
    annualInterestRatePercent: float
    tenureMonths: int
    monthlyEmi: float
    totalInterest: float
    totalRepayment: float
    totalMonthlyObligation: float
    foirPercent: float
    policyMaxFoirPercent: float = 50.0
    minimumIncomeRequired: float = 15000.0

class EligibilityExplanationResponse(BaseModel):
    indicativeStatus: str  # INDICATIVE_ELIGIBLE, INDICATIVE_INELIGIBLE, HIGH_FOIR_WARNING
    calculations: CalculationBreakdown
    naturalLanguageExplanation: str
    recommendations: List[str]
    disclaimer: str = (
        "This calculation is purely indicative and educational based on stated inputs. "
        "BankCore360 credit sanction is subject to formal credit bureau verification, "
        "KYC document authentication by Checker, and credit appraisal by Manager. "
        "Indicative guidance does not constitute a guaranteed loan sanction. "
        "The AI Assistant cannot promise or approve loans."
    )

# --- Document Intelligence Schemas ---
class DocumentExtractionRequest(BaseModel):
    documentType: str = Field(..., description="PAN, AADHAAR, SALARY_SLIP, BANK_STATEMENT, etc.")
    simulatedDocumentText: Optional[str] = Field(None, description="Text content extracted or simulated from test document")
    applicantName: Optional[str] = Field(None, description="Expected customer full name for cross-validation")
    applicationReference: Optional[str] = Field(None, description="Associated loan application reference")

class ExtractedField(BaseModel):
    fieldName: str
    extractedValue: Optional[str]
    confidenceScore: float  # 0.0 to 1.0
    sourceExcerpt: Optional[str] = None
    status: str  # MATCHED, MISSING, INCONSISTENT

class DocumentExtractionResponse(BaseModel):
    documentType: str
    isSyntheticallyValid: bool
    overallConfidence: float
    extractedFields: Dict[str, ExtractedField]
    flaggedConcerns: List[str]
    humanCheckerChecklist: List[str]
    advisoryNotice: str = (
        "AI Pre-Screening is an automated advisory assistance tool. "
        "AI cannot independently verify identity, certify authenticity, or approve documents. "
        "Final document verification must be performed by an authorized human BankCore360 Checker."
    )

# --- Application Status Assistant Schemas ---
class ApplicationStatusResponse(BaseModel):
    applicationReference: str
    customerFullName: str
    status: str
    loanType: str
    requestedAmount: float
    submittedAt: Optional[str]
    pendingDocuments: List[str] = []
    rejectedDocuments: List[Dict[str, str]] = []
    nextAction: str
    explanation: str

# --- Event Envelope ---
class AiEventEnvelope(BaseModel):
    eventId: str
    eventType: str
    aggregateId: str
    occurredAt: datetime
    version: int = 1
    payload: Dict[str, Any]
