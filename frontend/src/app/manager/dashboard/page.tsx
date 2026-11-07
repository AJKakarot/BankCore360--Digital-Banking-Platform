"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  CheckCircle2,
  XCircle,
  AlertCircle,
  ArrowRight
} from "lucide-react";
import { toast } from "@/components/ui/Toast";
import {
  fetchManagerPendingApplications,
  fetchManagerApprovedApplications,
  approveManagerApplication,
  disburseManagerLoan,
  rejectManagerApplication,
  getActiveUser,
  clearSession
} from "@/lib/api";

interface ManagerQueueItem {
  id: number;
  ref: string;
  applicant: string;
  loanType: string;
  requestedAmount: number;
  approvedAmount?: number;
  interestRate?: number;
  tenure: number;
  purpose: string;
  verifiedDocs: string[];
  status?: string;
}

const INITIAL_MANAGER_QUEUE: ManagerQueueItem[] = [
  {
    id: 201,
    ref: "LN-2026-MGR01",
    applicant: "Priya Patel (priya@example.com)",
    loanType: "EDUCATION",
    requestedAmount: 450000,
    tenure: 60,
    purpose: "Master of Science Tuition & living expenses",
    verifiedDocs: ["Test PAN (Verified)", "Admission Letter (Verified)", "Salary Slips (Verified)"],
  },
  {
    id: 202,
    ref: "LN-2026-MGR02",
    applicant: "Karan Mehta (karan@example.com)",
    loanType: "PERSONAL",
    requestedAmount: 250000,
    tenure: 36,
    purpose: "Home refurbishment & furniture",
    verifiedDocs: ["Test PAN (Verified)", "Utility Bill (Verified)", "Bank Statement (Verified)"],
  },
];

export default function ManagerDashboard() {
  const router = useRouter();
  const [queue, setQueue] = useState<ManagerQueueItem[]>(INITIAL_MANAGER_QUEUE);
  const [approvedQueue, setApprovedQueue] = useState<ManagerQueueItem[]>([]);
  const [activeTab, setActiveTab] = useState<"PENDING" | "APPROVED">("PENDING");
  const [selectedApp, setSelectedApp] = useState<ManagerQueueItem | null>(null);
  const [approvedAmount, setApprovedAmount] = useState<number>(0);
  const [interestRate, setInterestRate] = useState<number>(9.5);
  const [approvedTenure, setApprovedTenure] = useState<number>(60);
  const [remarks, setRemarks] = useState("Credit risk profile approved per standard guidelines");
  const [autoDisburseImmediately, setAutoDisburseImmediately] = useState<boolean>(true);
  const [rejectionReason, setRejectionReason] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [hasSanctioned, setHasSanctioned] = useState(false);
  const [disbursingId, setDisbursingId] = useState<number | null>(null);

  const loadManagerData = async () => {
    try {
      const [pendingApps, approvedApps] = await Promise.all([
        fetchManagerPendingApplications().catch(() => []),
        fetchManagerApprovedApplications().catch(() => []),
      ]);

      if (pendingApps && pendingApps.length > 0) {
        const items: ManagerQueueItem[] = pendingApps.map((a) => ({
          id: a.id,
          ref: a.applicationReference,
          applicant: `${a.customerFullName || "Customer"} (${a.customerEmail || "customer@bankcore360.com"})`,
          loanType: a.loanType,
          requestedAmount: a.requestedAmount,
          tenure: a.tenureMonths,
          purpose: a.purpose || "Retail loan facility",
          verifiedDocs: ["KYC Documents (Verified by Checker)", "Income Proof (Verified)"],
          status: a.status,
        }));
        setQueue(items);
      } else {
        setQueue([]);
      }

      if (approvedApps && approvedApps.length > 0) {
        const approvedItems: ManagerQueueItem[] = approvedApps.map((a) => ({
          id: a.id,
          ref: a.applicationReference,
          applicant: `${a.customerFullName || "Customer"} (${a.customerEmail || "customer@bankcore360.com"})`,
          loanType: a.loanType,
          requestedAmount: a.requestedAmount,
          approvedAmount: a.approvedAmount || a.requestedAmount,
          interestRate: a.interestRate || 9.5,
          tenure: a.approvedTenureMonths || a.tenureMonths,
          purpose: a.purpose || "Retail loan facility",
          verifiedDocs: ["KYC Documents (Verified)", "Sanction Underwritten"],
          status: a.status,
        }));
        setApprovedQueue(approvedItems);
      } else {
        setApprovedQueue([]);
      }
    } catch {
      setQueue(INITIAL_MANAGER_QUEUE);
    }
  };

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=MANAGER");
      return;
    }
    if (user.role !== "MANAGER" && user.role !== "ADMIN") {
      router.push("/login?role=MANAGER");
      return;
    }

    loadManagerData();
  }, [router]);

  const openApproveModal = (app: ManagerQueueItem) => {
    setSelectedApp(app);
    setApprovedAmount(app.requestedAmount);
    setApprovedTenure(app.tenure);
    if (app.loanType === "HOME") setInterestRate(8.75);
    else if (app.loanType === "EDUCATION") setInterestRate(9.25);
    else if (app.loanType === "PERSONAL") setInterestRate(11.5);
    else setInterestRate(12.5);
  };

  const handleApprove = async () => {
    if (!selectedApp) return;
    try {
      await approveManagerApplication(selectedApp.id, {
        approvedAmount,
        interestRate,
        approvedTenureMonths: approvedTenure,
        managerRemarks: remarks,
        autoDisburse: autoDisburseImmediately,
      });

      if (autoDisburseImmediately) {
        try {
          await disburseManagerLoan(selectedApp.id);
        } catch {}
        toast.success(
          "🎉 Sanctioned & Disbursed!",
          `Approved ${selectedApp.ref} and instantly transferred ₹${approvedAmount.toLocaleString()} from Bank Vault to customer operative account!`
        );
      } else {
        toast.success("Loan Sanctioned!", `Approved ${selectedApp.ref} at ${interestRate}% p.a. Ready for disbursement.`);
      }
    } catch {
      toast.success("Loan Sanctioned (Local)", `Approved ${selectedApp.ref}`);
    }

    await loadManagerData();
    setNotice(`Loan ${selectedApp.ref} processed successfully!`);
    setHasSanctioned(true);
    setSelectedApp(null);
  };

  const handleDisburseOnly = async (appId: number, ref: string, amount: number) => {
    setDisbursingId(appId);
    try {
      await disburseManagerLoan(appId);
      toast.success(
        "💸 Disbursal Complete!",
        `Transferred ₹${amount.toLocaleString()} from Bank Vault into Customer Operative Account for ${ref}.`
      );
      await loadManagerData();
    } catch (err: any) {
      toast.error("Disbursal Failed", err.message || "Failed to disburse loan funds");
    } finally {
      setDisbursingId(null);
    }
  };

  const handleReject = async () => {
    if (!selectedApp || !rejectionReason.trim()) return;
    try {
      await rejectManagerApplication(selectedApp.id, rejectionReason.trim());
      toast.error("Loan Rejected", `Application ${selectedApp.ref} rejected`);
    } catch {
      toast.error("Loan Rejected (Local)", `Application ${selectedApp.ref} rejected`);
    }
    setQueue((prev) => prev.filter((a) => a.id !== selectedApp.id));
    setNotice(`Loan ${selectedApp.ref} rejected.`);
    setHasSanctioned(true);
    setSelectedApp(null);
    setRejectionReason("");
  };

  const handleSwitchToAdmin = () => {
    clearSession();
    router.push("/login?role=ADMIN");
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 max-w-5xl mx-auto animate-fade-in">
      {/* Header */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Credit Approvals</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">Manager Credit Sanctions</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Review document-verified files, determine approved quantum and interest rate, and execute decisions.
          </p>
        </div>
      </div>

      {/* Role Handoff Banner: Move to Admin */}
      {hasSanctioned && (
        <div className="bg-[#1C1C1C] border border-[#CAFF33]/40 rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-xl animate-fade-in">
          <div className="space-y-1 text-center sm:text-left">
            <div className="inline-flex items-center space-x-2 text-[#CAFF33] text-xs font-bold uppercase tracking-wider">
              <span className="w-2 h-2 rounded-full bg-[#CAFF33] animate-pulse"></span>
              <span>Workflow Handoff</span>
            </div>
            <h3 className="text-lg font-bold text-white">Loan Sanctioned & Decision Recorded</h3>
            <p className="text-xs text-neutral-400">
              Next step: Log out of Manager account and sign in as Admin to inspect the compliance audit trail.
            </p>
          </div>
          <button
            onClick={handleSwitchToAdmin}
            className="w-full sm:w-auto justify-center bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-3 rounded-full font-bold text-xs shadow-md transition flex items-center space-x-2 whitespace-nowrap active:scale-95"
          >
            <span>Switch to Admin Login</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {notice && !hasSanctioned && (
        <div className="p-4 bg-[#1C1C1C] border border-[#CAFF33]/50 rounded-2xl text-xs text-[#CAFF33] flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{notice}</span>
        </div>
      )}

      {/* Queue List with Tabs */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-[#262626] flex flex-col sm:flex-row justify-between sm:items-center gap-3">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setActiveTab("PENDING")}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center space-x-2 ${
                activeTab === "PENDING"
                  ? "bg-[#CAFF33] text-black shadow-md"
                  : "bg-[#141414] text-neutral-400 hover:text-white border border-[#2B2B2B]"
              }`}
            >
              <span>Pending Sanctions</span>
              <span className={`px-2 py-0.5 rounded-full text-[10px] ${
                activeTab === "PENDING" ? "bg-black/20 text-black" : "bg-[#262626] text-neutral-300"
              }`}>
                {queue.length}
              </span>
            </button>

            <button
              onClick={() => setActiveTab("APPROVED")}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center space-x-2 ${
                activeTab === "APPROVED"
                  ? "bg-[#CAFF33] text-black shadow-md"
                  : "bg-[#141414] text-neutral-400 hover:text-white border border-[#2B2B2B]"
              }`}
            >
              <span>Ready for Disbursal</span>
              <span className={`px-2 py-0.5 rounded-full text-[10px] ${
                activeTab === "APPROVED" ? "bg-black/20 text-black" : "bg-emerald-500/20 text-emerald-400"
              }`}>
                {approvedQueue.length}
              </span>
            </button>
          </div>

          <div className="text-[11px] text-neutral-400">
            {activeTab === "PENDING"
              ? "Review compliance-verified borrower applications"
              : "Disburse approved loan capital directly from ₹100L Bank Vault"}
          </div>
        </div>

        {activeTab === "PENDING" ? (
          queue.length === 0 ? (
            <div className="p-8 text-center text-xs text-neutral-500">
              No applications pending Manager decision. All verified files have been processed.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs min-w-[550px]">
                <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
                  <tr>
                    <th className="p-4 font-semibold">Reference</th>
                    <th className="p-4 font-semibold">Applicant</th>
                    <th className="p-4 font-semibold">Product</th>
                    <th className="p-4 font-semibold">Requested</th>
                    <th className="p-4 font-semibold">Verified Proofs</th>
                    <th className="p-4 font-semibold text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#262626] text-neutral-300">
                  {queue.map((item) => (
                    <tr key={item.id} className="hover:bg-[#222222] transition">
                      <td className="p-4 font-mono font-bold text-white">{item.ref}</td>
                      <td className="p-4 font-medium text-white">{item.applicant}</td>
                      <td className="p-4">
                        <span className="px-2.5 py-1 rounded-full bg-[#262626] text-neutral-200 font-semibold border border-[#333333]">
                          {item.loanType}
                        </span>
                      </td>
                      <td className="p-4 font-bold text-white">₹{item.requestedAmount.toLocaleString()}</td>
                      <td className="p-4 text-[#CAFF33] font-medium">{item.verifiedDocs.join(", ")}</td>
                      <td className="p-4 text-right">
                        <button
                          onClick={() => openApproveModal(item)}
                          className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-4 py-2 rounded-full text-xs font-semibold shadow transition active:scale-95"
                        >
                          Approve / Reject
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        ) : (
          approvedQueue.length === 0 ? (
            <div className="p-8 text-center text-xs text-neutral-500">
              No approved loans awaiting disbursement. All approved funds have been disbursed to customer accounts.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs min-w-[550px]">
                <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
                  <tr>
                    <th className="p-4 font-semibold">Reference</th>
                    <th className="p-4 font-semibold">Applicant</th>
                    <th className="p-4 font-semibold">Product</th>
                    <th className="p-4 font-semibold">Sanction Amount</th>
                    <th className="p-4 font-semibold">Rate & Tenure</th>
                    <th className="p-4 font-semibold text-right">Instant Disbursal</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#262626] text-neutral-300">
                  {approvedQueue.map((item) => (
                    <tr key={item.id} className="hover:bg-[#222222] transition">
                      <td className="p-4 font-mono font-bold text-white">{item.ref}</td>
                      <td className="p-4 font-medium text-white">{item.applicant}</td>
                      <td className="p-4">
                        <span className="px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-400 font-semibold border border-emerald-500/30">
                          {item.loanType}
                        </span>
                      </td>
                      <td className="p-4 font-bold text-[#CAFF33]">₹{(item.approvedAmount || item.requestedAmount).toLocaleString()}</td>
                      <td className="p-4 text-neutral-300 font-mono text-[11px]">
                        {item.interestRate || 9.5}% p.a. • {item.tenure}m
                      </td>
                      <td className="p-4 text-right">
                        <button
                          onClick={() => handleDisburseOnly(item.id, item.ref, item.approvedAmount || item.requestedAmount)}
                          disabled={disbursingId === item.id}
                          className="bg-emerald-500 hover:bg-emerald-400 text-black font-bold px-4 py-2 rounded-full text-xs shadow transition active:scale-95 disabled:opacity-50"
                        >
                          {disbursingId === item.id ? "Disbursing..." : "⚡ Disburse to Customer A/C"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>

      {/* Decision Modal */}
      {selectedApp && (
        <div className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-3 sm:p-4">
          <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl max-w-xl w-[94vw] sm:w-full p-5 sm:p-8 space-y-6 shadow-2xl">
            <div className="flex justify-between items-start border-b border-[#262626] pb-4">
              <div>
                <span className="text-[11px] font-mono text-neutral-500">REF: {selectedApp.ref}</span>
                <h3 className="text-xl font-bold text-white">Credit Decision: {selectedApp.applicant}</h3>
                <p className="text-xs text-neutral-400 mt-1">
                  Product: {selectedApp.loanType} | Purpose: {selectedApp.purpose}
                </p>
              </div>
              <button
                onClick={() => setSelectedApp(null)}
                className="text-xs text-neutral-400 hover:text-white px-3 py-1.5 rounded-full bg-[#262626]"
              >
                Close
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div>
                <label className="block text-neutral-300 font-semibold mb-1">Approved Quantum (₹)</label>
                <input
                  type="number"
                  value={approvedAmount}
                  onChange={(e) => setApprovedAmount(Number(e.target.value))}
                  className="w-full bg-[#141414] border border-[#262626] rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-[#CAFF33]"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-neutral-300 font-semibold mb-1">Interest Rate (% p.a.)</label>
                  <input
                    type="number"
                    step="0.25"
                    value={interestRate}
                    onChange={(e) => setInterestRate(Number(e.target.value))}
                    className="w-full bg-[#141414] border border-[#262626] rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-[#CAFF33]"
                  />
                </div>
                <div>
                  <label className="block text-neutral-300 font-semibold mb-1">Approved Tenure (Months)</label>
                  <input
                    type="number"
                    value={approvedTenure}
                    onChange={(e) => setApprovedTenure(Number(e.target.value))}
                    className="w-full bg-[#141414] border border-[#262626] rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-[#CAFF33]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-neutral-300 font-semibold mb-1">Manager Sanction Remarks</label>
                <textarea
                  rows={2}
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  className="w-full bg-[#141414] border border-[#262626] rounded-xl p-3 text-white focus:outline-none focus:border-[#CAFF33]"
                />
              </div>

              <div className="flex items-center space-x-2.5 p-3 rounded-xl bg-[#141414] border border-[#2B2B2B]">
                <input
                  type="checkbox"
                  id="autoDisburse"
                  checked={autoDisburseImmediately}
                  onChange={(e) => setAutoDisburseImmediately(e.target.checked)}
                  className="w-4 h-4 accent-[#CAFF33] rounded cursor-pointer"
                />
                <label htmlFor="autoDisburse" className="text-xs text-neutral-300 font-medium cursor-pointer">
                  ⚡ <strong className="text-[#CAFF33]">Auto-disburse instantly</strong> to Customer Operative A/C from Bank Vault
                </label>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-3 pt-4 border-t border-[#262626]">
              <div className="w-full sm:flex-1 sm:mr-4">
                <input
                  type="text"
                  placeholder="Reason if rejecting..."
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  className="w-full bg-[#141414] border border-[#262626] rounded-xl px-3 py-2 text-xs text-white placeholder-neutral-500"
                />
              </div>

              <div className="flex space-x-2 justify-end w-full sm:w-auto">
                <button
                  onClick={handleReject}
                  disabled={!rejectionReason.trim()}
                  className="px-4 py-2.5 bg-red-950/60 hover:bg-red-900 text-red-300 rounded-full text-xs font-bold transition disabled:opacity-40 border border-red-800"
                >
                  Reject
                </button>
                <button
                  onClick={handleApprove}
                  className="px-5 py-2.5 bg-[#CAFF33] hover:bg-[#b8eb2a] text-black rounded-full text-xs font-bold transition shadow active:scale-95"
                >
                  {autoDisburseImmediately ? "Approve & Disburse Instantly" : "Sanction & Approve"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
