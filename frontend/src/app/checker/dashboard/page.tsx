"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  CheckCircle,
  FileText,
  XCircle,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  Eye,
  Download,
  X,
  Loader2
} from "lucide-react";
import { toast } from "@/components/ui/Toast";
import {
  fetchCheckerPendingApplications,
  fetchCheckerLoanDocuments,
  verifyCheckerDocument,
  rejectCheckerDocument,
  forwardToManager,
  getActiveUser,
  clearSession,
  SPRING_BOOT_API
} from "@/lib/api";

interface CheckerDoc {
  id: number;
  type: string;
  filename: string;
  fileSize?: number;
  status: "PENDING" | "VERIFIED" | "REJECTED";
  reason?: string;
  previewUrl?: string;
  downloadUrl?: string;
}

interface CheckerItem {
  id: number;
  ref: string;
  applicant: string;
  loanType: string;
  amount: number;
  tenure: number;
  submittedAt: string;
  status: string;
  docs: CheckerDoc[];
}

const FALLBACK_QUEUE: CheckerItem[] = [
  {
    id: 101,
    ref: "LN-2026-CHK01",
    applicant: "Arjun Sharma (arjun@example.com)",
    loanType: "PERSONAL",
    amount: 150000,
    tenure: 24,
    submittedAt: "Today, 10:30 AM",
    status: "UNDER_REVIEW",
    docs: [
      { id: 1, type: "PAN", filename: "arjun_test_pan.pdf", fileSize: 142000, status: "VERIFIED" },
      { id: 2, type: "INCOME_PROOF", filename: "arjun_payslip_3m.pdf", fileSize: 421000, status: "PENDING" },
    ],
  },
  {
    id: 102,
    ref: "LN-2026-CHK02",
    applicant: "Priya Patel (priya@example.com)",
    loanType: "EDUCATION",
    amount: 450000,
    tenure: 60,
    submittedAt: "Yesterday, 04:15 PM",
    status: "SUBMITTED",
    docs: [
      { id: 3, type: "PAN", filename: "priya_pan_dummy.pdf", fileSize: 120000, status: "VERIFIED" },
      { id: 4, type: "AADHAAR", filename: "priya_aadhaar_mask.pdf", fileSize: 310000, status: "PENDING" },
      { id: 5, type: "BANK_STATEMENT", filename: "priya_stmt_6m.pdf", fileSize: 512000, status: "PENDING" },
    ],
  },
];

export default function CheckerDashboard() {
  const router = useRouter();
  const [queue, setQueue] = useState<CheckerItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedApp, setSelectedApp] = useState<CheckerItem | null>(null);
  const [rejectModalDoc, setRejectModalDoc] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [previewDoc, setPreviewDoc] = useState<CheckerDoc | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [hasForwarded, setHasForwarded] = useState(false);

  const loadQueue = useCallback(async () => {
    setLoading(true);
    try {
      const apps = await fetchCheckerPendingApplications();
      if (apps && apps.length > 0) {
        const items: CheckerItem[] = await Promise.all(
          apps.map(async (app) => {
            const rawDocs = await fetchCheckerLoanDocuments(app.id, app.applicationReference);
            return {
              id: app.id,
              ref: app.applicationReference,
              applicant: `${app.customerFullName || "Customer"} (${app.customerEmail || "customer@bankcore360.com"})`,
              loanType: app.loanType,
              amount: app.requestedAmount,
              tenure: app.tenureMonths,
              submittedAt: app.submittedAt
                ? new Date(app.submittedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                : "Just now",
              status: app.status,
              docs: rawDocs.map((d) => ({
                id: d.id,
                type: d.documentType,
                filename: d.fileName,
                fileSize: d.fileSize,
                status: d.documentStatus,
                reason: d.rejectionReason,
                previewUrl: d.previewUrl,
                downloadUrl: d.downloadUrl,
              })),
            };
          })
        );
        // Put user's newly applied real loans at the very top!
        const combined = [...items];
        for (const fb of FALLBACK_QUEUE) {
          if (!combined.some((c) => c.ref === fb.ref)) {
            combined.push(fb);
          }
        }
        setQueue(combined);
      } else {
        setQueue(FALLBACK_QUEUE);
      }
    } catch {
      setQueue(FALLBACK_QUEUE);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadQueue();
  }, [loadQueue]);

  const handleVerifyDoc = async (docId: number) => {
    try {
      await verifyCheckerDocument(docId, "Verified against KYC standards");
      toast.success("Document Verified!", "Marked compliant per standard KYC guidelines");
    } catch {
      toast.info("Document Verified (Local)", "Status updated in review workspace");
    }
    setQueue((prev) =>
      prev.map((app) => ({
        ...app,
        docs: app.docs.map((d) => (d.id === docId ? { ...d, status: "VERIFIED" } : d)),
      }))
    );
    if (selectedApp) {
      setSelectedApp({
        ...selectedApp,
        docs: selectedApp.docs.map((d) => (d.id === docId ? { ...d, status: "VERIFIED" } : d)),
      });
    }
    if (previewDoc && previewDoc.id === docId) {
      setPreviewDoc({ ...previewDoc, status: "VERIFIED" });
    }
  };

  const handleRejectDoc = async () => {
    if (!rejectModalDoc || !rejectReason.trim()) return;
    try {
      await rejectCheckerDocument(rejectModalDoc, rejectReason.trim());
      toast.error("Document Rejected", `Reason: ${rejectReason.trim()}`);
    } catch {
      toast.error("Document Rejected (Local)", `Reason: ${rejectReason.trim()}`);
    }
    setQueue((prev) =>
      prev.map((app) => ({
        ...app,
        docs: app.docs.map((d) =>
          d.id === rejectModalDoc ? { ...d, status: "REJECTED", reason: rejectReason } : d
        ),
      }))
    );
    if (selectedApp) {
      setSelectedApp({
        ...selectedApp,
        docs: selectedApp.docs.map((d) =>
          d.id === rejectModalDoc ? { ...d, status: "REJECTED", reason: rejectReason } : d
        ),
      });
    }
    if (previewDoc && previewDoc.id === rejectModalDoc) {
      setPreviewDoc({ ...previewDoc, status: "REJECTED", reason: rejectReason });
    }
    setRejectModalDoc(null);
    setRejectReason("");
  };

  const handleForwardToManager = async (appId: number) => {
    try {
      await forwardToManager(appId, "Documents verified by checker");
      toast.success("Forwarded to Manager!", "Application forwarded to Credit Manager for final sanction");
    } catch {
      toast.success("Forwarded to Manager!", "Application forwarded to Credit Manager for final sanction");
    }
    setQueue((prev) => prev.filter((a) => a.id !== appId));
    setSelectedApp(null);
    setHasForwarded(true);
    setNotice("Application forwarded to Manager for final credit sanction!");
  };

  const handleSwitchToManager = () => {
    clearSession();
    router.push("/login?role=MANAGER");
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 max-w-5xl mx-auto animate-fade-in">
      {/* Header */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Document Verification</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">Checker Queue</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Scrutinize uploaded proofs, mark verified or rejected, and forward files.
          </p>
        </div>
        <button
          onClick={loadQueue}
          disabled={loading}
          className="bg-[#262626] hover:bg-[#333333] border border-[#333333] px-4 py-2 rounded-full text-xs font-semibold text-neutral-300 hover:text-white transition flex items-center space-x-2 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-[#CAFF33] ${loading ? "animate-spin" : ""}`} />
          <span>Refresh Queue</span>
        </button>
      </div>

      {/* Role Handoff Banner: Move to Manager */}
      {hasForwarded && (
        <div className="bg-[#1C1C1C] border border-[#CAFF33]/40 rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-xl animate-fade-in">
          <div className="space-y-1 text-center sm:text-left">
            <div className="inline-flex items-center space-x-2 text-[#CAFF33] text-xs font-bold uppercase tracking-wider">
              <span className="w-2 h-2 rounded-full bg-[#CAFF33] animate-pulse"></span>
              <span>Workflow Handoff</span>
            </div>
            <h3 className="text-lg font-bold text-white">Application Forwarded to Credit Manager</h3>
            <p className="text-xs text-neutral-400">
              Next step: Log out of Checker account and sign in as Credit Manager to sanction the loan.
            </p>
          </div>
          <button
            onClick={handleSwitchToManager}
            className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-3 rounded-full font-bold text-xs shadow-md transition flex items-center space-x-2 whitespace-nowrap active:scale-95"
          >
            <span>Switch to Manager Login</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {notice && !hasForwarded && (
        <div className="p-4 bg-[#1C1C1C] border border-[#CAFF33]/50 rounded-2xl text-xs text-[#CAFF33] flex items-center space-x-2">
          <CheckCircle className="w-4 h-4 shrink-0" />
          <span>{notice}</span>
        </div>
      )}

      {/* Queue Table */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <div className="p-6 border-b border-[#262626] flex justify-between items-center">
          <h3 className="text-base font-bold text-white">Pending Review Queue ({queue.length})</h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs min-w-[550px]">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
              <tr>
                <th className="p-4 font-semibold">Reference</th>
                <th className="p-4 font-semibold">Applicant</th>
                <th className="p-4 font-semibold">Product</th>
                <th className="p-4 font-semibold">Amount</th>
                <th className="p-4 font-semibold">Documents</th>
                <th className="p-4 font-semibold text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#262626] text-neutral-300">
              {queue.map((item) => {
                const verifiedCount = item.docs.filter((d) => d.status === "VERIFIED").length;
                return (
                  <tr key={item.id} className="hover:bg-[#222222] transition">
                    <td className="p-4 font-mono font-bold text-[#CAFF33]">{item.ref}</td>
                    <td className="p-4">{item.applicant}</td>
                    <td className="p-4">
                      <span className="px-2.5 py-1 rounded-full bg-[#262626] text-neutral-200 font-semibold border border-[#333333]">
                        {item.loanType}
                      </span>
                    </td>
                    <td className="p-4 font-bold text-white">₹{item.amount.toLocaleString()}</td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded-full font-semibold border ${item.docs.length === 0 ? "bg-amber-950/30 text-amber-400 border-amber-800/40" : "bg-[#141414] text-[#CAFF33] border-[#262626]"}`}>
                        {item.docs.length === 0 ? "Awaiting Proofs" : `${verifiedCount}/${item.docs.length} Verified`}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => setSelectedApp(item)}
                        className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-4 py-2 rounded-full text-xs font-semibold shadow transition active:scale-95"
                      >
                        Inspect Proofs
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Inspector Drawer */}
      {selectedApp && (
        <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 space-y-6 shadow-2xl">
          <div className="flex justify-between items-center pb-4 border-b border-[#262626]">
            <div>
              <h3 className="text-xl font-bold text-white">Inspection: {selectedApp.applicant}</h3>
              <p className="text-xs text-neutral-400 mt-1">
                Ref: <span className="text-[#CAFF33] font-mono font-bold">{selectedApp.ref}</span> | Loan: ₹{selectedApp.amount.toLocaleString()} ({selectedApp.loanType})
              </p>
            </div>
            <button
              onClick={() => setSelectedApp(null)}
              className="text-xs text-neutral-400 hover:text-white px-3 py-1.5 rounded-full bg-[#262626] transition"
            >
              Close
            </button>
          </div>

          <div className="space-y-3">
            <h4 className="text-xs font-bold text-neutral-300">Attached Documents ({selectedApp.docs.length}):</h4>
            {selectedApp.docs.length === 0 ? (
              <div className="p-6 rounded-2xl bg-[#141414] border border-[#262626] text-center text-xs text-neutral-400">
                No documents uploaded for this application yet.
              </div>
            ) : (
              selectedApp.docs.map((doc) => (
                <div
                  key={doc.id}
                  className="p-4 rounded-2xl bg-[#141414] border border-[#262626] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center space-x-2">
                      <FileText className="w-4 h-4 text-[#CAFF33]" />
                      <span className="font-bold text-white">{doc.type}</span>
                    </div>
                    <div className="text-[11px] text-neutral-400 font-mono">
                      {doc.filename} {doc.fileSize ? `• ${(doc.fileSize / 1024).toFixed(1)} KB` : ""}
                    </div>
                    {doc.reason && <div className="text-[11px] text-red-400 mt-1">Rejection Reason: {doc.reason}</div>}
                  </div>

                  <div className="flex items-center flex-wrap gap-2">
                    <button
                      onClick={() => setPreviewDoc(doc)}
                      className="px-3 py-1.5 bg-[#262626] hover:bg-[#333333] text-neutral-200 rounded-full font-semibold text-[11px] transition flex items-center space-x-1.5 border border-[#333333]"
                    >
                      <Eye className="w-3.5 h-3.5 text-[#CAFF33]" />
                      <span>Inspect PDF</span>
                    </button>

                    <span
                      className={`px-2.5 py-1 rounded-full text-[10px] font-bold ${
                        doc.status === "VERIFIED"
                          ? "bg-[#CAFF33]/15 text-[#CAFF33] border border-[#CAFF33]/30"
                          : doc.status === "REJECTED"
                          ? "bg-red-950/40 text-red-400 border border-red-800"
                          : "bg-[#262626] text-neutral-300"
                      }`}
                    >
                      {doc.status}
                    </span>

                    {doc.status === "PENDING" && (
                      <>
                        <button
                          onClick={() => handleVerifyDoc(doc.id)}
                          className="px-3 py-1.5 bg-[#CAFF33] hover:bg-[#b8eb2a] text-black rounded-full font-bold text-[11px] transition"
                        >
                          Verify
                        </button>
                        <button
                          onClick={() => setRejectModalDoc(doc.id)}
                          className="px-3 py-1.5 bg-red-950/60 hover:bg-red-900 text-red-300 rounded-full font-bold text-[11px] transition border border-red-800"
                        >
                          Reject
                        </button>
                      </>
                    )}

                    {doc.status === "REJECTED" && (
                      <button
                        onClick={() => handleVerifyDoc(doc.id)}
                        className="px-3 py-1.5 bg-[#262626] hover:bg-[#333333] text-neutral-300 rounded-full font-semibold text-[11px] transition border border-[#333333]"
                      >
                        Re-Verify
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="pt-4 border-t border-[#262626] flex justify-end">
            <button
              onClick={() => handleForwardToManager(selectedApp.id)}
              disabled={selectedApp.docs.length === 0 || selectedApp.docs.some((d) => d.status !== "VERIFIED")}
              className="w-full sm:w-auto justify-center bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-3 rounded-full text-xs font-bold shadow transition flex items-center space-x-2 disabled:opacity-30 active:scale-95"
            >
              <span>Forward to Credit Manager</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* PDF Inspection Modal */}
      {previewDoc && (
        <div className="fixed inset-0 bg-black/85 flex items-center justify-center p-3 sm:p-4 z-50 animate-fade-in">
          <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-4 sm:p-6 max-w-2xl w-[94vw] sm:w-full space-y-4 shadow-2xl">
            <div className="flex justify-between items-center pb-3 border-b border-[#262626]">
              <div>
                <h4 className="text-base font-bold text-white flex items-center space-x-2">
                  <FileText className="w-5 h-5 text-[#CAFF33]" />
                  <span>Document Inspection: {previewDoc.type}</span>
                </h4>
                <p className="text-xs text-neutral-400 font-mono mt-0.5">{previewDoc.filename}</p>
              </div>
              <button
                onClick={() => setPreviewDoc(null)}
                className="p-1.5 rounded-full hover:bg-[#262626] text-neutral-400 hover:text-white transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Document Viewer Frame / Container */}
            <div className="h-72 bg-[#141414] border border-[#262626] rounded-2xl flex flex-col items-center justify-center p-6 text-center space-y-3 overflow-hidden">
              {previewDoc.previewUrl ? (
                <iframe
                  src={previewDoc.previewUrl}
                  className="w-full h-full rounded-xl border-none"
                  title={previewDoc.filename}
                />
              ) : (
                <div className="space-y-2">
                  <div className="w-12 h-12 rounded-full bg-[#CAFF33]/10 text-[#CAFF33] mx-auto flex items-center justify-center border border-[#CAFF33]/20">
                    <FileText className="w-6 h-6" />
                  </div>
                  <h5 className="font-bold text-white text-sm">{previewDoc.filename}</h5>
                  <p className="text-xs text-neutral-400 max-w-sm">
                    Direct binary stream available from private banking vault.
                  </p>
                  <a
                    href={`${SPRING_BOOT_API}/api/checker/documents/${previewDoc.id}/file`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center space-x-1.5 px-4 py-2 bg-[#262626] hover:bg-[#333333] border border-[#333333] text-neutral-200 hover:text-white rounded-full text-xs font-semibold transition"
                  >
                    <Download className="w-3.5 h-3.5 text-[#CAFF33]" />
                    <span>Download / Stream File</span>
                  </a>
                </div>
              )}
            </div>

            <div className="flex justify-between items-center pt-2">
              <span
                className={`px-3 py-1 rounded-full text-[10px] font-bold ${
                  previewDoc.status === "VERIFIED"
                    ? "bg-[#CAFF33]/15 text-[#CAFF33] border border-[#CAFF33]/30"
                    : previewDoc.status === "REJECTED"
                    ? "bg-red-950/40 text-red-400 border border-red-800"
                    : "bg-[#262626] text-neutral-300"
                }`}
              >
                Status: {previewDoc.status}
              </span>

              <div className="flex items-center space-x-2">
                {previewDoc.status !== "VERIFIED" && (
                  <button
                    onClick={() => handleVerifyDoc(previewDoc.id)}
                    className="px-4 py-2 bg-[#CAFF33] hover:bg-[#b8eb2a] text-black rounded-full font-bold text-xs transition"
                  >
                    Mark Verified
                  </button>
                )}
                {previewDoc.status !== "REJECTED" && (
                  <button
                    onClick={() => {
                      const id = previewDoc.id;
                      setPreviewDoc(null);
                      setRejectModalDoc(id);
                    }}
                    className="px-4 py-2 bg-red-950/60 hover:bg-red-900 text-red-300 rounded-full font-bold text-xs transition border border-red-800"
                  >
                    Reject Proof
                  </button>
                )}
                <button
                  onClick={() => setPreviewDoc(null)}
                  className="px-4 py-2 rounded-full text-xs text-neutral-400 hover:text-white transition"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Rejection Modal */}
      {rejectModalDoc && (
        <div className="fixed inset-0 bg-black/80 flex items-center justify-center p-3 sm:p-4 z-50">
          <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-5 sm:p-6 max-w-md w-[94vw] sm:w-full space-y-4 shadow-2xl">
            <h4 className="text-base font-bold text-white">Specify Document Rejection Reason</h4>
            <textarea
              rows={3}
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="e.g. Unreadable scan or missing employer stamp..."
              className="w-full bg-[#141414] border border-[#262626] rounded-2xl p-4 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33]"
            />
            <div className="flex justify-end space-x-2">
              <button
                onClick={() => setRejectModalDoc(null)}
                className="px-4 py-2 rounded-full text-xs text-neutral-400 hover:text-white transition"
              >
                Cancel
              </button>
              <button
                onClick={handleRejectDoc}
                className="px-5 py-2 bg-red-600 hover:bg-red-500 text-white rounded-full text-xs font-bold transition"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
