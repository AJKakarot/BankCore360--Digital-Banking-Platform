"""
Document Intelligence Pre-Screening Assistant for synthetic test documents.
Extracts structured fields, calculates field confidence scores, checks consistency
against application details, and highlights potential concerns for human Checkers.
"""

import re
from typing import Dict, Any, List, Optional
from schemas import DocumentExtractionRequest, DocumentExtractionResponse, ExtractedField

def extract_document_information(req: DocumentExtractionRequest) -> DocumentExtractionResponse:
    text = (req.simulatedDocumentText or "").strip()
    doc_type = req.documentType.upper()
    applicant_name = (req.applicantName or "").strip()

    fields: Dict[str, ExtractedField] = {}
    concerns: List[str] = []
    checklist: List[str] = []

    if "PAN" in doc_type:
        _extract_pan(text, applicant_name, fields, concerns, checklist)
    elif "AADHAAR" in doc_type:
        _extract_aadhaar(text, applicant_name, fields, concerns, checklist)
    elif "SALARY" in doc_type or "INCOME" in doc_type:
        _extract_salary_slip(text, applicant_name, fields, concerns, checklist)
    elif "BANK" in doc_type or "STATEMENT" in doc_type:
        _extract_bank_statement(text, applicant_name, fields, concerns, checklist)
    else:
        _extract_generic(text, doc_type, applicant_name, fields, concerns, checklist)

    # Calculate overall confidence
    scores = [f.confidenceScore for f in fields.values()]
    overall_confidence = round(sum(scores) / len(scores), 2) if scores else 0.0
    is_valid = len(concerns) == 0 and overall_confidence >= 0.70

    return DocumentExtractionResponse(
        documentType=req.documentType,
        isSyntheticallyValid=is_valid,
        overallConfidence=overall_confidence,
        extractedFields=fields,
        flaggedConcerns=concerns,
        humanCheckerChecklist=checklist
    )

def _extract_pan(text: str, applicant: str, fields: Dict[str, ExtractedField],
                 concerns: List[str], checklist: List[str]):
    # 1. PAN Number Pattern: 5 letters, 4 digits, 1 letter (e.g. ABCDE1234F)
    pan_match = re.search(r"\b([A-Z]{5}[0-9]{4}[A-Z])\b", text.upper())
    if pan_match:
        val = pan_match.group(1)
        # Mask middle numbers for security: ABCDE****F
        masked_val = val[:5] + "****" + val[-1]
        fields["panNumber"] = ExtractedField(
            fieldName="panNumber",
            extractedValue=masked_val,
            confidenceScore=0.98,
            sourceExcerpt=pan_match.group(0),
            status="MATCHED"
        )
    else:
        fields["panNumber"] = ExtractedField(
            fieldName="panNumber",
            extractedValue=None,
            confidenceScore=0.0,
            status="MISSING"
        )
        concerns.append("Standard 10-character PAN pattern (e.g. ABCDE1234F) not detected in document text.")

    # 2. Name check
    _check_name_consistency(text, applicant, fields, concerns)

    # 3. DOB detection (DD/MM/YYYY or YYYY-MM-DD)
    dob_match = re.search(r"\b(\d{2}[/-]\d{2}[/-]\d{4}|\d{4}[/-]\d{2}[/-]\d{2})\b", text)
    if dob_match:
        fields["dateOfBirth"] = ExtractedField(
            fieldName="dateOfBirth",
            extractedValue=dob_match.group(1),
            confidenceScore=0.85,
            sourceExcerpt=dob_match.group(0),
            status="MATCHED"
        )
    else:
        fields["dateOfBirth"] = ExtractedField(
            fieldName="dateOfBirth",
            extractedValue=None,
            confidenceScore=0.20,
            status="MISSING"
        )
        concerns.append("Date of Birth not clearly identified in document.")

    checklist.extend([
        "Verify PAN holder name against customer registration record.",
        "Ensure signature and photograph are clearly visible and uncorrupted.",
        "Check that Date of Birth confirms applicant is at least 21 years old."
    ])

def _extract_aadhaar(text: str, applicant: str, fields: Dict[str, ExtractedField],
                     concerns: List[str], checklist: List[str]):
    # Aadhaar 12-digit pattern (with or without spaces/hyphens)
    aadhaar_match = re.search(r"\b(\d{4}[ -]?\d{4}[ -]?\d{4}|[X\d]{4}[ -]?[X\d]{4}[ -]?\d{4})\b", text)
    if aadhaar_match:
        raw = aadhaar_match.group(1).replace(" ", "").replace("-", "")
        masked = "XXXX-XXXX-" + raw[-4:]
        fields["aadhaarNumber"] = ExtractedField(
            fieldName="aadhaarNumber",
            extractedValue=masked,
            confidenceScore=0.95,
            sourceExcerpt=aadhaar_match.group(0),
            status="MATCHED"
        )
    else:
        fields["aadhaarNumber"] = ExtractedField(
            fieldName="aadhaarNumber",
            extractedValue=None,
            confidenceScore=0.0,
            status="MISSING"
        )
        concerns.append("12-digit Aadhaar pattern or masked Aadhaar format not detected.")

    _check_name_consistency(text, applicant, fields, concerns)

    # Address keywords
    has_address = any(w in text.lower() for w in ["address", "pin", "road", "street", "city", "state", "dist", "nagar"])
    fields["addressDetails"] = ExtractedField(
        fieldName="addressDetails",
        extractedValue="Detected" if has_address else None,
        confidenceScore=0.80 if has_address else 0.10,
        status="MATCHED" if has_address else "MISSING"
    )
    if not has_address:
        concerns.append("Residential address components could not be identified.")

    checklist.extend([
        "Confirm Aadhaar card has first 8 digits masked (regulatory compliance).",
        "Verify current residential address matches loan application details."
    ])

def _extract_salary_slip(text: str, applicant: str, fields: Dict[str, ExtractedField],
                         concerns: List[str], checklist: List[str]):
    _check_name_consistency(text, applicant, fields, concerns)

    # Detect Net Pay
    net_match = re.search(r"(?:net\s*(?:pay|salary)|take\s*home)[\s:=₹Rs.]*([\d,]+(?:\.\d{2})?)", text, re.IGNORECASE)
    if net_match:
        val_str = net_match.group(1).replace(",", "")
        try:
            val_num = float(val_str)
            fields["netSalary"] = ExtractedField(
                fieldName="netSalary",
                extractedValue=f"₹{val_num:,.2f}",
                confidenceScore=0.92,
                sourceExcerpt=net_match.group(0),
                status="MATCHED"
            )
            if val_num < 15000:
                concerns.append(f"Extracted net monthly salary (₹{val_num:,.2f}) is below the ₹15,000 policy threshold.")
        except ValueError:
            pass

    if "netSalary" not in fields:
        fields["netSalary"] = ExtractedField(
            fieldName="netSalary",
            extractedValue=None,
            confidenceScore=0.10,
            status="MISSING"
        )
        concerns.append("Explicit Net Salary or Take-Home Pay amount not detected in document.")

    # Employer Name
    emp_match = re.search(r"(?:employer|company|organization)[\s:=]+([A-Za-z0-9\s.,&'-]{3,40})", text, re.IGNORECASE)
    if emp_match:
        fields["employerName"] = ExtractedField(
            fieldName="employerName",
            extractedValue=emp_match.group(1).strip(),
            confidenceScore=0.85,
            sourceExcerpt=emp_match.group(0),
            status="MATCHED"
        )
    else:
        fields["employerName"] = ExtractedField(
            fieldName="employerName",
            extractedValue="Detected in header" if len(text) > 50 else None,
            confidenceScore=0.50,
            status="MATCHED" if len(text) > 50 else "MISSING"
        )

    checklist.extend([
        "Confirm employer name and verify against active corporate registry.",
        "Check that salary slip is from the most recent 3 calendar months.",
        "Verify net monthly salary satisfies the ₹15,000 minimum threshold."
    ])

def _extract_bank_statement(text: str, applicant: str, fields: Dict[str, ExtractedField],
                            concerns: List[str], checklist: List[str]):
    _check_name_consistency(text, applicant, fields, concerns)

    # Detect Account Number
    acct_match = re.search(r"(?:account|acct|a/c)[\s#:=no.]*([\d]{9,18})", text, re.IGNORECASE)
    if acct_match:
        raw = acct_match.group(1)
        masked = "XXXX" + raw[-4:]
        fields["accountNumber"] = ExtractedField(
            fieldName="accountNumber",
            extractedValue=masked,
            confidenceScore=0.90,
            sourceExcerpt=acct_match.group(0),
            status="MATCHED"
        )
    else:
        fields["accountNumber"] = ExtractedField(
            fieldName="accountNumber",
            extractedValue=None,
            confidenceScore=0.20,
            status="MISSING"
        )

    has_transactions = any(w in text.lower() for w in ["credit", "debit", "balance", "deposit", "transfer", "salary credit"])
    fields["transactionActivity"] = ExtractedField(
        fieldName="transactionActivity",
        extractedValue="Active Banking Transactions Detected" if has_transactions else None,
        confidenceScore=0.88 if has_transactions else 0.15,
        status="MATCHED" if has_transactions else "MISSING"
    )
    if not has_transactions:
        concerns.append("Standard bank statement debit/credit transaction entries not detected.")

    checklist.extend([
        "Verify statement covers a continuous 6-month period.",
        "Confirm regular monthly salary credits matching submitted salary slips.",
        "Check for any bounced cheques, overdraft penalties, or unserviced EMIs."
    ])

def _extract_generic(text: str, doc_type: str, applicant: str,
                     fields: Dict[str, ExtractedField], concerns: List[str], checklist: List[str]):
    _check_name_consistency(text, applicant, fields, concerns)
    fields["documentTextLength"] = ExtractedField(
        fieldName="documentTextLength",
        extractedValue=f"{len(text)} characters",
        confidenceScore=0.75 if len(text) > 50 else 0.25,
        status="MATCHED" if len(text) > 50 else "MISSING"
    )
    checklist.extend([
        f"Verify document validity dates for {doc_type}.",
        "Ensure issuing authority seal or digital signature is intact."
    ])

def _check_name_consistency(text: str, applicant: str, fields: Dict[str, ExtractedField], concerns: List[str]):
    if not applicant:
        return
    text_lower = text.lower()
    app_lower = applicant.lower()
    # Check full name or partial name match
    name_parts = [p for p in app_lower.split() if len(p) > 2]
    matched_parts = sum(1 for p in name_parts if p in text_lower)

    if app_lower in text_lower:
        score = 0.95
        status = "MATCHED"
        val = applicant
    elif name_parts and matched_parts >= len(name_parts) / 2:
        score = 0.75
        status = "MATCHED"
        val = f"{applicant} (Partial Match)"
    else:
        score = 0.20
        status = "INCONSISTENT"
        val = None
        concerns.append(f"Applicant name '{applicant}' was not clearly detected in the document text.")

    fields["applicantNameMatch"] = ExtractedField(
        fieldName="applicantNameMatch",
        extractedValue=val,
        confidenceScore=score,
        status=status
    )

