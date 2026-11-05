"""
BankCore360 Approved Banking Policy Knowledge Base (RAG Document Store).
Contains authoritative lending policies, eligibility guidelines, documentation checklists,
and compliance rules for educational retail and commercial banking.
"""

BANKING_POLICIES = [
    {
        "id": "POL-001",
        "title": "General Retail Lending Criteria",
        "category": "ELIGIBILITY",
        "content": (
            "All applicants must be at least 21 years of age and hold an active, KYC-verified BankCore360 account. "
            "Minimum net monthly salary or verifiable business income requirement is ₹15,000. "
            "The applicant's Fixed Obligation to Income Ratio (FOIR) or Debt-to-Income (DTI) ratio must not exceed 50%. "
            "Applicants with an existing delinquency or overdue credit record are subject to mandatory manual review."
        ),
        "keywords": ["eligibility", "income", "age", "foir", "dti", "minimum salary"]
    },
    {
        "id": "POL-002",
        "title": "Loan Product Matrix and Pricing",
        "category": "PRODUCTS",
        "content": (
            "1. Personal Loan: Unsecured credit from ₹25,000 to ₹10,00,000. Tenure: 6 to 60 months. Interest: 10.50% - 14.50% p.a.\n"
            "2. Home Loan: Secured mortgage credit from ₹5,00,000 to ₹1,50,00,000. Tenure: 12 to 360 months. Interest: 8.50% - 9.75% p.a.\n"
            "3. Education Loan: Studies support from ₹50,000 to ₹30,00,000. Tenure: 12 to 120 months. Interest: 9.00% - 11.25% p.a.\n"
            "4. Business Loan: Working capital and expansion credit from ₹1,00,000 to ₹50,00,000. Tenure: 12 to 84 months. Interest: 11.00% - 15.50% p.a."
        ),
        "keywords": ["personal", "home", "education", "business", "interest rate", "tenure", "limit"]
    },
    {
        "id": "POL-003",
        "title": "Mandatory Supporting Documents Checklist",
        "category": "DOCUMENTATION",
        "content": (
            "Every loan application requires at least two mandatory verified documents before Manager forwarding:\n"
            "1. Primary Identity Proof: Test PAN Card (mandatory for credit assessment).\n"
            "2. Address Proof: Test Aadhaar Card or Recent Utility Bill.\n"
            "3. Income Verification: Recent 3 months salary slips or Income Tax Returns (ITR-V).\n"
            "4. Bank Statement: Recent 6 months continuous bank statement showing salary or operational cashflow credits.\n"
            "Documents must be uploaded in non-executable formats (PDF, JPEG, PNG) strictly under 5 MB."
        ),
        "keywords": ["document", "pan", "aadhaar", "salary slip", "bank statement", "proof"]
    },
    {
        "id": "POL-004",
        "title": "Maker-Checker-Manager Dual Control Governance",
        "category": "WORKFLOW",
        "content": (
            "BankCore360 enforces strict segregation of duties:\n"
            "1. Maker (Customer): Submits loan application and uploads documents.\n"
            "2. Checker: Inspects document authenticity, checks metadata, and verifies or rejects individual documents with reasons. "
            "Applications cannot be forwarded to Manager if any document is missing, pending, or rejected.\n"
            "3. Manager: Reviews the verified file, sets approved loan amount (cannot exceed requested amount), interest rate, "
            "tenure, and records final approval or rejection.\n"
            "4. Disbursement: Authorized separate operational step generating monthly reducing-balance EMI schedule."
        ),
        "keywords": ["maker", "checker", "manager", "workflow", "governance", "segregation of duties", "disbursement"]
    },
    {
        "id": "POL-005",
        "title": "Reducing Balance EMI Amortization Principles",
        "category": "EMI_CALCULATION",
        "content": (
            "All loans amortize on a monthly Reducing Balance formula:\n"
            "E = P * r * (1 + r)^n / ((1 + r)^n - 1)\n"
            "Where P is Principal, r is Monthly Interest Rate (Annual Rate / 1200), and n is Tenure in Months.\n"
            "Under reducing balance, interest is charged only on the unpaid principal balance each month. "
            "In early months, the interest component is higher; in later months, the principal component dominates."
        ),
        "keywords": ["reducing balance", "emi", "amortization", "formula", "interest calculation"]
    }
]

def search_policies(query: str, top_k: int = 3):
    """Simple keyword & relevance matching RAG search."""
    tokens = [t.lower() for t in query.split() if len(t) > 2]
    scored = []
    for policy in BANKING_POLICIES:
        score = 0
        text = (policy["title"] + " " + policy["content"]).lower()
        for kw in policy["keywords"]:
            if kw in query.lower():
                score += 3
        for tok in tokens:
            if tok in text:
                score += 1
        scored.append((score, policy))
    
    scored.sort(key=lambda x: x[0], reverse=True)
    results = [item[1] for item in scored[:top_k] if item[0] > 0]
    return results if results else [BANKING_POLICIES[0], BANKING_POLICIES[1]]

