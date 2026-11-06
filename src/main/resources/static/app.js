// BankCore360 Loan Platform Frontend State & Controller
let currentUser = {
    username: "dev_customer",
    password: "devPassword123!",
    role: "CUSTOMER",
    token: btoa("dev_customer:devPassword123!")
};

let currentApplications = [];
let selectedApplication = null;

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
    initTabs();
    initCalculator();
    initQuickAuth();
    loadDashboardForCurrentRole();
    initAiChat();
    loadNotifications();
});

// Setup Basic Auth Headers
function getAuthHeaders() {
    return {
        "Authorization": "Basic " + currentUser.token,
        "Content-Type": "application/json"
    };
}

// Navigation Tabs
function initTabs() {
    const tabs = document.querySelectorAll(".tab-btn");
    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            tabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");

            document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
            const target = document.getElementById(tab.dataset.target);
            if (target) target.classList.add("active");

            // Role-specific auto refreshes
            if (tab.dataset.target === "tab-checker") loadCheckerQueue();
            if (tab.dataset.target === "tab-manager") loadManagerQueue();
            if (tab.dataset.target === "tab-admin") loadAdminAnalytics();
            if (tab.dataset.target === "tab-customer") loadMyApplications();
        });
    });
}

// Role Switcher / Quick Auth
function initQuickAuth() {
    document.querySelectorAll(".auth-switch-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            const user = btn.dataset.user;
            const role = btn.dataset.role;
            const pass = btn.dataset.pass || "devPassword123!";

            currentUser = {
                username: user,
                password: pass,
                role: role,
                token: btoa(`${user}:${pass}`)
            };

            document.getElementById("current-user-display").innerText = `${user} (${role})`;
            showNotification(`Switched role to ${role} (${user})`);
            loadDashboardForCurrentRole();
            loadNotifications();
        });
    });
}

function loadDashboardForCurrentRole() {
    // Switch to corresponding tab
    let tabTarget = "tab-customer";
    if (currentUser.role === "CHECKER") tabTarget = "tab-checker";
    if (currentUser.role === "MANAGER") tabTarget = "tab-manager";
    if (currentUser.role === "ADMIN") tabTarget = "tab-admin";

    const tabBtn = document.querySelector(`.tab-btn[data-target="${tabTarget}"]`);
    if (tabBtn) tabBtn.click();
}

// ----------------------------------------------------
// 1. LOAN ELIGIBILITY CALCULATOR
// ----------------------------------------------------
function initCalculator() {
    const incomeInput = document.getElementById("calc-income");
    const existingEmiInput = document.getElementById("calc-existing-emi");
    const amountInput = document.getElementById("calc-amount");
    const tenureInput = document.getElementById("calc-tenure");
    const rateInput = document.getElementById("calc-rate");

    const syncOutputs = () => {
        document.getElementById("calc-income-val").innerText = "₹" + Number(incomeInput.value).toLocaleString();
        document.getElementById("calc-amount-val").innerText = "₹" + Number(amountInput.value).toLocaleString();
        document.getElementById("calc-tenure-val").innerText = tenureInput.value + " Months";
        document.getElementById("calc-rate-val").innerText = rateInput.value + "% p.a.";
        calculateLoanEligibility();
    };

    [incomeInput, existingEmiInput, amountInput, tenureInput, rateInput].forEach(el => {
        if (el) el.addEventListener("input", syncOutputs);
    });

    syncOutputs();
}

async function calculateLoanEligibility() {
    const income = parseFloat(document.getElementById("calc-income").value);
    const existingEmi = parseFloat(document.getElementById("calc-existing-emi").value) || 0;
    const amount = parseFloat(document.getElementById("calc-amount").value);
    const tenure = parseInt(document.getElementById("calc-tenure").value);
    const rate = parseFloat(document.getElementById("calc-rate").value);

    try {
        const res = await fetch("/api/loans/calculator", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                monthlyIncome: income,
                existingMonthlyEmi: existingEmi,
                requestedAmount: amount,
                tenureMonths: tenure,
                annualInterestRate: rate
            })
        });

        if (res.ok) {
            const data = await res.json();
            document.getElementById("res-emi").innerText = "₹" + Number(data.estimatedMonthlyEmi).toLocaleString();
            document.getElementById("res-interest").innerText = "₹" + Number(data.totalInterest).toLocaleString();
            document.getElementById("res-total").innerText = "₹" + Number(data.totalRepayment).toLocaleString();
            document.getElementById("res-foir").innerText = data.monthlyEmiToIncomeRatio + "%";

            const statusEl = document.getElementById("res-status");
            if (data.eligible) {
                statusEl.className = "badge badge-green";
                statusEl.innerText = "INDICATIVE ELIGIBLE";
            } else {
                statusEl.className = "badge badge-amber";
                statusEl.innerText = "INDICATIVE INELIGIBLE";
            }

            document.getElementById("res-msg").innerText = data.statusMessage;
            document.getElementById("res-max-loan").innerText = "₹" + Number(data.maxEligibleLoanAmount).toLocaleString();
        }
    } catch (err) {
        console.error("Calculator error", err);
    }
}

// ----------------------------------------------------
// 2. CUSTOMER LOAN APPLICATION
// ----------------------------------------------------
async function submitLoanApplication(e) {
    e.preventDefault();
    const type = document.getElementById("app-type").value;
    const amount = parseFloat(document.getElementById("app-amount").value);
    const tenure = parseInt(document.getElementById("app-tenure").value);
    const purpose = document.getElementById("app-purpose").value;

    try {
        const res = await fetch("/api/loans/apply", {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                loanType: type,
                requestedAmount: amount,
                tenureMonths: tenure,
                purpose: purpose
            })
        });

        if (res.ok) {
            const data = await res.json();
            showNotification(`Loan application ${data.applicationReference} submitted successfully!`);
            document.getElementById("loan-apply-form").reset();
            loadMyApplications();
        } else {
            const err = await res.json();
            alert("Application failed: " + (err.message || "Invalid input"));
        }
    } catch (err) {
        alert("Error submitting application: " + err.message);
    }
}

async function loadMyApplications() {
    try {
        const res = await fetch("/api/loans/my-applications", { headers: getAuthHeaders() });
        if (res.ok) {
            currentApplications = await res.json();
            renderCustomerApplications(currentApplications);
            populateDocumentAppSelector(currentApplications);
        }
    } catch (err) {
        console.error("Failed loading customer applications", err);
    }
}

function renderCustomerApplications(apps) {
    const container = document.getElementById("customer-apps-table-body");
    if (!container) return;
    if (apps.length === 0) {
        container.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No loan applications yet. Submit your first application above!</td></tr>`;
        return;
    }

    container.innerHTML = apps.map(app => `
        <tr>
            <td><strong>${app.applicationReference}</strong></td>
            <td><span class="badge badge-purple">${app.loanType}</span></td>
            <td>₹${Number(app.requestedAmount).toLocaleString()}</td>
            <td>${app.tenureMonths}m</td>
            <td>${renderStatusBadge(app.status)}</td>
            <td>${new Date(app.submittedAt).toLocaleDateString()}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="inspectApplicationDetails('${app.applicationReference}')">Details</button>
                ${app.status === 'DISBURSED' ? `<button class="btn btn-primary btn-sm" onclick="viewRepayments('${app.applicationReference}')">EMI Schedule</button>` : ''}
            </td>
        </tr>
    `).join("");
}

function renderStatusBadge(status) {
    switch (status) {
        case "SUBMITTED": return `<span class="badge badge-blue">Submitted</span>`;
        case "UNDER_REVIEW": return `<span class="badge badge-amber">Under Review</span>`;
        case "DOCUMENTS_VERIFIED": return `<span class="badge badge-purple">Docs Verified</span>`;
        case "PENDING_MANAGER_APPROVAL": return `<span class="badge badge-amber">Pending Approval</span>`;
        case "APPROVED": return `<span class="badge badge-green">Approved</span>`;
        case "DISBURSED": return `<span class="badge badge-cyan" style="background:rgba(6,182,212,0.15);color:#22d3ee;border:1px solid #0891b2;">Disbursed</span>`;
        case "REJECTED": return `<span class="badge badge-red">Rejected</span>`;
        default: return `<span class="badge badge-blue">${status}</span>`;
    }
}

function populateDocumentAppSelector(apps) {
    const sel = document.getElementById("doc-app-select");
    if (!sel) return;
    sel.innerHTML = apps
        .filter(a => a.status !== 'REJECTED' && a.status !== 'DISBURSED')
        .map(a => `<option value="${a.applicationReference}">${a.applicationReference} (${a.loanType} - ₹${Number(a.requestedAmount).toLocaleString()})</option>`)
        .join("");
}

async function uploadLoanDocument(e) {
    e.preventDefault();
    const appRef = document.getElementById("doc-app-select").value;
    const docType = document.getElementById("doc-type-select").value;
    const fileInput = document.getElementById("doc-file-input");

    if (!appRef) {
        alert("Please select a loan application first.");
        return;
    }
    if (!fileInput.files || fileInput.files.length === 0) {
        alert("Please select a document file to upload.");
        return;
    }

    const formData = new FormData();
    formData.append("documentType", docType);
    formData.append("file", fileInput.files[0]);

    try {
        const res = await fetch(`/api/loans/${appRef}/documents`, {
            method: "POST",
            headers: {
                "Authorization": "Basic " + currentUser.token
            },
            body: formData
        });

        if (res.ok) {
            showNotification(`Document ${docType} uploaded successfully for ${appRef}`);
            fileInput.value = "";
            loadAppDocuments(appRef);
        } else {
            const err = await res.json();
            alert("Upload failed: " + (err.message || "Invalid document"));
        }
    } catch (err) {
        alert("Error uploading document: " + err.message);
    }
}

async function inspectApplicationDetails(reference) {
    try {
        const res = await fetch(`/api/loans/${reference}`, { headers: getAuthHeaders() });
        if (res.ok) {
            const app = await res.json();
            selectedApplication = app;
            document.getElementById("modal-app-ref").innerText = app.applicationReference;
            document.getElementById("modal-app-details").innerHTML = `
                <div style="display:grid; grid-template-columns:1fr 1fr; gap:0.75rem; margin-bottom:1rem;">
                    <div><strong>Customer:</strong> ${app.customerFullName} (${app.customerEmail})</div>
                    <div><strong>Status:</strong> ${renderStatusBadge(app.status)}</div>
                    <div><strong>Loan Type:</strong> ${app.loanType}</div>
                    <div><strong>Requested Amount:</strong> ₹${Number(app.requestedAmount).toLocaleString()}</div>
                    <div><strong>Tenure:</strong> ${app.tenureMonths} Months</div>
                    <div><strong>Submitted At:</strong> ${new Date(app.submittedAt).toLocaleString()}</div>
                </div>
                <div><strong>Purpose:</strong> ${app.purpose}</div>
                ${app.approvedAmount ? `
                    <div style="margin-top:1rem; padding:0.75rem; background:rgba(16,185,129,0.1); border:1px solid rgba(16,185,129,0.3); border-radius:6px;">
                        <strong>Approved Terms:</strong> ₹${Number(app.approvedAmount).toLocaleString()} at ${app.interestRate}% p.a. for ${app.approvedTenureMonths}m.
                        <br><strong>Manager Remarks:</strong> ${app.managerRemarks || 'None'}
                    </div>
                ` : ''}
                ${app.disbursementReference ? `
                    <div style="margin-top:0.5rem; padding:0.75rem; background:rgba(6,182,212,0.1); border:1px solid rgba(6,182,212,0.3); border-radius:6px;">
                        <strong>Disbursement Ref:</strong> ${app.disbursementReference} (Disbursed by ${app.disbursedBy})
                    </div>
                ` : ''}
                ${app.rejectionReason ? `
                    <div style="margin-top:0.5rem; padding:0.75rem; background:rgba(239,68,68,0.1); border:1px solid rgba(239,68,68,0.3); border-radius:6px;">
                        <strong>Rejection Reason:</strong> ${app.rejectionReason} (By ${app.rejectedBy})
                    </div>
                ` : ''}
            `;
            loadAppDocuments(reference);
            openModal("app-details-modal");
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadAppDocuments(reference) {
    try {
        const res = await fetch(`/api/loans/${reference}/documents`, { headers: getAuthHeaders() });
        if (res.ok) {
            const docs = await res.json();
            const container = document.getElementById("modal-docs-list");
            if (!container) return;
            if (docs.length === 0) {
                container.innerHTML = `<p style="color:var(--text-muted); font-size:0.85rem;">No documents uploaded yet.</p>`;
                return;
            }
            container.innerHTML = `
                <table style="margin-top:0.5rem;">
                    <thead><tr><th>Type</th><th>File</th><th>Status</th><th>Reviewer</th><th>Actions</th></tr></thead>
                    <tbody>
                        ${docs.map(d => `
                            <tr>
                                <td><strong>${d.documentType}</strong></td>
                                <td>${d.fileName} (${(d.fileSize / 1024).toFixed(1)} KB)</td>
                                <td>${renderDocStatus(d.documentStatus)}</td>
                                <td>${d.reviewedBy || '-'}</td>
                                <td>
                                    <a class="btn btn-secondary btn-sm" href="${d.downloadUrl}" target="_blank">Download</a>
                                </td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            `;
        }
    } catch (err) {
        console.error(err);
    }
}

function renderDocStatus(status) {
    switch (status) {
        case "VERIFIED": return `<span class="badge badge-green">Verified</span>`;
        case "REJECTED": return `<span class="badge badge-red">Rejected</span>`;
        default: return `<span class="badge badge-amber">Pending Review</span>`;
    }
}

// ----------------------------------------------------
// 3. CHECKER WORKFLOW
// ----------------------------------------------------
async function loadCheckerQueue() {
    try {
        const res = await fetch("/api/checker/loans/pending", { headers: getAuthHeaders() });
        if (res.ok) {
            const apps = await res.json();
            const container = document.getElementById("checker-queue-body");
            if (!container) return;
            if (apps.length === 0) {
                container.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No pending applications awaiting Checker verification.</td></tr>`;
                return;
            }
            container.innerHTML = apps.map(app => `
                <tr>
                    <td><strong>${app.applicationReference}</strong></td>
                    <td>${app.customerFullName}</td>
                    <td>${app.loanType}</td>
                    <td>₹${Number(app.requestedAmount).toLocaleString()} (${app.tenureMonths}m)</td>
                    <td>${renderStatusBadge(app.status)}</td>
                    <td>
                        <button class="btn btn-primary btn-sm" onclick="openCheckerReview(${app.id})">Inspect & Verify</button>
                    </td>
                </tr>
            `).join("");
        }
    } catch (err) {
        console.error("Failed loading checker queue", err);
    }
}

async function openCheckerReview(appId) {
    try {
        const res = await fetch(`/api/checker/loans/${appId}`, { headers: getAuthHeaders() });
        if (!res.ok) {
            const err = await res.json();
            alert("Error opening application: " + (err.message || "Unauthorized"));
            return;
        }
        const app = await res.json();
        selectedApplication = app;

        document.getElementById("checker-review-app-ref").innerText = app.applicationReference;
        document.getElementById("checker-review-summary").innerHTML = `
            <div><strong>Applicant:</strong> ${app.customerFullName} (${app.customerEmail})</div>
            <div><strong>Requested:</strong> ₹${Number(app.requestedAmount).toLocaleString()} | Tenure: ${app.tenureMonths}m | Type: ${app.loanType}</div>
            <div><strong>Purpose:</strong> ${app.purpose}</div>
            <div style="margin-top:0.5rem;"><strong>Status:</strong> ${renderStatusBadge(app.status)}</div>
        `;

        loadCheckerDocuments(appId);
        openModal("checker-review-modal");
    } catch (err) {
        alert("Error loading application: " + err.message);
    }
}

async function loadCheckerDocuments(appId) {
    try {
        const res = await fetch(`/api/checker/loans/${appId}/documents`, { headers: getAuthHeaders() });
        if (res.ok) {
            const docs = await res.json();
            const container = document.getElementById("checker-docs-container");
            if (!container) return;
            if (docs.length === 0) {
                container.innerHTML = `<p style="color:var(--text-amber);">No documents uploaded by applicant yet. Application cannot be forwarded without verified documents.</p>`;
                return;
            }
            container.innerHTML = docs.map(doc => `
                <div style="background:var(--bg-card); padding:0.85rem; border-radius:6px; margin-bottom:0.75rem; border:1px solid var(--border-color);">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <div>
                            <strong>${doc.documentType}</strong>: ${doc.fileName} (${(doc.fileSize / 1024).toFixed(1)} KB)
                            <div style="margin-top:0.25rem;">${renderDocStatus(doc.documentStatus)} ${doc.rejectionReason ? `<span style="color:var(--accent-red); font-size:0.8rem;">Reason: ${doc.rejectionReason}</span>` : ''}</div>
                        </div>
                        <div style="display:flex; gap:0.5rem;">
                            <a class="btn btn-secondary btn-sm" href="/api/checker/documents/${doc.id}/file" target="_blank">View File</a>
                            <button class="btn btn-success btn-sm" onclick="verifyCheckerDoc(${doc.id})">Verify</button>
                            <button class="btn btn-danger btn-sm" onclick="promptRejectDoc(${doc.id})">Reject</button>
                        </div>
                    </div>
                </div>
            `).join("");
        }
    } catch (err) {
        console.error(err);
    }
}

async function verifyCheckerDoc(docId) {
    try {
        const res = await fetch(`/api/checker/documents/${docId}/verify`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ remarks: "Document verified by Checker" })
        });
        if (res.ok) {
            showNotification("Document verified successfully!");
            if (selectedApplication) loadCheckerDocuments(selectedApplication.id);
        } else {
            const err = await res.json();
            alert("Verification failed: " + (err.message || "Error"));
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function promptRejectDoc(docId) {
    const reason = prompt("Enter mandatory rejection reason:");
    if (!reason || !reason.trim()) {
        alert("Rejection reason is required.");
        return;
    }

    try {
        const res = await fetch(`/api/checker/documents/${docId}/reject`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ rejectionReason: reason.trim() })
        });
        if (res.ok) {
            showNotification("Document rejected.");
            if (selectedApplication) loadCheckerDocuments(selectedApplication.id);
        } else {
            const err = await res.json();
            alert("Rejection failed: " + (err.message || "Error"));
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function forwardApplicationToManager() {
    if (!selectedApplication) return;
    try {
        const res = await fetch(`/api/checker/loans/${selectedApplication.id}/forward?remarks=All+mandatory+documents+verified+by+Checker`, {
            method: "POST",
            headers: getAuthHeaders()
        });

        if (res.ok) {
            showNotification(`Application ${selectedApplication.applicationReference} successfully forwarded to Manager!`);
            closeModal("checker-review-modal");
            loadCheckerQueue();
        } else {
            const err = await res.json();
            alert("Cannot forward: " + (err.message || "Error"));
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ----------------------------------------------------
// 4. MANAGER WORKFLOW
// ----------------------------------------------------
async function loadManagerQueue() {
    try {
        const res = await fetch("/api/manager/loans/pending", { headers: getAuthHeaders() });
        if (res.ok) {
            const apps = await res.json();
            const container = document.getElementById("manager-queue-body");
            if (!container) return;
            if (apps.length === 0) {
                container.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No applications pending Manager final approval.</td></tr>`;
                return;
            }
            container.innerHTML = apps.map(app => `
                <tr>
                    <td><strong>${app.applicationReference}</strong></td>
                    <td>${app.customerFullName}</td>
                    <td>${app.loanType}</td>
                    <td>₹${Number(app.requestedAmount).toLocaleString()} (${app.tenureMonths}m)</td>
                    <td>${renderStatusBadge(app.status)}</td>
                    <td>
                        <button class="btn btn-primary btn-sm" onclick="openManagerDecisionModal(${app.id})">Final Decision</button>
                    </td>
                </tr>
            `).join("");
        }
    } catch (err) {
        console.error("Failed loading manager queue", err);
    }
}

async function openManagerDecisionModal(appId) {
    try {
        const res = await fetch(`/api/manager/loans/${appId}`, { headers: getAuthHeaders() });
        if (!res.ok) {
            const err = await res.json();
            alert("Error: " + (err.message || "Unauthorized"));
            return;
        }
        const app = await res.json();
        selectedApplication = app;

        document.getElementById("mgr-app-ref").innerText = app.applicationReference;
        document.getElementById("mgr-applicant").innerText = `${app.customerFullName} (${app.customerEmail})`;
        document.getElementById("mgr-loan-info").innerText = `${app.loanType} | Requested: ₹${Number(app.requestedAmount).toLocaleString()} for ${app.tenureMonths} months`;

        // Pre-fill approval inputs
        document.getElementById("mgr-approve-amount").value = app.requestedAmount;
        document.getElementById("mgr-approve-tenure").value = app.tenureMonths;
        document.getElementById("mgr-approve-rate").value = app.loanType === 'HOME' ? 8.75 : 10.50;
        document.getElementById("mgr-approve-remarks").value = "Approved per credit policy and satisfactory KYC verification";

        openModal("manager-decision-modal");
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function submitManagerApproval() {
    if (!selectedApplication) return;
    const amount = parseFloat(document.getElementById("mgr-approve-amount").value);
    const rate = parseFloat(document.getElementById("mgr-approve-rate").value);
    const tenure = parseInt(document.getElementById("mgr-approve-tenure").value);
    const remarks = document.getElementById("mgr-approve-remarks").value;

    try {
        const res = await fetch(`/api/manager/loans/${selectedApplication.id}/approve`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                approvedAmount: amount,
                interestRate: rate,
                approvedTenureMonths: tenure,
                managerRemarks: remarks
            })
        });

        if (res.ok) {
            showNotification(`Loan ${selectedApplication.applicationReference} APPROVED for ₹${amount.toLocaleString()}!`);
            closeModal("manager-decision-modal");
            loadManagerQueue();
            promptDisbursement(selectedApplication.id, selectedApplication.applicationReference, amount);
        } else {
            const err = await res.json();
            alert("Approval failed: " + (err.message || "Invalid input"));
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function submitManagerRejection() {
    if (!selectedApplication) return;
    const reason = prompt("Enter mandatory rejection reason:");
    if (!reason || !reason.trim()) {
        alert("Rejection reason is mandatory.");
        return;
    }

    try {
        const res = await fetch(`/api/manager/loans/${selectedApplication.id}/reject`, {
            method: "PATCH",
            headers: getAuthHeaders(),
            body: JSON.stringify({ rejectionReason: reason.trim() })
        });

        if (res.ok) {
            showNotification(`Loan ${selectedApplication.applicationReference} REJECTED.`);
            closeModal("manager-decision-modal");
            loadManagerQueue();
        } else {
            const err = await res.json();
            alert("Rejection failed: " + (err.message || "Error"));
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function promptDisbursement(appId, appRef, amount) {
    if (confirm(`Application ${appRef} is APPROVED. Would you like to execute simulated disbursement of ₹${amount.toLocaleString()} and generate the reducing-balance EMI schedule now?`)) {
        try {
            const res = await fetch(`/api/manager/loans/${appId}/disburse`, {
                method: "POST",
                headers: getAuthHeaders()
            });

            if (res.ok) {
                const data = await res.json();
                alert(`Disbursement Executed Successfully!\nDisbursement Ref: ${data.disbursementReference}\nBeneficiary Account: ${data.simulatedBeneficiaryAccount}\nEMI Repayment schedule generated.`);
                loadManagerQueue();
            } else {
                const err = await res.json();
                alert("Disbursement failed: " + (err.message || "Error"));
            }
        } catch (err) {
            alert("Disbursement error: " + err.message);
        }
    }
}

// ----------------------------------------------------
// 5. EMI REPAYMENT SCHEDULE & SIMULATED PAY
// ----------------------------------------------------
async function viewRepayments(appRef) {
    try {
        const res = await fetch(`/api/loans/${appRef}/repayments`, { headers: getAuthHeaders() });
        if (res.ok) {
            const schedule = await res.json();
            document.getElementById("emi-app-ref").innerText = appRef;
            const container = document.getElementById("emi-schedule-body");
            if (!container) return;

            container.innerHTML = schedule.map(item => `
                <tr>
                    <td>#${item.installmentNumber}</td>
                    <td>${item.dueDate}</td>
                    <td>₹${Number(item.emiAmount).toLocaleString()}</td>
                    <td>₹${Number(item.principalComponent).toLocaleString()}</td>
                    <td>₹${Number(item.interestComponent).toLocaleString()}</td>
                    <td>₹${Number(item.outstandingBalance).toLocaleString()}</td>
                    <td>
                        ${item.status === 'PAID' ? `<span class="badge badge-green">PAID (${new Date(item.paidAt).toLocaleDateString()})</span>` : `<span class="badge badge-amber">UPCOMING</span>`}
                    </td>
                    <td>
                        ${item.status !== 'PAID' ? `<button class="btn btn-success btn-sm" onclick="payEmi('${appRef}', ${item.id})">Pay EMI</button>` : `<span style="color:var(--text-muted); font-size:0.8rem;">${item.paymentReference}</span>`}
                    </td>
                </tr>
            `).join("");

            openModal("emi-schedule-modal");
        }
    } catch (err) {
        alert("Error loading schedule: " + err.message);
    }
}

async function payEmi(appRef, repaymentId) {
    try {
        const res = await fetch(`/api/loans/${appRef}/repayments/${repaymentId}/pay`, {
            method: "POST",
            headers: getAuthHeaders()
        });

        if (res.ok) {
            const data = await res.json();
            showNotification(`Payment recorded! Ref: ${data.paymentReference}`);
            viewRepayments(appRef);
        } else {
            const err = await res.json();
            alert("Payment failed: " + (err.message || "Error"));
        }
    } catch (err) {
        alert("Payment error: " + err.message);
    }
}

// ----------------------------------------------------
// 6. ADMIN DASHBOARD & AUDIT LOGS
// ----------------------------------------------------
async function loadAdminAnalytics() {
    try {
        const res = await fetch("/api/admin/loans/analytics", { headers: getAuthHeaders() });
        if (res.ok) {
            const data = await res.json();
            document.getElementById("adm-customers").innerText = data.totalCustomers;
            document.getElementById("adm-apps").innerText = data.totalApplications;
            document.getElementById("adm-pending-checker").innerText = data.pendingCheckerReviews;
            document.getElementById("adm-pending-manager").innerText = data.pendingManagerApprovals;
            document.getElementById("adm-approved").innerText = data.approvedLoans;
            document.getElementById("adm-disbursed").innerText = data.disbursedLoans;
            document.getElementById("adm-approved-amt").innerText = "₹" + Number(data.totalApprovedLoanAmount).toLocaleString();
            document.getElementById("adm-disbursed-amt").innerText = "₹" + Number(data.totalDisbursedLoanAmount).toLocaleString();
        }
        loadAdminAuditLogs();
    } catch (err) {
        console.error("Failed loading admin analytics", err);
    }
}

async function loadAdminAuditLogs() {
    try {
        const res = await fetch("/api/admin/loans/audit-logs?page=0&size=15", { headers: getAuthHeaders() });
        if (res.ok) {
            const page = await res.json();
            const container = document.getElementById("admin-audit-table-body");
            if (!container) return;
            container.innerHTML = page.content.map(log => `
                <tr>
                    <td><strong>${log.applicationReference}</strong></td>
                    <td>${log.actorUsername} (${log.actorRole})</td>
                    <td><span class="badge badge-blue">${log.action}</span></td>
                    <td>${log.previousStatus || '-'} &rarr; ${log.newStatus || '-'}</td>
                    <td>${new Date(log.timestamp).toLocaleString()}</td>
                    <td style="font-size:0.8rem; color:var(--text-secondary);">${log.remarks || '-'}</td>
                </tr>
            `).join("");
        }
    } catch (err) {
        console.error("Failed loading audit logs", err);
    }
}

async function createPrivilegedUser(e) {
    e.preventDefault();
    const username = document.getElementById("p-username").value;
    const email = document.getElementById("p-email").value;
    const password = document.getElementById("p-password").value;
    const role = document.getElementById("p-role").value;

    try {
        const res = await fetch("/api/auth/users", {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ username, email, password, role })
        });

        if (res.ok) {
            showNotification(`User ${username} created successfully with role ${role}!`);
            document.getElementById("create-user-form").reset();
        } else {
            const err = await res.json();
            alert("Failed to create user: " + (err.message || "Error"));
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ----------------------------------------------------
// 7. IN-APP NOTIFICATIONS
// ----------------------------------------------------
async function loadNotifications() {
    try {
        const res = await fetch("/api/notifications", { headers: getAuthHeaders() });
        if (res.ok) {
            const list = await res.json();
            const unreadCount = list.filter(n => !n.readStatus).length;
            const badgeEl = document.getElementById("notif-badge");
            if (badgeEl) {
                badgeEl.innerText = unreadCount;
                badgeEl.style.display = unreadCount > 0 ? "inline-block" : "none";
            }

            const listEl = document.getElementById("notif-dropdown-list");
            if (listEl) {
                if (list.length === 0) {
                    listEl.innerHTML = `<p style="padding:1rem; color:var(--text-muted); font-size:0.85rem;">No notifications.</p>`;
                } else {
                    listEl.innerHTML = list.map(n => `
                        <div style="padding:0.75rem 1rem; border-bottom:1px solid var(--border-color); background:${n.readStatus ? 'transparent' : 'rgba(59,130,246,0.05)'};">
                            <div style="font-weight:600; font-size:0.85rem; color:${n.readStatus ? 'var(--text-secondary)' : 'var(--text-primary)'};">${n.title}</div>
                            <div style="font-size:0.8rem; color:var(--text-secondary); margin-top:0.2rem;">${n.message}</div>
                            <div style="font-size:0.75rem; color:var(--text-muted); margin-top:0.2rem;">${new Date(n.createdAt).toLocaleTimeString()}</div>
                        </div>
                    `).join("");
                }
            }
        }
    } catch (err) {
        console.error("Notifications error", err);
    }
}

function toggleNotifDropdown() {
    const dropdown = document.getElementById("notif-dropdown");
    if (!dropdown) return;
    dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
    if (dropdown.style.display === "block") {
        fetch("/api/notifications/read-all", { method: "PATCH", headers: getAuthHeaders() })
            .then(() => {
                const badgeEl = document.getElementById("notif-badge");
                if (badgeEl) badgeEl.style.display = "none";
            });
    }
}

// ----------------------------------------------------
// 8. AI BANKING ASSISTANT
// ----------------------------------------------------
function initAiChat() {
    const fab = document.getElementById("ai-fab");
    const modal = document.getElementById("ai-modal");
    const closeBtn = document.getElementById("ai-close-btn");
    const form = document.getElementById("ai-chat-form");
    const input = document.getElementById("ai-chat-input");

    if (fab) fab.addEventListener("click", () => modal.classList.toggle("open"));
    if (closeBtn) closeBtn.addEventListener("click", () => modal.classList.remove("open"));

    if (form) {
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const query = input.value.trim();
            if (!query) return;

            appendChatMessage("user", query);
            input.value = "";

            try {
                const res = await fetch("/api/ai/chat", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ message: query })
                });

                if (res.ok) {
                    const data = await res.json();
                    let botHtml = `<div>${data.response}</div>`;
                    if (data.suggestedNextSteps && data.suggestedNextSteps.length > 0) {
                        botHtml += `<div style="margin-top:0.5rem; font-size:0.8rem; color:var(--accent-cyan);"><strong>Suggested Next Steps:</strong><ul style="padding-left:1.2rem; margin-top:0.2rem;">${data.suggestedNextSteps.map(s => `<li>${s}</li>`).join("")}</ul></div>`;
                    }
                    appendChatMessage("bot", botHtml);
                } else {
                    appendChatMessage("bot", "I am currently running in offline fallback mode. Please ask about loan products, eligibility guidelines, or reducing-balance EMI calculations!");
                }
            } catch (err) {
                appendChatMessage("bot", "AI Assistant is running locally. You can ask: 'What are the required documents for a Personal loan?' or 'How is reducing balance EMI calculated?'");
            }
        });
    }
}

function appendChatMessage(sender, htmlContent) {
    const container = document.getElementById("ai-messages");
    if (!container) return;
    const bubble = document.createElement("div");
    bubble.className = `chat-bubble ${sender === 'user' ? 'bubble-user' : 'bubble-bot'}`;
    bubble.innerHTML = htmlContent;
    container.appendChild(bubble);
    container.scrollTop = container.scrollHeight;
}

// Modal Utilities
function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add("open");
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove("open");
}

function showNotification(msg) {
    const el = document.createElement("div");
    el.style.position = "fixed";
    el.style.bottom = "20px";
    el.style.left = "20px";
    el.style.background = "#10b981";
    el.style.color = "#ffffff";
    el.style.padding = "10px 18px";
    el.style.borderRadius = "8px";
    el.style.fontWeight = "600";
    el.style.fontSize = "0.85rem";
    el.style.boxShadow = "0 4px 12px rgba(0,0,0,0.3)";
    el.style.zIndex = "999";
    el.innerText = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 3500);
}

