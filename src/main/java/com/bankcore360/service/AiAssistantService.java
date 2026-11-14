package com.bankcore360.service;

import com.bankcore360.dto.AiChatRequest;
import com.bankcore360.dto.AiChatResponse;
import com.bankcore360.dto.AiDocumentAnalysisRequest;
import com.bankcore360.dto.AiDocumentAnalysisResponse;
import com.bankcore360.entity.LoanDocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AiAssistantService {

    private final RestClient restClient;
    private final String aiServiceUrl;

    public AiAssistantService(@Value("${AI_SERVICE_URL:http://localhost:8000}") String aiServiceUrl) {
        this.aiServiceUrl = aiServiceUrl;
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5s fail-fast during Render cold start
        requestFactory.setReadTimeout(12000);  // 12s read timeout
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(aiServiceUrl)
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        // Try calling the FastAPI microservice first
        try {
            ResponseEntity<AiChatResponse> response = restClient.post()
                    .uri("/ai/chat")
                    .body(request)
                    .retrieve()
                    .toEntity(AiChatResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception ignored) {
            // Fallback to built-in RAG logic when external AI microservice is offline
        }

        return generateFallbackRagResponse(request);
    }

    public Map<String, Object> explainEmi(BigDecimal principal, BigDecimal interestRate, Integer tenureMonths) {
        if (principal == null) principal = new BigDecimal("100000.00");
        if (interestRate == null) interestRate = new BigDecimal("10.50");
        if (tenureMonths == null) tenureMonths = 12;

        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("1200"), 6, RoundingMode.HALF_UP);
        double r = monthlyRate.doubleValue();
        double p = principal.doubleValue();
        int n = tenureMonths;

        double emiReducing = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        BigDecimal emi = BigDecimal.valueOf(emiReducing).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepayment = emi.multiply(BigDecimal.valueOf(n));
        BigDecimal totalInterest = totalRepayment.subtract(principal);

        // Flat interest comparison
        BigDecimal flatInterest = principal.multiply(interestRate).multiply(BigDecimal.valueOf(n))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
        BigDecimal flatTotal = principal.add(flatInterest);
        BigDecimal flatEmi = flatTotal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        Map<String, Object> explanation = new LinkedHashMap<>();
        explanation.put("loanAmount", principal);
        explanation.put("annualInterestRatePercent", interestRate);
        explanation.put("tenureMonths", tenureMonths);
        explanation.put("reducingBalanceMonthlyEmi", emi);
        explanation.put("reducingBalanceTotalInterest", totalInterest);
        explanation.put("reducingBalanceTotalRepayment", totalRepayment);
        explanation.put("flatRateMonthlyEmi", flatEmi);
        explanation.put("flatRateTotalInterest", flatInterest);
        explanation.put("educationalExplanation",
                "Under the Reducing Balance method used by BankCore360, interest is calculated each month only on the remaining unpaid principal balance. "
                        + "As you repay principal every month, the interest portion of each EMI progressively decreases, while the principal portion increases. "
                        + "This saves you ₹" + flatInterest.subtract(totalInterest).max(BigDecimal.ZERO) + " in total interest compared to flat rate interest.");

        return explanation;
    }

    public AiDocumentAnalysisResponse analyzeDocument(AiDocumentAnalysisRequest request) {
        // Try calling the FastAPI microservice first
        try {
            ResponseEntity<AiDocumentAnalysisResponse> response = restClient.post()
                    .uri("/ai/analyze-document")
                    .body(request)
                    .retrieve()
                    .toEntity(AiDocumentAnalysisResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception ignored) {
            // Fallback to built-in analysis
        }

        return generateFallbackDocumentAnalysis(request);
    }

    private AiChatResponse generateFallbackRagResponse(AiChatRequest request) {
        String msg = request.getMessage().toLowerCase();

        if (msg.contains("hi") || msg.contains("hello") || msg.contains("hey") || msg.contains("namaste")) {
            return new AiChatResponse(
                    "Hello! I am your BankCore360 AI Banking Assistant. I can assist you with retail loan eligibility, reducing balance EMI calculation, KYC documentation, and Maker-Checker approval workflows. What can I help you with today?",
                    "GREETING",
                    List.of("BankCore360 Customer Self-Service Guidelines 2026"),
                    List.of(
                            "Check loan eligibility and income requirements",
                            "Explore Personal, Home, Education, and Business loan limits",
                            "Learn about required KYC documents and file formats"
                    )
            );
        }

        if (msg.contains("personal")) {
            return new AiChatResponse(
                    "BankCore360 Personal Loans offer quick financing from ₹25,000 to ₹10,00,000 with tenures up to 60 months. Interest rates range between 11.5% and 14.0% p.a. (reducing balance). Eligibility requires a minimum net monthly income of ₹15,000 and max FOIR of 50%.",
                    "PRODUCT_INQUIRY_PERSONAL",
                    List.of("Product Schedule: Personal Lending Guidelines 2026"),
                    List.of("Apply online via /customer/apply", "Upload 3 months payslips and test PAN")
            );
        }

        if (msg.contains("home") || msg.contains("house") || msg.contains("property")) {
            return new AiChatResponse(
                    "BankCore360 Home Loans provide housing finance from ₹5,00,000 to ₹1,50,00,000 with flexible repayment tenures up to 30 years (360 months). Competitive annual interest rates start from 8.75% to 9.50% p.a. Tax benefits apply under Section 80C and Section 24.",
                    "PRODUCT_INQUIRY_HOME",
                    List.of("Product Schedule: Housing Finance Policy 2026"),
                    List.of("Check indicative EMI on the Loan Originator page", "Submit property title documents and bank statements")
            );
        }

        if (msg.contains("education") || msg.contains("study") || msg.contains("college")) {
            return new AiChatResponse(
                    "BankCore360 Education Loans support students from ₹50,000 up to ₹30,00,000 at 9.0% to 11.0% p.a. for domestic and international courses. Includes a moratorium period (Course Duration + 6 months) before principal repayment commences.",
                    "PRODUCT_INQUIRY_EDUCATION",
                    List.of("Product Schedule: Higher Education Financing Policy 2026"),
                    List.of("Provide admission offer letter and fee schedule", "Co-applicant income proof required")
            );
        }

        if (msg.contains("business") || msg.contains("sme") || msg.contains("enterprise")) {
            return new AiChatResponse(
                    "BankCore360 Business Loans offer working capital and term loans from ₹1,00,000 to ₹50,00,000 with repayment tenures up to 84 months. Interest rates start at 12.0% p.a. tailored for growth enterprises and MSMEs.",
                    "PRODUCT_INQUIRY_BUSINESS",
                    List.of("Product Schedule: MSME & Commercial Credit Policy 2026"),
                    List.of("Submit 12 months GST returns and audited balance sheet", "Operative business bank account statement")
            );
        }

        if (msg.contains("status") || msg.contains("track") || msg.contains("where is my loan")) {
            return new AiChatResponse(
                    "You can track the live progress of your loan anytime under 'My Loan Applications' (/customer/applications). The stages proceed: SUBMITTED -> UNDER_REVIEW (Checker KYC verification) -> PENDING_MANAGER_APPROVAL -> APPROVED / DISBURSED.",
                    "STATUS_TRACKING",
                    List.of("Customer Service SOP: Real-time Lifecycle Visibility"),
                    List.of("Open Portfolio dashboard to review current application stage", "Check real-time notifications for automated status updates")
            );
        }

        if (msg.contains("eligib") || msg.contains("salary") || msg.contains("income") || msg.contains("qualify") || msg.contains("cibil")) {
            return new AiChatResponse(
                    "To qualify for a BankCore360 loan, your minimum monthly income should be at least ₹15,000. "
                            + "We evaluate eligibility using the Fixed Obligation to Income Ratio (FOIR), where your total monthly EMIs "
                            + "(including the new loan) must not exceed 50% of your net monthly income. A preferred CIBIL score of 700+ ensures faster sanction.",
                    "ELIGIBILITY_INQUIRY",
                    List.of(
                            "BankCore360 Credit Policy Section 3.1: Minimum Monthly Income ₹15,000",
                            "BankCore360 Credit Policy Section 3.2: Maximum Debt-to-Income / FOIR is 50%"
                    ),
                    List.of(
                            "Use the Loan Eligibility Calculator at /api/loans/calculator to check your borrowing capacity",
                            "Ensure your KYC profile is completed before submitting an application"
                    )
            );
        }

        if (msg.contains("document") || msg.contains("pan") || msg.contains("aadhaar") || msg.contains("upload") || msg.contains("proof")) {
            return new AiChatResponse(
                    "For loan processing, BankCore360 requires: (1) Identity Proof: Dummy PAN Card, "
                            + "(2) Address Proof: Dummy Aadhaar or Utility Bill, (3) Income Proof: Last 3 months payslips, "
                            + "and (4) Bank Statement: 6 months operative bank account statement. Documents must be in PDF, JPEG, or PNG format under 5 MB.",
                    "DOCUMENTATION_INQUIRY",
                    List.of(
                            "KYC & Documentation Policy Section 4: Acceptable Identification and Financial Proofs",
                            "File Storage Guidelines: Non-executable formats only (PDF/JPEG/PNG) up to 5 MB"
                    ),
                    List.of(
                            "Submit your application first via POST /api/loans/apply",
                            "Upload test documents through POST /api/loans/{reference}/documents"
                    )
            );
        }

        if (msg.contains("emi") || msg.contains("interest") || msg.contains("rate") || msg.contains("repay")) {
            return new AiChatResponse(
                    "BankCore360 calculates repayments using the standardized reducing-balance method. "
                            + "Our indicative annual interest rates are: Personal Loan: 10.5%–14%, Home Loan: 8.5%–9.5%, "
                            + "Education Loan: 9.0%–11.0%, and Business Loan: 11.0%–15.0%. Tenures range from 6 to 360 months.",
                    "INTEREST_AND_REPAYMENT_INQUIRY",
                    List.of(
                            "Product Schedule: Retail & SME Lending Interest Matrix 2026",
                            "Standard Repayment Terms: Monthly Reducing Balance Amortization"
                    ),
                    List.of(
                            "Try the EMI endpoint GET /api/ai/explain-emi with your target amount and tenure",
                            "View generated monthly schedules after loan disbursement"
                    )
            );
        }

        if (msg.contains("maker") || msg.contains("checker") || msg.contains("flow") || msg.contains("dual")
                || msg.contains("control") || msg.contains("segregation") || msg.contains("step")
                || msg.contains("process") || msg.contains("workflow") || msg.contains("approval")
                || msg.contains("how") || msg.contains("stage") || msg.contains("kya") || msg.contains("batao")) {
            return new AiChatResponse(
                    "### 🛡️ BankCore360 Maker-Checker-Manager Dual Control Flow\n\n"
                            + "The **Maker-Checker Flow** (also known as the *Four-Eyes Principle* or *Segregation of Duties*) is a fundamental banking governance framework. "
                            + "It guarantees that no single person can unilaterally originate, approve, and disburse bank funds, strictly eliminating fraud, internal bias, and errors.\n\n"
                            + "#### 👥 3 Distinct Roles & Responsibilities:\n"
                            + "1. **The Maker (Customer / Loan Originator):**\n"
                            + "   - Initiates the loan application with purpose, requested amount, and tenure.\n"
                            + "   - Uploads mandatory identification & financial proofs (PAN Card, Aadhaar, 3 Months Payslips, Bank Statements).\n"
                            + "   - Initial application state: `SUBMITTED`.\n\n"
                            + "2. **The Checker (Compliance & KYC Verification Officer):**\n"
                            + "   - Operates in an independent verification queue (`/checker/dashboard`).\n"
                            + "   - Authenticates uploaded documents against KYC criteria and checks minimum ₹15,000 monthly income.\n"
                            + "   - Marks each document as `VERIFIED` or `REJECTED` with mandatory audit remarks.\n"
                            + "   - Cannot approve loans or disburse funds. Lifecycle state transitions: `UNDER_REVIEW` ➔ `DOCUMENTS_VERIFIED` ➔ `PENDING_MANAGER_APPROVAL`.\n\n"
                            + "3. **The Approver / Manager (Branch Credit Sanctioning Authority):**\n"
                            + "   - Evaluates credit risk, verified proofs, CIBIL benchmark (700+), and FOIR debt-to-income ratio (strictly ≤ 50%).\n"
                            + "   - Holds sole sanction authority to `APPROVE` (setting final interest rate & sanctioned amount) or `REJECT` the application.\n"
                            + "   - Authorizes atomic **Disbursement** directly from the BankCore360 ₹100 Lakh Vault to the Customer's Operative Account.\n\n"
                            + "#### 🔐 Core Banking Safeguards:\n"
                            + "• **Self-Review Prevention:** A user is blocked by the system from reviewing or approving their own loan.\n"
                            + "• **Immutable Audit Trail:** Every submission, verification note, approval, and vault ledger debit is logged permanently with actor username and timestamps.",
                    "MAKER_CHECKER_FLOW",
                    List.of(
                            "Operational SOP: Maker-Checker Governance & Dual Control Policy (POL-MC-2026-V1)",
                            "Regulatory Banking Standard: Four-Eyes Principle & Segregation of Duties Mandate"
                    ),
                    List.of(
                            "Submit an application as CUSTOMER at /customer/apply",
                            "Review pending applications as CHECKER at /checker/dashboard",
                            "Sanction and disburse loans as MANAGER at /manager/dashboard"
                    )
            );
        }

        return new AiChatResponse(
                "Welcome to BankCore360 AI Assistant! I can explain loan products (Personal, Home, Education, Business), "
                        + "calculate eligibility criteria, detail required documents, or clarify the Maker-Checker-Manager approval process.",
                "GENERAL_ASSISTANCE",
                List.of("BankCore360 General Lending Guidelines 2026"),
                List.of(
                        "Ask: 'What documents are required for a personal loan?'",
                        "Ask: 'How is reducing balance EMI calculated?'",
                        "Ask: 'What are the steps from application to disbursement?'"
                )
        );
    }

    private AiDocumentAnalysisResponse generateFallbackDocumentAnalysis(AiDocumentAnalysisRequest request) {
        LoanDocumentType docType = request.getDocumentType();
        String text = request.getSimulatedDocumentText() != null ? request.getSimulatedDocumentText() : "";
        String applicant = request.getApplicantName() != null ? request.getApplicantName() : "";

        Map<String, String> fields = new LinkedHashMap<>();
        List<String> concerns = new ArrayList<>();
        List<String> checklist = new ArrayList<>();

        switch (docType) {
            case PAN -> {
                fields.put("documentType", "PAN Card");
                boolean hasPanNumber = text.matches(".*[A-Z]{5}[0-9]{4}[A-Z].*");
                fields.put("detectedPanFormat", hasPanNumber ? "Valid 10-digit Alphanumeric" : "Not detected");
                if (!hasPanNumber) {
                    concerns.add("Document text does not match standard 10-digit PAN format (e.g., ABCDE1234F).");
                }
                if (!applicant.isBlank() && !text.toLowerCase().contains(applicant.toLowerCase())) {
                    concerns.add("Applicant name '" + applicant + "' was not detected on the PAN document.");
                }
                checklist.add("Verify date of birth against Customer profile");
                checklist.add("Ensure photograph and signature are clearly legible");
            }
            case INCOME_PROOF -> {
                fields.put("documentType", "Salary Slip / Income Proof");
                boolean hasGross = text.toLowerCase().contains("gross") || text.toLowerCase().contains("salary") || text.toLowerCase().contains("net");
                fields.put("salaryKeywordsDetected", hasGross ? "Yes" : "No");
                if (!hasGross) {
                    concerns.add("Gross or Net salary figure could not be automatically identified.");
                }
                checklist.add("Verify employer name and designation");
                checklist.add("Confirm pay period matches recent 3 calendar months");
            }
            case BANK_STATEMENT -> {
                fields.put("documentType", "Bank Statement");
                boolean hasTransactions = text.toLowerCase().contains("balance") || text.toLowerCase().contains("credit");
                fields.put("financialActivityDetected", hasTransactions ? "Yes" : "No");
                checklist.add("Confirm minimum 6-month continuous statement period");
                checklist.add("Cross-check salary credit transactions against pay slip");
            }
            default -> {
                fields.put("documentType", docType.name());
                checklist.add("Inspect validity, issue date, and issuing authority");
            }
        }

        boolean complete = concerns.isEmpty();
        return new AiDocumentAnalysisResponse(
                docType,
                complete,
                fields,
                concerns,
                checklist
        );
    }
}

