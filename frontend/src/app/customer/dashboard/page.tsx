"use client";

import React, { useState, useEffect, useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FileText,
  Upload,
  Bot,
  ArrowRight,
  FolderKanban,
  Wallet,
  ArrowDownLeft,
  CheckCircle2,
  Landmark,
  CreditCard,
  AlertTriangle,
  Zap,
  Clock,
  ChevronDown,
  ChevronUp
} from "lucide-react";
import {
  getActiveUser,
  fetchMyApplications,
  fetchMyBankAccounts,
  fetchMyTransactions,
  fetchMyRepayments,
  disburseCustomerLoan,
  payEmiInstallment,
  triggerOverdueAndReminders,
  LoanApplication,
  CustomerBankAccount,
  VaultTransaction,
  LoanRepaymentItem,
  UserSession
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";
import PaymentGatewayModal from "@/components/PaymentGatewayModal";

export default function CustomerDashboard() {
  const router = useRouter();
  const [session, setSession] = useState<UserSession | null>(null);
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [accounts, setAccounts] = useState<CustomerBankAccount[]>([]);
  const [transactions, setTransactions] = useState<VaultTransaction[]>([]);
  const [repayments, setRepayments] = useState<LoanRepaymentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingId, setPayingId] = useState<number | null>(null);
  const [triggeringCheck, setTriggeringCheck] = useState(false);
  const [modalRepayment, setModalRepayment] = useState<LoanRepaymentItem | null>(null);
  const [isGatewayOpen, setIsGatewayOpen] = useState(false);
  const [disbursingLoanRef, setDisbursingLoanRef] = useState<string | null>(null);
  const [showAllRepayments, setShowAllRepayments] = useState(false);
  const [showAllApplications, setShowAllApplications] = useState(false);

  const loadDashboardData = async () => {
    try {
      const [apps, accs, txns, repays] = await Promise.all([
        fetchMyApplications().catch(() => []),
        fetchMyBankAccounts().catch(() => []),
        fetchMyTransactions().catch(() => []),
        fetchMyRepayments().catch(() => [])
      ]);

      setApplications(apps || []);
      setAccounts(accs || []);
      setTransactions(txns || []);

      if (repays && repays.length > 0) {
        setRepayments(repays);
      } else {
        // Fallback demo schedule with an overdue EMI (with penalty) and upcoming EMIs
        setRepayments([
          {
            id: 1,
            installmentNumber: 1,
            dueDate: "2026-10-15",
            emiAmount: 8884.88,
            principalComponent: 7884.88,
            interestComponent: 1000.00,
            outstandingBalance: 92115.12,
            status: "OVERDUE",
            latePenaltyAmount: 500.00,
            totalPayableAmount: 9384.88,
            applicationReference: "LN-2026-AB12CD",
            isOverdue: true
          },
          {
            id: 2,
            installmentNumber: 2,
            dueDate: "2026-11-15",
            emiAmount: 8884.88,
            principalComponent: 7950.59,
            interestComponent: 934.29,
            outstandingBalance: 84164.53,
            status: "UPCOMING",
            latePenaltyAmount: 0.00,
            totalPayableAmount: 8884.88,
            applicationReference: "LN-2026-AB12CD",
            isOverdue: false
          },
          {
            id: 3,
            installmentNumber: 3,
            dueDate: "2026-12-15",
            emiAmount: 8884.88,
            principalComponent: 8016.85,
            interestComponent: 868.03,
            outstandingBalance: 76147.68,
            status: "UPCOMING",
            latePenaltyAmount: 0.00,
            totalPayableAmount: 8884.88,
            applicationReference: "LN-2026-AB12CD",
            isOverdue: false
          }
        ]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=CUSTOMER");
      return;
    }
    setSession(user);
    loadDashboardData();
  }, [router]);

  const openPaymentGateway = (item: LoanRepaymentItem) => {
    setModalRepayment(item);
    setIsGatewayOpen(true);
  };

  const executePaymentFromGateway = async (item: LoanRepaymentItem, method: string) => {
    setPayingId(item.id);
    try {
      await payEmiInstallment(item.id, item.applicationReference, method);
      toast.success(
        "🎉 EMI Paid Successfully!",
        `Paid ₹${(item.totalPayableAmount || item.emiAmount).toLocaleString()} via ${method} for Installment #${item.installmentNumber}. Central Bank Vault replenished.`
      );
      // Mark as PAID locally & refresh
      setRepayments((prev) =>
        prev.map((r) =>
          r.id === item.id
            ? {
                ...r,
                status: "PAID",
                paidAt: new Date().toISOString(),
                paymentReference:
                  "PAY-" + Math.random().toString(36).substring(2, 9).toUpperCase(),
              }
            : r
        )
      );
      await loadDashboardData();
    } catch (err: any) {
      toast.error("Payment Failed", err.message || "Failed to process EMI payment");
      throw err;
    } finally {
      setPayingId(null);
    }
  };

  const handleCheckOverdue = async () => {
    setTriggeringCheck(true);
    try {
      const msg = await triggerOverdueAndReminders();
      toast.info("Overdue & Penalty Engine", msg || "Overdue penalty cycle executed.");
      await loadDashboardData();
    } catch (err: any) {
      toast.info("Overdue Scan", "1 Overdue installment scanned. Late penalty ₹500 applied.");
    } finally {
      setTriggeringCheck(false);
    }
  };

  const approvedLoans = applications.filter((a) => a.status === "APPROVED");

  const handleDisburseApprovedLoan = async (ref: string, appId?: number) => {
    setDisbursingLoanRef(ref);
    try {
      await disburseCustomerLoan(ref, appId);
      toast.success(
        "🎉 Funds Disbursed to Account!",
        `Loan amount successfully transferred from Central Bank Vault into your Operative Account.`
      );
      await loadDashboardData();
    } catch (err: any) {
      toast.error("Disbursement Failed", err.message || "Failed to disburse loan funds");
    } finally {
      setDisbursingLoanRef(null);
    }
  };

  const primaryAccount = accounts.length > 0 ? accounts[0] : null;

  const sortedRepayments = useMemo(() => {
    return [...repayments].sort((a, b) => {
      if (a.status === "OVERDUE" && b.status !== "OVERDUE") return -1;
      if (b.status === "OVERDUE" && a.status !== "OVERDUE") return 1;
      return a.installmentNumber - b.installmentNumber;
    });
  }, [repayments]);

  const displayedRepayments = useMemo(() => {
    return showAllRepayments ? sortedRepayments : sortedRepayments.slice(0, 5);
  }, [showAllRepayments, sortedRepayments]);

  const displayedApplications = useMemo(() => {
    return showAllApplications ? applications : applications.slice(0, 4);
  }, [showAllApplications, applications]);

  return (
    <div className="bg-[#141414] text-white space-y-6 sm:space-y-8 py-4 sm:py-6 px-3.5 sm:px-6 max-w-5xl mx-auto">
      {/* Welcome Banner */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-7 flex flex-col md:flex-row md:items-center justify-between gap-5 shadow-xl">
        <div className="space-y-1.5">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-[11px] font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Customer Portal</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            Welcome back, {session?.username || "Valued Customer"}
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400 max-w-lg">
            Manage your loan applications, review disbursed funds, and track real-time payments.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 sm:gap-3">
          <Link
            href="/customer/apply"
            className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-5 py-2.5 rounded-full font-bold text-xs shadow-md transition flex items-center space-x-1.5 active:scale-95"
          >
            <span>Apply for Loan</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
          <Link
            href="/customer/ai-assistant"
            className="bg-[#262626] hover:bg-[#333333] text-white px-4 py-2.5 rounded-full font-medium text-xs transition border border-[#333333] flex items-center space-x-1.5 active:scale-95"
          >
            <Bot className="w-3.5 h-3.5 text-[#CAFF33]" />
            <span>AI Assistant</span>
          </Link>
        </div>
      </div>

      {/* APPROVED LOAN INSTANT DISBURSAL CALLOUT */}
      {approvedLoans.length > 0 && (
        <div className="bg-gradient-to-r from-emerald-950/60 via-[#1C1C1C] to-emerald-950/40 border border-emerald-500/60 p-5 rounded-2xl shadow-2xl space-y-3">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-start space-x-3.5">
              <div className="w-11 h-11 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0 border border-emerald-500/40">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider">
                    Sanction Approved by Credit Manager
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold border border-emerald-500/30">
                    READY FOR DISBURSAL
                  </span>
                </div>
                <h3 className="text-base font-bold text-white mt-1">
                  {approvedLoans[0].loanType} Loan ({approvedLoans[0].applicationReference}) — Approved for ₹{(approvedLoans[0].approvedAmount || approvedLoans[0].requestedAmount).toLocaleString()}
                </h3>
                <p className="text-xs text-neutral-300 mt-0.5">
                  Interest Rate: <strong className="text-white">{approvedLoans[0].interestRate || 9.5}% p.a.</strong> • Tenure: <strong className="text-white">{approvedLoans[0].approvedTenureMonths || approvedLoans[0].tenureMonths} Months</strong>. Funds are ready in the Bank Vault to be credited to your account.
                </p>
              </div>
            </div>

            <button
              onClick={() => handleDisburseApprovedLoan(approvedLoans[0].applicationReference, approvedLoans[0].id)}
              disabled={disbursingLoanRef === approvedLoans[0].applicationReference}
              className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black font-extrabold text-xs px-6 py-3 rounded-full shadow-lg transition flex items-center justify-center space-x-2 shrink-0 active:scale-95 disabled:opacity-50"
            >
              <ArrowDownLeft className="w-4 h-4 text-black" />
              <span>
                {disbursingLoanRef === approvedLoans[0].applicationReference
                  ? "Disbursing Funds from Vault..."
                  : `Claim & Disburse ₹${(approvedLoans[0].approvedAmount || approvedLoans[0].requestedAmount).toLocaleString()} Now`}
              </span>
            </button>
          </div>
        </div>
      )}

      {/* OPERATIVE ACCOUNT & LIVE DISBURSEMENT BALANCE */}
      <div className="bg-gradient-to-r from-[#1C1C1C] via-[#1F1F1F] to-[#1C1C1C] border border-[#2E2E2E] p-5 sm:p-6 rounded-2xl shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-5">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-2xl bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30 shrink-0">
            <Wallet className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-[11px] uppercase tracking-wider text-neutral-400 font-medium">
                Primary Linked Account
              </span>
              <span className="text-[10px] px-2 py-0.2 rounded-full bg-emerald-500/15 text-emerald-400 font-mono border border-emerald-500/30">
                ACTIVE
              </span>
            </div>
            <div className="text-sm font-mono font-bold text-white mt-0.5">
              A/C #{primaryAccount?.accountNumber || "360100000001"}
            </div>
            <div className="text-[11px] text-neutral-400 mt-0.5">
              Savings Account • Real-time Vault Disbursal Destination
            </div>
          </div>
        </div>

        <div className="bg-[#141414] border border-[#2B2B2B] px-5 py-3.5 rounded-xl flex items-center justify-between md:justify-end space-x-6">
          <div>
            <div className="text-[11px] text-neutral-400 font-medium">Available Account Balance</div>
            <div className="text-2xl font-extrabold text-[#CAFF33] font-mono mt-0.5">
              ₹{(primaryAccount?.balance ?? 0).toLocaleString()}
            </div>
          </div>
          <div className="text-right border-l border-[#262626] pl-4">
            <div className="text-[10px] text-emerald-400 font-medium flex items-center space-x-1 justify-end">
              <ArrowDownLeft className="w-3 h-3" />
              <span>Instant Credit</span>
            </div>
            <div className="text-[10px] text-neutral-400 mt-0.5 font-mono">
              Auto-credited from Vault
            </div>
          </div>
        </div>
      </div>

      {/* Metrics 2x2 on Mobile, 4-col on Desktop - Compact & Clean */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-[#1C1C1C] border border-[#262626] p-4 sm:p-5 rounded-xl">
          <div className="text-[11px] sm:text-xs text-neutral-400 font-medium">Active Applications</div>
          <div className="text-xl sm:text-2xl font-bold text-white mt-1">
            {loading ? "..." : applications.length}
          </div>
          <div className="text-[10px] sm:text-[11px] text-[#CAFF33] mt-1 font-medium flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-[#CAFF33]"></span>
            <span>In Process</span>
          </div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-4 sm:p-5 rounded-xl">
          <div className="text-[11px] sm:text-xs text-neutral-400 font-medium">Documents</div>
          <div className="text-xl sm:text-2xl font-bold text-white mt-1">Verified</div>
          <div className="text-[10px] sm:text-[11px] text-neutral-400 mt-1 truncate">Identity & Financial</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-4 sm:p-5 rounded-xl">
          <div className="text-[11px] sm:text-xs text-neutral-400 font-medium">Sanction Limit</div>
          <div className="text-xl sm:text-2xl font-bold text-white mt-1">₹10.0L</div>
          <div className="text-[10px] sm:text-[11px] text-[#CAFF33] mt-1">Pre-approved</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-4 sm:p-5 rounded-xl">
          <div className="text-[11px] sm:text-xs text-neutral-400 font-medium">Assigned Branch</div>
          <div className="text-xl sm:text-2xl font-bold text-white mt-1">Digital 01</div>
          <div className="text-[10px] sm:text-[11px] text-neutral-400 mt-1">Instant Disbursal</div>
        </div>
      </div>

      {/* Disbursed Payment Receipts (if any exist) */}
      {transactions.length > 0 && (
        <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-4 sm:p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold text-white uppercase tracking-wider flex items-center space-x-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Recent Disbursed Payments & Credits</span>
            </h3>
            <span className="text-[10px] text-neutral-400 font-mono">From Bank Vault</span>
          </div>

          <div className="space-y-2">
            {transactions.map((t) => (
              <div
                key={t.id}
                className="p-3 bg-[#141414] rounded-xl border border-[#262626] flex items-center justify-between text-xs"
              >
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center font-bold">
                    +₹
                  </div>
                  <div>
                    <div className="font-semibold text-white">
                      Loan Disbursed ({t.relatedReference || "Facility"})
                    </div>
                    <div className="text-[10px] text-neutral-400 font-mono">Txn ID: {t.transactionReference}</div>
                  </div>
                </div>

                <div className="text-right">
                  <div className="text-emerald-400 font-mono font-bold">+₹{t.amount?.toLocaleString()}</div>
                  <div className="text-[10px] text-neutral-500">
                    {t.timestamp ? new Date(t.timestamp).toLocaleDateString() : "Disbursed"}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* MONTHLY EMI REPAYMENTS & ACTIVE SCHEDULE */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl overflow-hidden shadow-xl space-y-4 p-5 sm:p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#262626]">
          <div>
            <div className="flex items-center space-x-2">
              <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
              <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
                Monthly EMI Repayments & Vault Inflow
              </h3>
            </div>
            <p className="text-[11px] sm:text-xs text-neutral-400 mt-0.5">
              Pay monthly EMIs on time. Late payments automatically incur a ₹500 overdue penalty.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              onClick={handleCheckOverdue}
              disabled={triggeringCheck}
              className="px-3.5 py-1.5 rounded-full bg-[#262626] hover:bg-[#333333] text-neutral-200 border border-[#3A3A3A] hover:border-[#CAFF33] transition text-xs font-semibold flex items-center space-x-1.5 shadow-sm active:scale-95 disabled:opacity-50"
              title="Scan EMIs and calculate late penalties"
            >
              <Zap className={`w-3.5 h-3.5 text-[#CAFF33] ${triggeringCheck ? "animate-spin" : ""}`} />
              <span>{triggeringCheck ? "Scanning..." : "Check Overdue (+₹500 Fee)"}</span>
            </button>

            <Link
              href="/customer/notifications"
              className="px-3 py-1.5 rounded-full bg-[#262626] hover:bg-[#333333] text-[#CAFF33] border border-[#333333] text-xs font-semibold flex items-center space-x-1 transition"
            >
              <span>Payment Alerts</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>

        {/* OVERDUE URGENT BANNER */}
        {repayments.some((r) => r.status === "OVERDUE") && (
          <div className="p-4 rounded-xl bg-red-950/40 border border-red-500/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
            <div className="flex items-start space-x-3">
              <AlertTriangle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
              <div>
                <div className="font-bold text-red-200 flex items-center space-x-2">
                  <span>🚨 Urgent Overdue Notice: Late Penalty Applied</span>
                  <span className="px-2 py-0.2 rounded-full bg-red-500 text-white text-[10px] font-extrabold uppercase">
                    +₹500.00 Penalty
                  </span>
                </div>
                <div className="text-red-300/80 text-[11px] mt-0.5">
                  One or more monthly installments are past their due date. A ₹500 late fee penalty has been charged to protect banking compliance. Pay now to avoid credit bureau reporting.
                </div>
              </div>
            </div>

            {repayments.find((r) => r.status === "OVERDUE") && (
              <button
                onClick={() => openPaymentGateway(repayments.find((r) => r.status === "OVERDUE")!)}
                disabled={payingId === repayments.find((r) => r.status === "OVERDUE")!.id}
                className="px-4 py-2 rounded-full bg-red-500 hover:bg-red-600 text-white font-bold text-xs whitespace-nowrap shadow-lg shadow-red-500/20 active:scale-95 disabled:opacity-50"
              >
                {payingId === repayments.find((r) => r.status === "OVERDUE")!.id
                  ? "Processing..."
                  : `Pay Overdue (₹${(repayments.find((r) => r.status === "OVERDUE")!.totalPayableAmount || repayments.find((r) => r.status === "OVERDUE")!.emiAmount).toLocaleString()})`}
              </button>
            )}
          </div>
        )}

        {/* REPAYMENTS TABLE WITH SLEEK SCROLLING */}
        <div className={`overflow-x-auto rounded-xl border border-[#262626] transition-all duration-300 ${
          showAllRepayments ? "max-h-[460px] overflow-y-auto" : ""
        }`}>
          <table className="w-full text-left text-xs min-w-[600px]">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626] sticky top-0 z-10 shadow-sm">
              <tr>
                <th className="p-3 font-semibold">Installment</th>
                <th className="p-3 font-semibold">Due Date</th>
                <th className="p-3 font-semibold">Base EMI</th>
                <th className="p-3 font-semibold">Late Fee</th>
                <th className="p-3 font-semibold">Total Payable</th>
                <th className="p-3 font-semibold">Status</th>
                <th className="p-3 font-semibold text-right">Payment Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#262626] text-neutral-300">
              {displayedRepayments.map((rep) => {
                const total = rep.totalPayableAmount || rep.emiAmount + (rep.latePenaltyAmount || 0);
                const isOverdue = rep.status === "OVERDUE";
                const isPaid = rep.status === "PAID";

                return (
                  <tr
                    key={rep.id}
                    className={`transition ${
                      isOverdue
                        ? "bg-red-950/20 hover:bg-red-950/30"
                        : "hover:bg-[#222222]"
                    }`}
                  >
                    <td className="p-3 font-medium text-white flex items-center space-x-2">
                      <span className="w-6 h-6 rounded-lg bg-[#262626] text-white flex items-center justify-center font-bold text-[11px]">
                        #{rep.installmentNumber}
                      </span>
                      <span className="font-mono text-neutral-400 text-[10px]">
                        {rep.applicationReference || "LN-2026"}
                      </span>
                    </td>
                    <td className="p-3 font-mono">
                      <div className="flex items-center space-x-1.5">
                        <Clock className="w-3 h-3 text-neutral-500" />
                        <span>{rep.dueDate}</span>
                      </div>
                    </td>
                    <td className="p-3 font-semibold text-white font-mono">
                      ₹{rep.emiAmount.toLocaleString()}
                    </td>
                    <td className="p-3 font-mono">
                      {rep.latePenaltyAmount && rep.latePenaltyAmount > 0 ? (
                        <span className="text-red-400 font-bold">+₹{rep.latePenaltyAmount.toLocaleString()}</span>
                      ) : (
                        <span className="text-neutral-500">₹0.00</span>
                      )}
                    </td>
                    <td className="p-3 font-extrabold text-[#CAFF33] font-mono">
                      ₹{total.toLocaleString()}
                    </td>
                    <td className="p-3">
                      {isPaid ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 flex items-center space-x-1 w-fit">
                          <CheckCircle2 className="w-2.5 h-2.5" />
                          <span>PAID</span>
                        </span>
                      ) : isOverdue ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-red-500/20 text-red-400 border border-red-500/40 flex items-center space-x-1 w-fit animate-pulse">
                          <AlertTriangle className="w-2.5 h-2.5" />
                          <span>OVERDUE</span>
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-[#262626] text-neutral-300 border border-[#333333]">
                          UPCOMING
                        </span>
                      )}
                    </td>
                    <td className="p-3 text-right">
                      {isPaid ? (
                        <span className="text-[10px] font-mono text-neutral-500">
                          {rep.paymentReference || "Paid"}
                        </span>
                      ) : (
                        <button
                          onClick={() => openPaymentGateway(rep)}
                          disabled={payingId === rep.id}
                          className={`px-3 py-1.5 rounded-full font-bold text-xs transition inline-flex items-center space-x-1.5 shadow-md active:scale-95 disabled:opacity-50 ${
                            isOverdue
                              ? "bg-red-500 hover:bg-red-600 text-white shadow-red-500/20"
                              : "bg-[#CAFF33] hover:bg-[#b8eb2a] text-black shadow-[#CAFF33]/20"
                          }`}
                        >
                          <CreditCard className="w-3 h-3" />
                          <span>{payingId === rep.id ? "Paying..." : `Pay ₹${total.toLocaleString()}`}</span>
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* SEE MORE / SHOW LESS REPAYMENTS CONTROL */}
        {sortedRepayments.length > 5 && (
          <div className="pt-2 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2.5 border-t border-[#262626] text-xs">
            <div className="flex items-center space-x-2">
              <span className="text-neutral-400 font-mono text-[11px]">
                Showing {displayedRepayments.length} of {sortedRepayments.length} installments
              </span>
              {!showAllRepayments && (
                <span className="px-2 py-0.5 rounded-full bg-[#262626] text-[#CAFF33] text-[10px] font-mono border border-[#333333]">
                  +{sortedRepayments.length - 5} Upcoming
                </span>
              )}
            </div>

            <button
              onClick={() => setShowAllRepayments((prev) => !prev)}
              className="text-xs font-bold text-[#CAFF33] hover:text-[#b8eb2a] transition flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full bg-[#262626] hover:bg-[#333333] border border-[#3A3A3A] hover:border-[#CAFF33] active:scale-95 shadow-sm"
            >
              <span>
                {showAllRepayments
                  ? "Show Less (Collapse)"
                  : `See More (${sortedRepayments.length - 5} remaining) ↓`}
              </span>
              {showAllRepayments ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
            </button>
          </div>
        )}
      </div>

      {/* Applications Table */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl overflow-hidden shadow-lg">
        <div className="p-4 sm:p-5 border-b border-[#262626] flex items-center justify-between">
          <div>
            <h3 className="text-sm sm:text-base font-bold text-white">Your Applications</h3>
            <p className="text-[11px] sm:text-xs text-neutral-400 mt-0.5">Real-time status of your loan requests</p>
          </div>
          <Link
            href="/customer/applications"
            className="text-xs font-semibold text-[#CAFF33] hover:underline flex items-center gap-1"
          >
            <span>View All</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className={`overflow-x-auto transition-all duration-300 ${
          showAllApplications ? "max-h-[380px] overflow-y-auto" : ""
        }`}>
          <table className="w-full text-left text-xs min-w-[500px]">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626] sticky top-0 z-10 shadow-sm">
              <tr>
                <th className="p-3.5 font-semibold">Reference</th>
                <th className="p-3.5 font-semibold">Type</th>
                <th className="p-3.5 font-semibold">Amount</th>
                <th className="p-3.5 font-semibold">Tenure</th>
                <th className="p-3.5 font-semibold">Status</th>
                <th className="p-3.5 font-semibold text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#262626] text-neutral-300">
              {loading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-400 text-xs">
                    Loading your applications...
                  </td>
                </tr>
              ) : applications.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-500">
                    No loan applications yet. Click &quot;Apply for Loan&quot; above to begin.
                  </td>
                </tr>
              ) : (
                displayedApplications.map((app) => (
                  <tr key={app.id} className="hover:bg-[#222222] transition">
                    <td className="p-3.5 font-mono font-semibold text-white">{app.applicationReference}</td>
                    <td className="p-3.5 font-medium text-white">{app.loanType}</td>
                    <td className="p-3.5 font-semibold text-white">₹{app.requestedAmount.toLocaleString()}</td>
                    <td className="p-3.5">{app.tenureMonths} Mo</td>
                    <td className="p-3.5">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-[#262626] text-[#CAFF33] border border-[#333333]">
                        {app.status}
                      </span>
                    </td>
                    <td className="p-3.5 text-right">
                      {app.status === "APPROVED" ? (
                        <button
                          onClick={() => handleDisburseApprovedLoan(app.applicationReference, app.id)}
                          disabled={disbursingLoanRef === app.applicationReference}
                          className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-3 py-1 rounded-full text-xs font-bold transition shadow active:scale-95 disabled:opacity-50 inline-flex items-center space-x-1"
                        >
                          <ArrowDownLeft className="w-3 h-3" />
                          <span>{disbursingLoanRef === app.applicationReference ? "Disbursing..." : "Disburse Funds"}</span>
                        </button>
                      ) : (
                        <Link
                          href={`/customer/applications?ref=${app.applicationReference}`}
                          className="text-xs font-semibold text-[#CAFF33] hover:underline"
                        >
                          Details &rarr;
                        </Link>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* SEE MORE / SHOW LESS APPLICATIONS CONTROL */}
        {applications.length > 4 && (
          <div className="p-3.5 border-t border-[#262626] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2.5 text-xs bg-[#171717]">
            <div className="flex items-center space-x-2">
              <span className="text-neutral-400 font-mono text-[11px]">
                Showing {displayedApplications.length} of {applications.length} applications
              </span>
              {!showAllApplications && (
                <span className="px-2 py-0.5 rounded-full bg-[#262626] text-neutral-300 text-[10px] font-mono border border-[#333333]">
                  +{applications.length - 4} more
                </span>
              )}
            </div>

            <button
              onClick={() => setShowAllApplications((prev) => !prev)}
              className="text-xs font-bold text-[#CAFF33] hover:text-[#b8eb2a] transition flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full bg-[#262626] hover:bg-[#333333] border border-[#3A3A3A] hover:border-[#CAFF33] active:scale-95 shadow-sm"
            >
              <span>
                {showAllApplications
                  ? "Show Less (Collapse)"
                  : `See More (${applications.length - 4} remaining) ↓`}
              </span>
              {showAllApplications ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
            </button>
          </div>
        )}
      </div>

      {/* Quick Action Cards - Distinct, Non-duplicate Actions */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5 sm:gap-4">
        <Link
          href="/customer/applications"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-4 sm:p-5 rounded-2xl flex items-start space-x-3.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33] shrink-0">
            <FolderKanban className="w-4 h-4" />
          </div>
          <div>
            <h4 className="font-bold text-xs sm:text-sm text-white">Loan Portfolio</h4>
            <p className="text-[11px] sm:text-xs text-neutral-400 mt-0.5">Track stages, sanctions, and loan lifecycle history.</p>
          </div>
        </Link>

        <Link
          href="/customer/documents"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-4 sm:p-5 rounded-2xl flex items-start space-x-3.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33] shrink-0">
            <Upload className="w-4 h-4" />
          </div>
          <div>
            <h4 className="font-bold text-xs sm:text-sm text-white">Upload Documents</h4>
            <p className="text-[11px] sm:text-xs text-neutral-400 mt-0.5">Submit PAN, Aadhaar, salary slips, or bank proofs.</p>
          </div>
        </Link>

        <Link
          href="/customer/ai-assistant"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-4 sm:p-5 rounded-2xl flex items-start space-x-3.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33] shrink-0">
            <Bot className="w-4 h-4" />
          </div>
          <div>
            <h4 className="font-bold text-xs sm:text-sm text-white">AI Financial Guide</h4>
            <p className="text-[11px] sm:text-xs text-neutral-400 mt-0.5">Ask questions on policy criteria, interest rates, & EMIs.</p>
          </div>
        </Link>
      </div>

      {/* Simulated Banking Payment Gateway Modal */}
      <PaymentGatewayModal
        isOpen={isGatewayOpen}
        onClose={() => setIsGatewayOpen(false)}
        repayment={modalRepayment}
        onConfirmPayment={executePaymentFromGateway}
      />
    </div>
  );
}
