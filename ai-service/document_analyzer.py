"""
Document pre-screening and metadata consistency analysis module.
Performs preliminary format checks, detects missing fields, and highlights potential
concerns for human Checker verification.
"""

from typing import Dict, Any, List

def analyze_document_text(document_type: str, simulated_text: str, applicant_name: str = "") -> Dict[str, Any]:
    text = simulated_text or ""
    applicant = applicant_name or ""
    
    extracted = {}
    concerns = []
    checklist = []
    
    doc_upper = document_type.upper()
    
    if "PAN" in doc_upper:
        extracted["documentType"] = "PAN Card"
        # Check standard 10-char PAN format
        import re
        pan_match = re.search(r"[A-Z]{5}[0-9]{4}[A-Z]", text.upper())
        if pan_match:
            extracted["detectedPanNumber"] = pan_match.group(0)
            extracted["formatValid"] = "YES"
        else:
            extracted["detectedPanNumber"] = "Not found"
            extracted["formatValid"] = "NO"
            concerns.append("Standard 10-digit PAN alphanumeric pattern (e.g. ABCDE1234F) not detected in document text.")
            
        if applicant and applicant.lower() not in text.lower():
            concerns.append(f"Applicant name '{applicant}' was not clearly matched in document text.")
            
        checklist = [
            "Verify PAN holder name against Customer registration record",
            "Ensure signature and photo are clearly visible",
            "Verify date of birth matches system records"
        ]
        
    elif "INCOME" in doc_upper or "SALARY" in doc_upper:
        extracted["documentType"] = "Income Proof / Salary Slip"
        has_salary = any(term in text.lower() for term in ["gross", "net", "salary", "basic", "ctc"])
        extracted["salaryFiguresDetected"] = "YES" if has_salary else "NO"
        if not has_salary:
            concerns.append("Could not identify explicit Gross/Net salary amounts in text.")
            
        checklist = [
            "Confirm employer organization name and contact info",
            "Verify net monthly salary meets the ₹15,000 threshold",
            "Ensure pay slips cover the recent 3 calendar months"
        ]
        
    elif "BANK" in doc_upper:
        extracted["documentType"] = "Bank Statement"
        has_txns = any(term in text.lower() for term in ["balance", "credit", "debit", "deposit"])
        extracted["transactionActivityDetected"] = "YES" if has_txns else "NO"
        
        checklist = [
            "Confirm minimum 6-month continuous statement period",
            "Cross-verify monthly salary credits against submitted pay slips",
            "Check for any bounced cheques or overdraft penalties"
        ]
        
    else:
        extracted["documentType"] = document_type
        checklist = [
            "Verify issuing authority and document validity dates",
            "Check applicant name and address consistency"
        ]
        
    metadata_complete = len(concerns) == 0
    
    return {
        "documentType": document_type,
        "metadataComplete": metadata_complete,
        "extractedFields": extracted,
        "flaggedInconsistencies": concerns,
        "humanReviewerChecklist": checklist,
        "advisoryNotice": "AI Pre-Screening is for advisory guidance only. Final verification must be performed by human Checker."
    }

