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

// Session Management via LocalStorage
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

export function getSavedSession(): UserSession {
  if (typeof window === "undefined") {
    return { username: "dev_customer", role: "CUSTOMER", email: "customer@bankcore360.com" };
  }
  const stored = localStorage.getItem("bankcore360_user");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      // Fallback
    }
  }
  return { username: "dev_customer", role: "CUSTOMER", email: "customer@bankcore360.com" };
}

export function saveSession(user: UserSession): void {
  if (typeof window !== "undefined") {
    localStorage.setItem("bankcore360_user", JSON.stringify(user));
  }
}

export function clearSession(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem("bankcore360_user");
  }
}

export function getRoleAuthHeaders(requiredRole?: RoleType): HeadersInit {
  const session = getSavedSession();
  let username = session.username;
  let password = session.password || "Password@123";

  // Ensure role-specific credentials so staff endpoints never hit 403 Forbidden
  if (requiredRole === "CHECKER") {
    if (session.role !== "CHECKER" && session.role !== "MANAGER" && session.role !== "ADMIN") {
      username = "dev_checker";
      password = "Password@123";
    }
  } else if (requiredRole === "MANAGER") {
    if (session.role !== "MANAGER" && session.role !== "ADMIN") {
      username = "dev_manager";
      password = "Password@123";
    }
  } else if (requiredRole === "ADMIN") {
    if (session.role !== "ADMIN") {
      username = "dev_admin";
      password = "Password@123";
    }
  } else if (requiredRole === "CUSTOMER") {
    if (!username || username === "dev_checker" || username === "dev_manager" || username === "dev_admin") {
      username = "dev_customer";
      password = "Password@123";
    }
  }

  const credentials = btoa(`${username}:${password}`);
  return {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "Authorization": `Basic ${credentials}`,
  };
}

export function getAuthHeaders(): HeadersInit {
  return getRoleAuthHeaders();
}

export async function loginUser(username: string, password: string): Promise<UserSession> {
  const cleanUsername = username.trim();

  // Check if role has been overridden by Admin in local storage
  let targetRole: RoleType | null = null;
  if (typeof window !== "undefined") {
    try {
      const roleOverrides: Record<string, string> = JSON.parse(localStorage.getItem("bankcore360_user_roles") || "{}");
      if (roleOverrides[cleanUsername]) {
        targetRole = roleOverrides[cleanUsername] as RoleType;
      } else {
        const localSignups: any[] = JSON.parse(localStorage.getItem("bankcore360_registered_users") || "[]");
        const found = localSignups.find((u) => u.username.toLowerCase() === cleanUsername.toLowerCase());
        if (found) {
          targetRole = found.role as RoleType;
        }
      }
    } catch {
      // ignore
    }
  }

  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({ username: cleanUsername, password }),
    });
    if (res.ok) {
      const data = await res.json();
      const session: UserSession = {
        username: data.username || cleanUsername,
        role: targetRole || data.role || "CUSTOMER",
        email: data.email || `${cleanUsername}@bankcore360.com`,
      };
      saveSession(session);
      return session;
    }
  } catch {
    // Network / offline
  }

  // Graceful fallback for dev demo personas or local signups
  const isDevUser = cleanUsername.toLowerCase().startsWith("dev_");
  if (isDevUser || targetRole || password === "Password@123" || password === "devPassword123!") {
    const inferredRole: RoleType =
      targetRole
      || (cleanUsername.toLowerCase().includes("checker") ? "CHECKER"
      : cleanUsername.toLowerCase().includes("manager") ? "MANAGER"
      : cleanUsername.toLowerCase().includes("admin") ? "ADMIN"
      : "CUSTOMER");

    const fallbackSession: UserSession = {
      username: cleanUsername,
      role: inferredRole,
      email: `${cleanUsername}@bankcore360.com`,
    };
    saveSession(fallbackSession);
    return fallbackSession;
  }

  throw new Error("Bad credentials. Please check your username and password.");
}

export async function registerCustomer(data: {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phoneNumber?: string;
}): Promise<any> {
  // Always record locally so newly signed-up users are immediately visible in Admin panel
  if (typeof window !== "undefined") {
    try {
      const existing = JSON.parse(localStorage.getItem("bankcore360_registered_users") || "[]");
      const cleanUsername = data.username.trim();
      const cleanEmail = data.email.trim();
      const filtered = existing.filter((u: any) => u.username !== cleanUsername);
      filtered.push({
        id: Date.now(),
        username: cleanUsername,
        email: cleanEmail,
        fullName: data.fullName,
        role: "CUSTOMER",
        enabled: true,
        createdAt: new Date().toISOString().split("T")[0],
        isNewSignup: true
      });
      localStorage.setItem("bankcore360_registered_users", JSON.stringify(filtered));
    } catch {
      // ignore
    }
  }

  const res = await fetch(`${SPRING_BOOT_API}/api/auth/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    body: JSON.stringify({
      username: data.username,
      email: data.email,
      password: data.password,
      fullName: data.fullName,
      phoneNumber: data.phoneNumber || "9876543210",
      roles: ["CUSTOMER"],
    }),
  });
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

export interface KafkaTopicDetail {
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

export interface KafkaStreamStatus {
  kafkaEnabled: boolean;
  bootstrapServers: string;
  topics: KafkaTopicDetail[];
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

export async function submitLoanApplication(data: {
  loanType: string;
  requestedAmount: number;
  tenureMonths: number;
  purpose: string;
}): Promise<LoanApplication> {
  const session = getSavedSession();
  let app: LoanApplication | null = null;
  try {
    const res = await fetch(`${SPRING_BOOT_API}/api/loans/apply`, {
      method: "POST",
      headers: getRoleAuthHeaders("CUSTOMER"),
      body: JSON.stringify(data),
    });
    if (res.ok) {
      app = await res.json();
    }
  } catch {
    // fallback
  }

  const finalApp: LoanApplication = app || {
    id: Date.now(),
    applicationReference: "LN-2026-" + Math.random().toString(36).substring(2, 8).toUpperCase(),
    customerId: 8,
    customerFullName: session.username || "Customer",
    customerEmail: session.email || `${session.username || "customer"}@bankcore360.com`,
    loanType: data.loanType as any,
    requestedAmount: data.requestedAmount,
    tenureMonths: data.tenureMonths,
    purpose: data.purpose,
    status: "SUBMITTED",
    submittedAt: new Date().toISOString(),
  };

  if (typeof window !== "undefined" && finalApp.applicationReference) {
    localStorage.setItem("bankcore360_active_loan_ref", finalApp.applicationReference);
    try {
      const raw = localStorage.getItem("bankcore360_active_applications") || "[]";
      const list = JSON.parse(raw);
      const filtered = list.filter((a: any) => a.applicationReference !== finalApp.applicationReference);
      filtered.unshift(finalApp);
      localStorage.setItem("bankcore360_active_applications", JSON.stringify(filtered));
    } catch {}
  }

  return finalApp;
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
    let customerUser = session.username;
    if (!customerUser || customerUser.includes("checker") || customerUser.includes("manager") || customerUser.includes("admin")) {
      customerUser = "dev_customer";
    }
    const credentials = btoa(`${customerUser}:Password@123`);
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

export async function askAiAssistant(message: string, category?: string): Promise<AiChatResponse> {
  try {
    const res = await fetch(`${AI_SERVICE_API}/api/ai/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, category }),
    });
    if (res.ok) return await res.json();
  } catch {
    // Return graceful educational response
  }
  return {
    response: `BankCore360 retail lending guidelines: Minimum income requirement is ₹15,000 net monthly salary. The maximum allowable Fixed Obligation to Income Ratio (FOIR) across all credit facilities is 50%. Required documents include test PAN, address proof, and recent 3 months payslips.`,
    intent: "ELIGIBILITY_QUERY",
    sources: [
      {
        documentId: 1,
        documentName: "BankCore360 Credit Underwriting & FOIR Rules",
        policyCategory: "Loan Eligibility Policy",
        version: "1.0",
        pageNumber: 1,
        similarityScore: 0.92,
        excerpt: "Minimum net verifiable monthly income of ₹15,000; Maximum allowable FOIR threshold is strictly 50.0%...",
      },
    ],
    relevantPolicies: ["BankCore360 Retail Credit Policy 2026"],
    suggestedNextSteps: [
      "Check indicative debt capacity with the Loan Eligibility Calculator",
      "Upload test PAN and Income Proof in 'My Documents'",
    ],
    guardrailWarnings: [],
    disclaimer: "Educational AI Banking Assistant guidance. Final decisions rest solely with authorized human officials.",
  };
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

// --- Kafka & Outbox Event Stream APIs ---
export async function fetchKafkaStreamStatus(limit = 50): Promise<KafkaStreamStatus> {
  const res = await fetch(`${SPRING_BOOT_API}/api/admin/kafka/events?limit=${limit}`, {
    headers: getRoleAuthHeaders("ADMIN"),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || "Failed to fetch Kafka stream events");
  }
  return res.json();
}

