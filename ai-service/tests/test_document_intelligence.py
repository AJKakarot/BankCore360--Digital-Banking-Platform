"""
Tests for Document Intelligence Pre-Screening Assistant.
"""

import pytest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from document_intelligence import extract_document_information
from schemas import DocumentExtractionRequest

def test_extract_pan_card_success():
    req = DocumentExtractionRequest(
        documentType="PAN",
        simulatedDocumentText=(
            "INCOME TAX DEPARTMENT GOVT OF INDIA\n"
            "PERMANENT ACCOUNT NUMBER CARD\n"
            "ABCDE1234F\n"
            "NAME: ARJUN SHARMA\n"
            "DATE OF BIRTH: 12/04/1988\n"
        ),
        applicantName="Arjun Sharma"
    )
    res = extract_document_information(req)
    assert res.isSyntheticallyValid is True
    assert res.overallConfidence >= 0.85
    assert "panNumber" in res.extractedFields
    assert res.extractedFields["panNumber"].extractedValue == "ABCDE****F"
    assert res.extractedFields["panNumber"].status == "MATCHED"
    assert res.extractedFields["applicantNameMatch"].status == "MATCHED"
    assert len(res.humanCheckerChecklist) >= 2

def test_extract_pan_card_invalid_pattern():
    req = DocumentExtractionRequest(
        documentType="PAN",
        simulatedDocumentText="Invalid Card Document text without any PAN",
        applicantName="Arjun Sharma"
    )
    res = extract_document_information(req)
    assert res.isSyntheticallyValid is False
    assert res.extractedFields["panNumber"].status == "MISSING"
    assert any("pattern" in c.lower() for c in res.flaggedConcerns)

def test_extract_salary_slip_success():
    req = DocumentExtractionRequest(
        documentType="SALARY_SLIP",
        simulatedDocumentText=(
            "ACME CORP TECHNOLOGIES PAYSLIP\n"
            "Employee: Priya Patel | EmpID: AC9921\n"
            "Basic: 35,000 | HRA: 15,000 | Allowances: 10,000\n"
            "Gross Salary: ₹60,000.00\n"
            "Deductions: ₹5,000.00\n"
            "Net Salary: ₹55,000.00\n"
        ),
        applicantName="Priya Patel"
    )
    res = extract_document_information(req)
    assert res.isSyntheticallyValid is True
    assert "netSalary" in res.extractedFields
    assert "55,000" in res.extractedFields["netSalary"].extractedValue
    assert res.extractedFields["applicantNameMatch"].status == "MATCHED"

def test_extract_salary_slip_below_policy_threshold():
    req = DocumentExtractionRequest(
        documentType="SALARY_SLIP",
        simulatedDocumentText="Part-time work Net Salary: ₹10,000.00 Employee: Test",
        applicantName="Test"
    )
    res = extract_document_information(req)
    assert any("below the ₹15,000 policy threshold" in c for c in res.flaggedConcerns)

