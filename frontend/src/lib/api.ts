/**
 * BankCore360 API Client
 * Connects Next.js Frontend to Spring Boot backend (Port 8080)
 * and FastAPI AI Service (Port 8000).
 */

export const SPRING_BOOT_API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
export const AI_SERVICE_API = process.env.NEXT_PUBLIC_AI_URL || "http://localhost:8000";

export type RoleType = "CUSTOMER" | "CHECKER" | "MANAGER" | "ADMIN";

export interface UserSession {
  username: string;
  role: RoleType;
  email: string;
  token?: string;
  password?: string;
}

// Session Management via LocalStorage & Cookies
export function getActiveUser(): UserSession | null {
  if (typeof window === "undefined") return null;
  const stored = localStorage.getItem("bankcore360_user");
  if (!stored) return null;
  try {
    return JSON.parse(stored);
  } catch {
    return null;
  }
}

export function saveSession(user: UserSession): void {
  if (typeof window !== "undefined") {
    localStorage.setItem("bankcore360_user", JSON.stringify(user));
    // Set cookie for Next.js middleware route protection
    document.cookie = `bankcore360_user=${encodeURIComponent(JSON.stringify(user))}; path=/; max-age=86400; SameSite=Lax`;
  }
}

export function clearSession(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem("bankcore360_user");
    document.cookie = "bankcore360_user=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax";
  }
}

export function getSavedSession(): UserSession | null {
  return getActiveUser();
}

export function getAuthHeaders(): HeadersInit {
  const session = getActiveUser();
  if (!session || !session.username) {
    return {
      "Content-Type": "application/json",
      "Accept": "application/json",
    };
  }

  const password = session.password || "Password@123";
  const credentials = btoa(`${session.username}:${password}`);
  return {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "Authorization": `Basic ${credentials}`,
  };
}

export function getRoleAuthHeaders(requiredRole?: RoleType): HeadersInit {
  return getAuthHeaders();
}

export async function loginUser(username: string, password: string): Promise<UserSession> {
  const cleanUsername = username.trim();
  const cleanPassword = password.trim();

  let res: Response;
  try {
    res = await fetch(`${SPRING_BOOT_API}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({ username: cleanUsername, password: cleanPassword }),
    });
  } catch (netErr: any) {
    throw new Error(
      `Cannot connect to BankCore360 backend at ${SPRING_BOOT_API}. Please ensure the backend is running and reachable.`
    );
  }

  if (!res.ok) {
    let errMessage = "Invalid credentials. User not found in database or password incorrect.";
    try {
      const errData = await res.json();
      if (errData.message) {
        errMessage = errData.message;
      }
    } catch {
      // ignore
    }
    throw new Error(errMessage);
  }

  const data = await res.json();
  const session: UserSession = {
    username: data.username || cleanUsername,
    role: (data.role as RoleType) || "CUSTOMER",
    email: data.email || `${cleanUsername}@bankcore360.com`,
    password: cleanPassword, // Stored to authenticate subsequent API calls with HTTP Basic
  };

  saveSession(session);
  return session;
}

export async function registerCustomer(data: {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phoneNumber?: string;
}): Promise<any> {
  let res: Response;
  try {
    res = await fetch(`${SPRING_BOOT_API}/api/auth/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({
        username: data.username.trim(),
        email: data.email.trim(),
        password: data.password.trim(),
        fullName: data.fullName.trim(),
        phoneNumber: data.phoneNumber?.trim() || "9876543210",
        roles: ["CUSTOMER"],
      }),
    });
  } catch (err: any) {
    throw new Error(
      `Cannot connect to BankCore360 backend at ${SPRING_BOOT_API}. Account creation requires an active backend database.`
    );
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || "Registration failed. Username or email may already be in use.");
  }
  return res.json();
}

// --- Loan Application Types ---
export interface LoanApplication {
  id: number;
  applicationReference: string;
  customerId?: number;
  customerFullName: string;
  customerEmail: string;
  loanType: "PERSONAL" | "HOME" | "EDUCATION" | "BUSINESS";
  requestedAmount: number;
  tenureMonths: number;
  purpose: string;
  status: "SUBMITTED" | "UNDER_REVIEW" | "DOCUMENTS_VERIFIED" | "PENDING_MANAGER_APPROVAL" | "APPROVED" | "DISBURSED" | "REJECTED";
  submittedAt: string;
  approvedAmount?: number;
  interestRate?: number;
  approvedTenureMonths?: number;
  rejectionReason?: string;
  managerRemarks?: string;
  disbursementReference?: string;
}

export interface LoanDocument {
  id: number;
  loanApplicationId: number;
  applicationReference?: string;
  documentType: string;
  fileName: string;
  fileSize: number;
  documentStatus: "PENDING" | "VERIFIED" | "REJECTED";
  rejectionReason?: string;
  remarks?: string;
  uploadedAt: string;
  reviewedBy?: string;
  downloadUrl?: string;
  previewUrl?: string;
}

export interface NotificationItem {
  id: number;
  recipientUsername?: string;
  notificationType?: string;
  title: string;
  message: string;
  relatedReference?: string;
  readStatus: boolean;
  createdAt: string;
}

export interface EventChannelDetail {
  name: string;
  consumerGroup: string;
  description: string;
}

export interface OutboxEventItem {
  id: number;
  eventId: string;
  eventType: string;
  aggregateId: string;
  topic: string;
  payloadJson: string;
  status: "PENDING" | "PUBLISHED" | "FAILED";
  retryCount: number;
  createdAt: string;
  publishedAt?: string;
  lastError?: string;
}

export interface ProcessedEventItem {
  id: number;
  eventId: string;
  eventType: string;
  consumerGroup: string;
  processedAt: string;
}

export interface EventStreamStatus {
  streamEnabled?: boolean;
  streamEngine?: string;
  channels?: EventChannelDetail[];
  topics?: EventChannelDetail[];
  outboxStats: {
    totalEvents: number;
    pendingEvents: number;
    publishedEvents: number;
    failedEvents: number;
  };
  outboxEvents: OutboxEventItem[];
  processedEvents: ProcessedEventItem[];
}

export interface AuditLogItem {
  id: number;
  applicationReference: string;
  actorUsername: string;
  actorRole: string;
  action: string;
  previousState?: string;
  newState?: string;
  remarks: string;
  timestamp: string;
}

// --- AI Chat Types ---
export interface SourceCitation {
  documentId?: number;
  documentName?: string;
  policyCategory?: string;
  version?: string;
  pageNumber?: number;
  similarityScore?: number;
  excerpt?: string;
  policy_id?: string;
  section?: string;
  text?: string;
}

export interface AiChatResponse {
  response?: string;
  answer?: string;
  intent?: string;
  sources?: SourceCitation[];
  relevantPolicies?: string[];
  suggestedNextSteps?: string[];
  guardrailWarnings?: string[];
  disclaimer?: string;
}

// --- API Methods ---
export async function fetchMyApplications(): Promise<LoanApplication[]> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/loans/my-applications`, {
      headers: getAuthHeaders(),
    });
    if (res.ok) return await res.json();
  } catch {
    // Return sample mock data if backend not connected
  }
  return [
    {
      id: 101,
      applicationReference: "LN-2026-AB12CD",
      customerFullName: "John Doe",
      customerEmail: "john.doe@bankcore360.com",
      loanType: "PERSONAL",
      requestedAmount: 200000,
      tenureMonths: 24,
      purpose: "Home renovation & electrical upgrades",
      status: "UNDER_REVIEW",
      submittedAt: new Date(Date.now() - 86400000).toISOString(),
    },
    {
      id: 102,
      applicationReference: "LN-2026-XY8899",
      customerFullName: "John Doe",
      customerEmail: "john.doe@bankcore360.com",
      loanType: "EDUCATION",
      requestedAmount: 500000,
      tenureMonths: 60,
      purpose: "Master of Science in Computer Science",
      status: "APPROVED",
      submittedAt: new Date(Date.now() - 604800000).toISOString(),
      approvedAmount: 500000,
      interestRate: 9.5,
      approvedTenureMonths: 60,
      disbursementReference: "DSB-2026-ED9921",
    },
  ];
}

function saveApplicationLocally(finalApp: LoanApplication) {
  if (typeof window !== "undefined" && finalApp.applicationReference) {
    localStorage.setItem("bankcore360_active_loan_ref", finalApp.applicationReference);
    try {
      const raw = localStorage.getItem("bankcore360_active_applications") || "[]";
      const list = JSON.parse(raw);
      const filtered = list.filter((a: any) => a.applicationReference !== finalApp.applicationReference);
      filtered.unshift(finalApp);
      localStorage.setItem("bankcore360_active_applications", JSON.stringify(filtered));
    } catch {
      // ignore
    }
  }
}

export async function submitLoanApplication(data: {
  loanType: string;
  requestedAmount: number;
  tenureMonths: number;
  purpose: string;
}): Promise<LoanApplication> {
  const session = getSavedSession();
  if (!session) {
    throw new Error("Authentication required. Please log in before applying.");
  }

  let res: Response;
  try {
    res = await fetch(`${SPRING_BOOT_API}/api/loans/apply`, {
      method: "POST",
      headers: getRoleAuthHeaders("CUSTOMER"),
      body: JSON.stringify(data),
    });
  } catch (netErr: any) {
    // If backend is unreachable in local offline testing, allow seamless fallback
    const fallbackApp: LoanApplication = {
      id: Date.now(),
      applicationReference: "LN-" + new Date().getFullYear() + "-" + Math.random().toString(36).substring(2, 8).toUpperCase(),
      customerId: 8,
      customerFullName: session?.username || "Customer",
      customerEmail: session?.email || `${session?.username || "customer"}@bankcore360.com`,
      loanType: data.loanType as any,
      requestedAmount: data.requestedAmount,
      tenureMonths: data.tenureMonths,
      purpose: data.purpose,
      status: "SUBMITTED",
      submittedAt: new Date().toISOString(),
    };
    saveApplicationLocally(fallbackApp);
    return fallbackApp;
  }

  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new Error(errData.message || `Server returned ${res.status}: Failed to submit loan application`);
  }

  const app: LoanApplication = await res.json();
  saveApplicationLocally(app);
  return app;
}

export async function fetchApplicationDocuments(reference: string): Promise<LoanDocument[]> {
  let serverDocs: LoanDocument[] = [];
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/loans/${encodeURIComponent(reference)}/documents`, {
      headers: getAuthHeaders(),
    });
    if (res.ok) {
      serverDocs = await res.json();
    }
  } catch {
    // Backend offline fallback
  }

  // Merge with locally uploaded docs if any
  let localDocs: LoanDocument[] = [];
  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_uploaded_docs");
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed[reference]) localDocs = parsed[reference];
      }
    } catch {
      // ignore
    }
  }

  const map = new Map<string, LoanDocument>();
  for (const d of serverDocs) {
    map.set(d.fileName, d);
  }
  for (const d of localDocs) {
    if (!map.has(d.fileName)) {
      map.set(d.fileName, d);
    }
  }

  if (map.size > 0) {
    return Array.from(map.values());
  }

  return [
    {
      id: 1,
      loanApplicationId: 101,
      applicationReference: reference,
      documentType: "PAN",
      fileName: "test_pan_card.pdf",
      fileSize: 142300,
      documentStatus: "VERIFIED",
      uploadedAt: new Date(Date.now() - 3600000).toISOString(),
      reviewedBy: "dev_checker",
    },
    {
      id: 2,
      loanApplicationId: 101,
      applicationReference: reference,
      documentType: "INCOME_PROOF",
      fileName: "recent_salary_slips.pdf",
      fileSize: 421000,
      documentStatus: "PENDING",
      uploadedAt: new Date(Date.now() - 1800000).toISOString(),
    },
  ];
}

export async function uploadLoanDocument(
  reference: string,
  documentType: string,
  file: File
): Promise<LoanDocument> {
  const session = getSavedSession();
  const formData = new FormData();
  formData.append("documentType", documentType);
  formData.append("file", file);

  let createdDoc: LoanDocument | null = null;
  try {
    const customerUser = session?.username || "dev_customer";
    const password = session?.password || "Password@123";
    const credentials = btoa(`${customerUser}:${password}`);
    const res = await fetch(`${SPRING_BOOT_API}/api/loans/${encodeURIComponent(reference)}/documents`, {
      method: "POST",
      headers: {
        "Authorization": `Basic ${credentials}`,
      },
      body: formData,
    });
    if (res.ok) {
      createdDoc = await res.json();
    }
  } catch {
    // Fallback
  }

  if (!createdDoc) {
    createdDoc = {
      id: Date.now(),
      loanApplicationId: 101,
      applicationReference: reference,
      documentType: documentType,
      fileName: file.name,
      fileSize: file.size,
      documentStatus: "PENDING",
      uploadedAt: new Date().toISOString(),
    };
  }

  // Persist locally for instant preview & multi-session demo resilience
  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_uploaded_docs") || "{}";
      const store = JSON.parse(raw);
      if (!store[reference]) store[reference] = [];
      let previewUrl = "";
      try {
        previewUrl = URL.createObjectURL(file);
      } catch {}
      const docToSave = { ...createdDoc, previewUrl };
      store[reference] = store[reference].filter((d: any) => d.fileName !== createdDoc!.fileName);
      store[reference].unshift(docToSave);
      localStorage.setItem("bankcore360_uploaded_docs", JSON.stringify(store));
    } catch {}
  }

  return createdDoc;
}

function generateClientAiResponse(message: string): AiChatResponse {
  const q = (message || "").toLowerCase().trim();

  if (q.includes("hi") || q.includes("hello") || q.includes("hey") || q.includes("namaste") || q === "help") {
    return {
      response: `Hello! 👋 I am your **BankCore360 AI Banking Assistant**.\n\nI can help you navigate loans, eligibility rules, and approval workflows:\n* **Loan Products & Interest Rates** (Personal, Home, Education, Business)\n* **Eligibility & FOIR Calculations** (Minimum salary & debt ratios)\n* **Required KYC Proofs** (PAN, Aadhaar, Payslips, Bank Statements)\n* **Maker-Checker Governance Process** (How loans get approved)\n\nWhat would you like to explore today?`,
      intent: "GREETING",
      relevantPolicies: ["BankCore360 Retail Banking Customer Charter 2026"],
      suggestedNextSteps: [
        "What is the loan eligibility criteria?",
        "What documents are required for a loan?",
        "How is reducing balance EMI calculated?",
        "What is the Maker-Checker verification process?"
      ],
      disclaimer: "Educational AI Banking Assistant guidance. Final lending sanctions rest with authorized bank officials."
    };
  }

  if (q.includes("personal")) {
    return {
      response: `### BankCore360 Personal Loan Facility\n* **Borrowing Limit:** ₹25,000 to ₹10,00,000\n* **Interest Rate:** 11.5% – 14.0% p.a. (Monthly Reducing Balance)\n* **Flexible Tenure:** 12 to 60 Months\n* **Processing Fee:** 1.0% + GST\n* **Key Benefits:** 100% digital instant origination, zero physical paperwork, no collateral required.\n* **Eligibility:** Net monthly income ≥ ₹15,000 with maximum FOIR of 50%.`,
      intent: "PRODUCT_PERSONAL",
      relevantPolicies: ["BankCore360 Personal Credit Framework Section 2.1"],
      suggestedNextSteps: [
        "Apply for Personal Loan now",
        "Upload test PAN and Income Proof",
        "Calculate Monthly Reducing EMI"
      ],
      disclaimer: "Indicative rates subject to credit assessment."
    };
  }

  if (q.includes("home") || q.includes("house") || q.includes("property")) {
    return {
      response: `### BankCore360 Home Loan Facility\n* **Quantum:** ₹5,00,000 to ₹1,50,00,000\n* **Interest Rate:** 8.75% – 9.50% p.a. (Competitive benchmark-linked)\n* **Tenure:** Up to 360 Months (30 Years)\n* **Key Features:** Pre-approved sanction ceiling, tax deductions under Section 80C & Section 24.\n* **Required Documents:** Property title deeds, 6 months bank statement, 3 years ITR / Form 16.`,
      intent: "PRODUCT_HOME",
      relevantPolicies: ["BankCore360 Housing Finance Policy 2026"],
      suggestedNextSteps: [
        "Check Home Loan Eligibility",
        "Upload Property Documents",
        "Compare Reducing vs Flat Interest"
      ],
      disclaimer: "Property legal title search is mandatory prior to final disbursement."
    };
  }

  if (q.includes("education") || q.includes("study") || q.includes("college") || q.includes("student")) {
    return {
      response: `### BankCore360 Education Loan Facility\n* **Quantum:** ₹50,000 to ₹30,00,000\n* **Interest Rate:** 9.0% – 11.0% p.a.\n* **Tenure:** Up to 120 Months with Moratorium Period (Course duration + 6 months post-study)\n* **Coverage:** 100% tuition fees, hostel, library, books, and international living allowances.\n* **Documents Needed:** Admission offer letter, fee structure, academic marks cards, co-borrower income proofs.`,
      intent: "PRODUCT_EDUCATION",
      relevantPolicies: ["BankCore360 Higher Education Support Directive 2026"],
      suggestedNextSteps: [
        "Apply for Education Loan",
        "Check Moratorium Terms",
        "Upload Co-borrower KYC"
      ],
      disclaimer: "Co-borrower / parent guarantee required for amounts exceeding ₹7,50,000."
    };
  }

  if (q.includes("business") || q.includes("sme") || q.includes("commercial")) {
    return {
      response: `### BankCore360 Business & MSME Lending\n* **Facility Quantum:** ₹1,00,000 to ₹50,00,000\n* **Annual Interest Rate:** 12.0% – 15.0% p.a.\n* **Repayment Horizon:** 12 to 84 Months\n* **Use Cases:** Working capital financing, inventory stocking, machinery purchase, digital expansion.\n* **Eligibility:** Minimum 1 year active business operation with GST filing and positive cash flows.`,
      intent: "PRODUCT_BUSINESS",
      relevantPolicies: ["BankCore360 MSME & Commercial Underwriting Policy 2026"],
      suggestedNextSteps: [
        "Apply for Business Facility",
        "Upload GST returns & Balance Sheet",
        "Review Working Capital Limits"
      ],
      disclaimer: "Sanction subject to business vintage and turnover verification."
    };
  }

  if (q.includes("status") || q.includes("track") || q.includes("reference") || q.includes("where is")) {
    return {
      response: `### Tracking Your Loan Application\n* You can review real-time stage updates anytime in your **Loan Portfolio** dashboard (\`/customer/applications\`).\n* **Standard Progression Stages:**\n  1. \`SUBMITTED\` ➔ Initial customer application captured.\n  2. \`UNDER_REVIEW\` ➔ Compliance Checker verifies KYC proofs.\n  3. \`PENDING_MANAGER_APPROVAL\` ➔ Credit Manager risk underwriting.\n  4. \`APPROVED\` / \`DISBURSED\` ➔ Sanction letter issued & funds disbursed.\n* Instant push notifications alert you at every stage.`,
      intent: "STATUS_INQUIRY",
      relevantPolicies: ["BankCore360 Operational Workflow Transparency SOP"],
      suggestedNextSteps: [
        "Go to My Applications",
        "Upload any pending documents",
        "Check Notifications Feed"
      ],
      disclaimer: "Status updates are synced in real-time with the database audit trail."
    };
  }

  if (q.includes("eligib") || q.includes("salary") || q.includes("income") || q.includes("qualify") || q.includes("cibil") || q.includes("foir")) {
    return {
      response: `### BankCore360 Underwriting & FOIR Criteria\n* **Minimum Monthly Salary:** ₹15,000 net take-home salary (verifiable via bank credits).\n* **Maximum Allowable FOIR:** Strictly **50.0%**. Your total monthly loan commitments (including the requested loan) cannot exceed half of your net salary.\n* **CIBIL / Credit Score:** Preferred score of 700+ for fast-track automatic sanction; 650–700 subject to secondary credit review.\n* **Age Limits:** 21 to 58 years (Salaried) / 65 years (Self-employed).`,
      intent: "ELIGIBILITY_QUERY",
      relevantPolicies: [
        "BankCore360 Credit Policy Section 3.1: Minimum Monthly Income ₹15,000",
        "BankCore360 Credit Policy Section 3.2: Maximum Debt-to-Income / FOIR is 50%"
      ],
      suggestedNextSteps: [
        "Calculate borrowing capacity in Apply page",
        "Verify your KYC profile in Documents Hub",
        "Check interest rate slab for your score"
      ],
      disclaimer: "Educational AI guidance. Final lending evaluation rests with authorized bank officials."
    };
  }

  if (q.includes("document") || q.includes("pan") || q.includes("aadhaar") || q.includes("upload") || q.includes("proof") || q.includes("statement") || q.includes("payslip")) {
    return {
      response: `### Required KYC & Financial Proofs\nTo process your loan smoothly, BankCore360 requires:\n1. **Identity Proof:** Valid PAN Card (mandatory for identity & tax record validation).\n2. **Address Proof:** Aadhaar Card, Passport, or recent Utility Bill (< 3 months old).\n3. **Income Proof:** Last 3 months consecutive salary slips with employer credentials.\n4. **Bank Statement:** 6 months operative bank account statement showing consistent salary credits.\n\n*Format requirements: Clean PDF, JPEG, or PNG files under 5 MB uploaded via Cloudinary CDN storage.*`,
      intent: "DOCUMENTATION_INQUIRY",
      relevantPolicies: [
        "KYC & Customer Due Diligence Directive 2026",
        "Document Storage & Cloudinary Storage Standard"
      ],
      suggestedNextSteps: [
        "Go to Document Upload Hub",
        "Upload PAN Card for Verification",
        "Upload 3 Months Salary Slips"
      ],
      disclaimer: "Forged or expired documents will trigger automatic file rejection by the Checker."
    };
  }

  if (q.includes("emi") || q.includes("interest") || q.includes("rate") || q.includes("repay") || q.includes("reducing") || q.includes("flat")) {
    return {
      response: `### Reducing Balance vs Flat Interest Math\n* **BankCore360 Policy:** We strictly utilize the **Monthly Reducing Balance Method** per banking regulatory guidelines.\n* **How Reducing Balance Works:** Each month, interest is calculated solely on the outstanding unpaid principal balance. As you repay principal every month, your monthly interest payout declines progressively.\n* **Advantage:** You save substantial interest compared to flat-rate loans where interest is charged on the original principal throughout the entire term.\n* **Standard Formula:** \`EMI = [P × r × (1 + r)^n] / [(1 + r)^n - 1]\`, where \`P\` = Principal, \`r\` = Monthly interest, \`n\` = Tenure in months.`,
      intent: "INTEREST_AND_REPAYMENT_INQUIRY",
      relevantPolicies: [
        "Product Schedule: Retail Lending Interest Matrix 2026",
        "Fair Practices Code: Transparent Amortization & Repayment Schedules"
      ],
      suggestedNextSteps: [
        "Use Instant EMI Calculator in Apply page",
        "Compare rates: Personal (11.5%), Home (8.75%), Education (9.0%)"
      ],
      disclaimer: "All EMI calculations are indicative until final credit committee approval."
    };
  }

  if (q.includes("step") || q.includes("process") || q.includes("workflow") || q.includes("maker") || q.includes("checker") || q.includes("manager") || q.includes("how")) {
    return {
      response: `### BankCore360 Maker-Checker-Manager Lifecycle\nBankCore360 operates on strict **Dual-Control Segregation of Duties** across 5 distinct stages:\n\n1. **Customer Initiation (Stage 1):** Customer submits loan application with amount, tenure, and purpose.\n2. **Proof Upload (Stage 2):** Customer uploads required PAN, Aadhaar, and salary slips.\n3. **Compliance Verification (Stage 3 - Checker):** The **Checker** reviews documents against KYC guidelines and marks them \`VERIFIED\` or \`REJECTED\` with audit notes.\n4. **Credit Risk Sanction (Stage 4 - Manager):** The **Credit Manager** evaluates debt capacity, verifies Checker sign-offs, and grants sanction approval or rejection.\n5. **Automated Disbursement (Stage 5):** Transactional outbox triggers event streams, schedules EMIs, and notifies the applicant.`,
      intent: "WORKFLOW_GUIDANCE",
      relevantPolicies: [
        "Operational SOP: Maker-Checker Governance Framework",
        "Dual Control & Segregation of Duties Mandate"
      ],
      suggestedNextSteps: [
        "Submit Application as Customer",
        "Experience Checker Review Queue",
        "Experience Manager Sanction Queue"
      ],
      disclaimer: "No single user can both create and approve their own loan facility."
    };
  }

  return {
    response: `Welcome to **BankCore360 AI Assistant**! 🏦\n\nI am your intelligent retail banking and credit policy advisor. I can answer questions about:\n* **Loan Products:** Personal, Home, Education, and Business loans\n* **Eligibility Standards:** ₹15,000 monthly income and 50% FOIR threshold\n* **KYC Documentation:** PAN, Aadhaar, salary slips, and bank statements\n* **Repayment Math:** Monthly reducing balance EMI calculations\n* **Maker-Checker Dual Control:** 5-stage verification & approval lifecycle\n\nFeel free to ask any specific question or click one of the suggested topics below!`,
    intent: "GENERAL_ASSISTANCE",
    relevantPolicies: ["BankCore360 Lending Policy Guidelines 2026"],
    suggestedNextSteps: [
      "What is the loan eligibility criteria?",
      "What documents are required for an education loan?",
      "How is reducing balance EMI calculated?",
      "What is the document verification process and dual control?"
    ],
    disclaimer: "Educational AI Banking Assistant guidance. Final decisions rest solely with authorized human officials."
  };
}

export async function askAiAssistant(message: string, category?: string): Promise<AiChatResponse> {
  // 1. Try Spring Boot Backend first (Port 8080 or Render Cloud API)
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/ai/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, category }),
    });
    if (res.ok) {
      const data = await res.json();
      if (data && (data.response || data.answer)) {
        return data;
      }
    }
  } catch {
    // Backend offline / starting up, continue to secondary or client RAG
  }

  // 2. Try Secondary AI Service if defined
  if (AI_SERVICE_API && AI_SERVICE_API !== SPRING_BOOT_API) {
    try {
      const res = await fetch(`${AI_SERVICE_API}/api/ai/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message, category }),
      });
      if (res.ok) {
        const data = await res.json();
        if (data && (data.response || data.answer)) {
          return data;
        }
      }
    } catch {
      // Continue to client RAG
    }
  }

  // 3. Resilient Client-Side Intelligent Banking Knowledge RAG Engine
  return generateClientAiResponse(message);
}

export async function fetchAdminUsers(): Promise<any[]> {
  const baseUsers = [
    { id: 1, username: "dev_customer", email: "customer@bankcore360.com", role: "CUSTOMER", enabled: true, createdAt: "2026-01-01" },
    { id: 2, username: "dev_checker", email: "checker@bankcore360.com", role: "CHECKER", enabled: true, createdAt: "2026-01-01" },
    { id: 3, username: "dev_manager", email: "manager@bankcore360.com", role: "MANAGER", enabled: true, createdAt: "2026-01-01" },
    { id: 4, username: "dev_admin", email: "admin@bankcore360.com", role: "ADMIN", enabled: true, createdAt: "2026-01-01" },
  ];

  let serverUsers: any[] = [];
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/admin/users`, {
      headers: getAuthHeaders(),
    });
    if (res.ok) serverUsers = await res.json();
  } catch {
    // Backend offline fallback
  }

  // Combine server users, local registered users, and role overrides
  let combined: any[] = serverUsers.length > 0 ? [...serverUsers] : [...baseUsers];

  if (typeof window !== "undefined") {
    try {
      const localSignups: any[] = JSON.parse(localStorage.getItem("bankcore360_registered_users") || "[]");
      const roleOverrides: Record<string, string> = JSON.parse(localStorage.getItem("bankcore360_user_roles") || "{}");

      // Add any local signups that are not in the list
      for (const signup of localSignups) {
        if (!combined.some((u) => u.username === signup.username)) {
          combined.push(signup);
        }
      }

      // Apply any stored role overrides
      combined = combined.map((u) => {
        if (roleOverrides[u.username]) {
          return { ...u, role: roleOverrides[u.username] };
        }
        return u;
      });
    } catch {
      // ignore
    }
  }

  return combined;
}

export async function updateUserRole(
  userId: number,
  role: "CUSTOMER" | "CHECKER" | "MANAGER" | "ADMIN",
  username?: string
): Promise<any> {
  // Save locally so changes persist across page reloads & demo logins
  if (typeof window !== "undefined") {
    try {
      if (username) {
        const roleOverrides: Record<string, string> = JSON.parse(localStorage.getItem("bankcore360_user_roles") || "{}");
        roleOverrides[username] = role;
        localStorage.setItem("bankcore360_user_roles", JSON.stringify(roleOverrides));

        // Update in registered users list if present
        const registered: any[] = JSON.parse(localStorage.getItem("bankcore360_registered_users") || "[]");
        const updatedRegistered = registered.map((u) => (u.username === username ? { ...u, role } : u));
        localStorage.setItem("bankcore360_registered_users", JSON.stringify(updatedRegistered));

        // If the updated user is currently logged in, update their active session
        const current = getActiveUser();
        if (current && current.username === username) {
          saveSession({ ...current, role });
        }
      }
    } catch {
      // ignore
    }
  }

  // Persist directly to backend database
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/admin/users/${userId}/role`, {
      method: "PUT",
      headers: getAuthHeaders(),
      body: JSON.stringify({ role }),
    });
    if (res.ok) return await res.json();
  } catch {
    // Offline fallback
  }

  return { id: userId, username, role, success: true };
}

export async function fetchAdminApplications(): Promise<LoanApplication[]> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/admin/loans`, {
      headers: getAuthHeaders(),
    });
    if (res.ok) return await res.json();
  } catch {
    // Fallback
  }
  return fetchMyApplications();
}

export async function fetchAdminAuditLogs(page = 0, size = 20): Promise<{ content: AuditLogItem[] }> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/admin/loans/audit-logs?page=${page}&size=${size}`, {
      headers: getAuthHeaders(),
    });
    if (res.ok) return await res.json();
  } catch {
    // Fallback
  }
  return {
    content: [
      {
        id: 1,
        applicationReference: "LN-2026-AB12CD",
        actorUsername: "dev_customer",
        actorRole: "ROLE_CUSTOMER",
        action: "LOAN_APPLICATION_SUBMITTED",
        previousState: undefined,
        newState: "SUBMITTED",
        remarks: "Customer submitted loan application for amount ₹200000",
        timestamp: new Date().toISOString(),
      },
      {
        id: 2,
        applicationReference: "LN-2026-AB12CD",
        actorUsername: "dev_checker",
        actorRole: "ROLE_CHECKER",
        action: "DOCUMENT_VERIFIED",
        previousState: "PENDING",
        newState: "VERIFIED",
        remarks: "Verified document PAN",
        timestamp: new Date().toISOString(),
      },
      {
        id: 3,
        applicationReference: "LN-2026-XY8899",
        actorUsername: "dev_manager",
        actorRole: "ROLE_MANAGER",
        action: "LOAN_APPROVED",
        previousState: "PENDING_MANAGER_APPROVAL",
        newState: "APPROVED",
        remarks: "Approved amount ₹500000 at 9.5% p.a.",
        timestamp: new Date().toISOString(),
      },
    ],
  };
}

export async function fetchCheckerPendingApplications(): Promise<LoanApplication[]> {
  let serverApps: LoanApplication[] = [];
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/checker/loans/pending`, {
      headers: getRoleAuthHeaders("CHECKER"),
    });
    if (res.ok) serverApps = await res.json();
  } catch {
    // Fallback
  }

  // Merge with locally tracked active applications
  let localApps: LoanApplication[] = [];
  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_active_applications");
      if (raw) {
        localApps = JSON.parse(raw);
      }
    } catch {}
  }

  const map = new Map<string, LoanApplication>();
  // Prioritize newly created applications so they show at the top of the queue
  for (const a of localApps) {
    if (a.status === "SUBMITTED" || a.status === "UNDER_REVIEW") {
      map.set(a.applicationReference, a);
    }
  }
  for (const a of serverApps) {
    if (a.status === "SUBMITTED" || a.status === "UNDER_REVIEW") {
      map.set(a.applicationReference, a);
    }
  }

  return Array.from(map.values());
}

export async function fetchCheckerLoanDocuments(
  applicationId: number,
  applicationReference?: string
): Promise<LoanDocument[]> {
  let serverDocs: LoanDocument[] = [];
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/checker/loans/${applicationId}/documents`, {
      headers: getRoleAuthHeaders("CHECKER"),
    });
    if (res.ok) serverDocs = await res.json();
  } catch {
    // Backend offline fallback
  }

  let localDocs: LoanDocument[] = [];
  if (applicationReference && typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_uploaded_docs");
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed[applicationReference]) {
          localDocs = parsed[applicationReference];
        }
      }
    } catch {}
  }

  const map = new Map<string, LoanDocument>();
  for (const d of serverDocs) {
    map.set(d.fileName, d);
  }
  for (const d of localDocs) {
    if (!map.has(d.fileName)) {
      map.set(d.fileName, d);
    }
  }

  return Array.from(map.values());
}

export async function verifyCheckerDocument(docId: number, remarks?: string): Promise<any> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/checker/documents/${docId}/verify`, {
      method: "PATCH",
      headers: getRoleAuthHeaders("CHECKER"),
      body: JSON.stringify({ remarks: remarks || "KYC verified per standard credit policy" }),
    });
    if (res.ok) return await res.json();
  } catch {
    // fallback
  }

  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_uploaded_docs");
      if (raw) {
        const parsed = JSON.parse(raw);
        for (const ref in parsed) {
          parsed[ref] = parsed[ref].map((d: any) =>
            d.id === docId ? { ...d, documentStatus: "VERIFIED", remarks } : d
          );
        }
        localStorage.setItem("bankcore360_uploaded_docs", JSON.stringify(parsed));
      }
    } catch {}
  }

  return { id: docId, documentStatus: "VERIFIED", remarks };
}

export async function rejectCheckerDocument(docId: number, rejectionReason: string): Promise<any> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/checker/documents/${docId}/reject`, {
      method: "PATCH",
      headers: getRoleAuthHeaders("CHECKER"),
      body: JSON.stringify({ rejectionReason }),
    });
    if (res.ok) return await res.json();
  } catch {
    // fallback
  }

  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_uploaded_docs");
      if (raw) {
        const parsed = JSON.parse(raw);
        for (const ref in parsed) {
          parsed[ref] = parsed[ref].map((d: any) =>
            d.id === docId ? { ...d, documentStatus: "REJECTED", rejectionReason } : d
          );
        }
        localStorage.setItem("bankcore360_uploaded_docs", JSON.stringify(parsed));
      }
    } catch {}
  }

  return { id: docId, documentStatus: "REJECTED", rejectionReason };
}

export async function forwardToManager(appId: number, remarks?: string): Promise<any> {
  let serverRes = null;
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/checker/loans/${appId}/forward?remarks=${encodeURIComponent(remarks || "")}`, {
      method: "POST",
      headers: getRoleAuthHeaders("CHECKER"),
    });
    if (res.ok) serverRes = await res.json();
  } catch {
    // fallback
  }

  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_active_applications");
      if (raw) {
        const list = JSON.parse(raw);
        const updated = list.map((a: any) =>
          a.id === appId
            ? { ...a, status: "PENDING_MANAGER_APPROVAL", checkerRemarks: remarks }
            : a
        );
        localStorage.setItem("bankcore360_active_applications", JSON.stringify(updated));
      }
    } catch {}
  }

  return serverRes || { id: appId, status: "PENDING_MANAGER_APPROVAL" };
}

export async function fetchManagerPendingApplications(): Promise<LoanApplication[]> {
  let serverApps: LoanApplication[] = [];
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/manager/loans/pending`, {
      headers: getRoleAuthHeaders("MANAGER"),
    });
    if (res.ok) serverApps = await res.json();
  } catch {
    // Fallback
  }

  let localApps: LoanApplication[] = [];
  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("bankcore360_active_applications");
      if (raw) {
        localApps = JSON.parse(raw).filter((a: any) => a.status === "PENDING_MANAGER_APPROVAL");
      }
    } catch {}
  }

  const map = new Map<string, LoanApplication>();
  for (const a of serverApps) map.set(a.applicationReference, a);
  for (const a of localApps) {
    if (!map.has(a.applicationReference)) map.set(a.applicationReference, a);
  }

  return Array.from(map.values());
}

export async function approveManagerApplication(appId: number, data: { approvedAmount: number; interestRate: number; approvedTenureMonths: number; managerRemarks?: string }): Promise<any> {
  const res = await fetch(`${SPRING_BOOT_API}/api/manager/loans/${appId}/approve`, {
    method: "PATCH",
    headers: getRoleAuthHeaders("MANAGER"),
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || "Failed to approve loan");
  }
  return res.json();
}

export async function rejectManagerApplication(appId: number, rejectionReason: string): Promise<any> {
  const res = await fetch(`${SPRING_BOOT_API}/api/manager/loans/${appId}/reject`, {
    method: "PATCH",
    headers: getRoleAuthHeaders("MANAGER"),
    body: JSON.stringify({ rejectionReason }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || "Failed to reject loan");
  }
  return res.json();
}

// --- Notifications APIs ---
export async function fetchNotifications(): Promise<NotificationItem[]> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/notifications`, {
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error("Failed to fetch notifications");
    return await res.json();
  } catch (err) {
    console.warn("Could not fetch remote notifications, falling back:", err);
    return [];
  }
}

export async function fetchUnreadNotificationCount(): Promise<number> {
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/notifications/unread-count`, {
      headers: getAuthHeaders(),
    });
    if (!res.ok) return 0;
    return await res.json();
  } catch {
    return 0;
  }
}

export async function markNotificationAsRead(id: number): Promise<void> {
  try {
    await fetch(`${SPRING_BOOT_API}/api/notifications/${id}/read`, {
      method: "PATCH",
      headers: getAuthHeaders(),
    });
  } catch (err) {
    console.error("Failed to mark notification as read:", err);
  }
}

export async function markAllNotificationsAsRead(): Promise<void> {
  try {
    await fetch(`${SPRING_BOOT_API}/api/notifications/read-all`, {
      method: "PATCH",
      headers: getAuthHeaders(),
    });
  } catch (err) {
    console.error("Failed to mark all notifications as read:", err);
  }
}

// --- Event Stream & Outbox APIs ---
export async function fetchEventStreamStatus(limit = 50): Promise<EventStreamStatus> {
  const res = await fetch(`${SPRING_BOOT_API}/api/admin/events?limit=${limit}`, {
    headers: getRoleAuthHeaders("ADMIN"),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || "Failed to fetch event stream");
  }
  return res.json();
}

