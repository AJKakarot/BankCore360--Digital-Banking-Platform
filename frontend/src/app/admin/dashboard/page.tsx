"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Users,
  FileText,
  ShieldCheck,
  BookOpen,
  ArrowRight,
  RotateCcw,
  Radio,
  Landmark,
  PlusCircle,
  RefreshCw,
  TrendingDown,
  TrendingUp,
  X,
  CheckCircle2,
  Lock
} from "lucide-react";
import {
  getActiveUser,
  clearSession,
  fetchVaultStatus,
  injectVaultCapital,
  VaultStatus,
  VaultTransaction
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";

export default function AdminDashboard() {
  const router = useRouter();
  const [vaultStatus, setVaultStatus] = useState<VaultStatus | null>(null);
  const [loadingVault, setLoadingVault] = useState(true);
  const [injectModalOpen, setInjectModalOpen] = useState(false);
  const [injectAmount, setInjectAmount] = useState<number>(1000000); // 10 Lakh default
  const [injectRemarks, setInjectRemarks] = useState("Q1 Treasury Capital Inflow");
  const [submittingInject, setSubmittingInject] = useState(false);

  useEffect(() => {
    const user = getActiveUser();
    if (!user || user.role !== "ADMIN") {
      router.push("/login?role=ADMIN");
      return;
    }
    loadVaultData();
  }, [router]);

  const loadVaultData = async () => {
    setLoadingVault(true);
    try {
      const data = await fetchVaultStatus();
      setVaultStatus(data);
    } catch {
      // Graceful fallback
    } finally {
      setLoadingVault(false);
    }
  };

  const handleInjectCapital = async (e: React.FormEvent) => {
    e.preventDefault();
    if (injectAmount < 1000) {
      toast.error("Invalid Amount", "Minimum injection amount is ₹1,000");
      return;
    }

    setSubmittingInject(true);
    const toastId = toast.loading("Injecting Capital...", `Adding ₹${injectAmount.toLocaleString()} to Vault`);

    try {
      await injectVaultCapital(injectAmount, injectRemarks);
      toast.dismiss(toastId);
      toast.success("Capital Injected Successfully!", `Vault increased by ₹${injectAmount.toLocaleString()}`);
      setInjectModalOpen(false);
      loadVaultData();
    } catch (err: any) {
      toast.dismiss(toastId);
      toast.error("Injection Failed", err.message || "Could not inject capital");
    } finally {
      setSubmittingInject(false);
    }
  };

  const handleRestartDemo = () => {
    clearSession();
    router.push("/login?role=CUSTOMER");
  };

  // Convert numbers to Lakhs format (₹100.0L)
  const formatLakhs = (val: number = 0) => {
    const inLakhs = val / 100000;
    if (inLakhs >= 100) {
      return `₹${(inLakhs / 100).toFixed(2)} Cr`;
    }
    return `₹${inLakhs.toFixed(2)} Lakh`;
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 sm:space-y-8 py-4 sm:py-6 px-3.5 sm:px-6 max-w-6xl mx-auto animate-fade-in">
      {/* Banner */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-7 flex flex-col md:flex-row md:items-center justify-between gap-5 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-[11px] font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Admin Console</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">Admin Overview & Treasury</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Central monitor for Bank Core ₹100 Lakh Vault, loan disbursals, audit trails, and role management.
          </p>
        </div>

        <div className="flex flex-wrap gap-2.5 sm:gap-3">
          <button
            onClick={() => setInjectModalOpen(true)}
            className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-4 sm:px-5 py-2.5 rounded-full font-bold text-xs shadow-md transition flex items-center space-x-1.5 active:scale-95"
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span>Inject Capital</span>
          </button>
          <button
            onClick={handleRestartDemo}
            className="bg-[#262626] hover:bg-[#333333] text-white px-4 py-2.5 rounded-full font-medium text-xs transition border border-[#333333] flex items-center space-x-1.5 active:scale-95"
          >
            <RotateCcw className="w-3.5 h-3.5 text-[#CAFF33]" />
            <span>Switch to Customer</span>
          </button>
        </div>
      </div>

      {/* CORE BANK VAULT & TREASURY CARD */}
      <div className="bg-gradient-to-br from-[#1C1C1C] to-[#171717] border border-[#2E2E2E] rounded-2xl p-5 sm:p-7 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-[#CAFF33]/5 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#262626] pb-4 mb-5">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-[#262626] border border-[#333333] flex items-center justify-center text-[#CAFF33] shadow-md">
              <Landmark className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-base font-bold text-white">BankCore360 Central Treasury & Vault</h3>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-semibold font-mono">
                  ACTIVE • ACID
                </span>
              </div>
              <p className="text-xs text-neutral-400 mt-0.5">
                Atomic Pessimistic-Lock Reserve Pool. Deducts automatically upon loan approval.
              </p>
            </div>
          </div>

          <button
            onClick={loadVaultData}
            disabled={loadingVault}
            className="p-2 rounded-full bg-[#262626] hover:bg-[#333333] text-neutral-400 hover:text-white transition self-end sm:self-auto border border-[#333333]"
            title="Refresh Vault Status"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loadingVault ? "animate-spin text-[#CAFF33]" : ""}`} />
          </button>
        </div>

        {/* 3 Main Treasury Pillars */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          {/* Total Capital */}
          <div className="bg-[#141414] border border-[#262626] p-4 rounded-xl">
            <div className="text-[11px] text-white font-medium">Total Bank Capital</div>
            <div className="text-2xl font-extrabold text-white mt-1">
              {formatLakhs(vaultStatus?.totalCapital || 10000000)}
            </div>
            <div className="text-[10px] text-white mt-1 font-mono">
              ₹{(vaultStatus?.totalCapital || 10000000).toLocaleString()} Core Base
            </div>
          </div>

          {/* Available Liquidity */}
          <div className="bg-[#141414] border border-[#262626] p-4 rounded-xl">
            <div className="text-[11px] text-white font-medium flex items-center justify-between">
              <span>Available Liquidity</span>
              <span className="w-2 h-2 rounded-full bg-[#CAFF33] animate-pulse"></span>
            </div>
            <div className="text-2xl font-extrabold text-white mt-1">
              {formatLakhs(vaultStatus?.availableLiquidity || 9750000)}
            </div>
            <div className="text-[10px] text-white mt-1 font-mono">
              ₹{(vaultStatus?.availableLiquidity || 9750000).toLocaleString()} Ready to Disburse
            </div>
          </div>

          {/* Total Disbursed */}
          <div className="bg-[#141414] border border-[#262626] p-4 rounded-xl">
            <div className="text-[11px] text-white font-medium flex items-center justify-between">
              <span>Disbursed to Borrowers</span>
              <TrendingDown className="w-3.5 h-3.5 text-white/70" />
            </div>
            <div className="text-2xl font-extrabold text-white mt-1">
              {formatLakhs(vaultStatus?.totalDisbursed || 250000)}
            </div>
            <div className="text-[10px] text-white mt-1 font-mono">
              {vaultStatus?.utilizationPercent ?? 2.5}% Capital Utilization
            </div>
          </div>
        </div>

        {/* Liquidity Utilization Progress Bar */}
        <div className="space-y-1.5 mb-6 bg-[#141414] p-3.5 rounded-xl border border-[#262626]">
          <div className="flex justify-between items-center text-xs">
            <span className="text-neutral-400 font-medium">Liquidity Allocation Ratio</span>
            <span className="font-mono text-[#CAFF33] font-semibold">
              {(vaultStatus?.utilizationPercent ?? 2.5).toFixed(1)}% Disbursed
            </span>
          </div>
          <div className="w-full bg-[#262626] h-2.5 rounded-full overflow-hidden">
            <div
              className="bg-gradient-to-r from-emerald-400 to-[#CAFF33] h-full rounded-full transition-all duration-500"
              style={{ width: `${Math.min(Math.max(vaultStatus?.utilizationPercent ?? 2.5, 3), 100)}%` }}
            />
          </div>
          <div className="flex justify-between text-[10px] text-neutral-500 font-mono">
            <span>₹0 (0%)</span>
            <span>Safety Threshold: 80%</span>
            <span>Total: {formatLakhs(vaultStatus?.totalCapital || 10000000)}</span>
          </div>
        </div>

        {/* Recent Vault Ledger Transactions Table */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">
              Immutable Vault Transaction Ledger ({vaultStatus?.recentTransactions?.length || 0})
            </h4>
            <span className="text-[10px] text-neutral-400 font-mono">Atomic Idempotent Records</span>
          </div>

          <div className="overflow-x-auto rounded-xl border border-[#262626] bg-[#141414]">
            <table className="w-full text-left text-xs min-w-[600px]">
              <thead className="bg-[#1C1C1C] text-neutral-400 border-b border-[#262626] text-[10px] uppercase font-semibold">
                <tr>
                  <th className="p-3">Txn Ref</th>
                  <th className="p-3">Type</th>
                  <th className="p-3">Amount</th>
                  <th className="p-3">Beneficiary / Target</th>
                  <th className="p-3">Vault Remaining</th>
                  <th className="p-3">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#262626] text-neutral-300 font-mono text-[11px]">
                {!vaultStatus?.recentTransactions || vaultStatus.recentTransactions.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="p-6 text-center text-neutral-500">
                      No vault ledger entries recorded yet.
                    </td>
                  </tr>
                ) : (
                  vaultStatus.recentTransactions.map((txn) => (
                    <tr key={txn.id} className="hover:bg-[#1C1C1C] transition">
                      <td className="p-3 text-white font-bold">{txn.transactionReference}</td>
                      <td className="p-3">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            txn.direction === "CREDIT"
                              ? "bg-emerald-950/40 text-emerald-400 border border-emerald-800"
                              : "bg-amber-950/40 text-amber-300 border border-amber-800"
                          }`}
                        >
                          {txn.direction === "CREDIT" ? "+ CAPITAL" : "- DISBURSE"}
                        </span>
                      </td>
                      <td
                        className={`p-3 font-bold ${
                          txn.direction === "CREDIT" ? "text-emerald-400" : "text-[#CAFF33]"
                        }`}
                      >
                        {txn.direction === "CREDIT" ? "+" : "-"}₹{txn.amount.toLocaleString()}
                      </td>
                      <td className="p-3 text-neutral-300 font-sans">
                        <div>{txn.beneficiaryName || "Customer"}</div>
                        <div className="text-[10px] text-neutral-500 font-mono">{txn.beneficiaryAccountNumber || "A/C"}</div>
                      </td>
                      <td className="p-3 text-white">₹{txn.balanceAfter.toLocaleString()}</td>
                      <td className="p-3 text-neutral-400 text-[10px]">
                        {txn.timestamp ? new Date(txn.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "Recent"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Navigation Quick Links Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3.5 sm:gap-4">
        <Link
          href="/admin/users"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-5 rounded-2xl space-y-2.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <Users className="w-4 h-4" />
          </div>
          <h3 className="font-bold text-white text-xs sm:text-sm">User Directory</h3>
          <p className="text-[11px] text-neutral-400">View user accounts and assign roles.</p>
        </Link>

        <Link
          href="/admin/applications"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-5 rounded-2xl space-y-2.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <FileText className="w-4 h-4" />
          </div>
          <h3 className="font-bold text-white text-xs sm:text-sm">Loan Registry</h3>
          <p className="text-[11px] text-neutral-400">Search and review customer loan files.</p>
        </Link>

        <Link
          href="/admin/events"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-5 rounded-2xl space-y-2.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <Radio className="w-4 h-4" />
          </div>
          <h3 className="font-bold text-white text-xs sm:text-sm">Event Stream & Outbox</h3>
          <p className="text-[11px] text-neutral-400">Monitor event outbox and consumer logs.</p>
        </Link>

        <Link
          href="/admin/audit"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-5 rounded-2xl space-y-2.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <ShieldCheck className="w-4 h-4" />
          </div>
          <h3 className="font-bold text-white text-xs sm:text-sm">Audit Trail</h3>
          <p className="text-[11px] text-neutral-400">Surveillance logs of all operations.</p>
        </Link>

        <Link
          href="/admin/policies"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-5 rounded-2xl space-y-2.5 transition hover:-translate-y-0.5"
        >
          <div className="w-9 h-9 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <BookOpen className="w-4 h-4" />
          </div>
          <h3 className="font-bold text-white text-xs sm:text-sm">Policy Manager</h3>
          <p className="text-[11px] text-neutral-400">Credit underwriting policies for AI.</p>
        </Link>
      </div>

      {/* Capital Injection Modal */}
      {injectModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#1C1C1C] border border-[#333333] rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl animate-fade-in">
            <div className="flex items-center justify-between border-b border-[#262626] pb-3">
              <div className="flex items-center space-x-2">
                <Landmark className="w-5 h-5 text-[#CAFF33]" />
                <h3 className="font-bold text-sm text-white">Inject Capital into Bank Vault</h3>
              </div>
              <button
                onClick={() => setInjectModalOpen(false)}
                className="p-1 rounded-lg text-neutral-400 hover:text-white hover:bg-[#262626]"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleInjectCapital} className="space-y-4">
              <div>
                <label className="text-xs text-neutral-400 block mb-1.5 font-medium">
                  Injection Amount (₹)
                </label>
                <div className="relative">
                  <span className="absolute left-3.5 top-2.5 text-neutral-500 font-bold">₹</span>
                  <input
                    type="number"
                    min="1000"
                    step="1000"
                    required
                    value={injectAmount}
                    onChange={(e) => setInjectAmount(Number(e.target.value))}
                    className="w-full pl-8 pr-4 py-2.5 bg-[#141414] border border-[#2E2E2E] rounded-xl text-white font-mono font-bold text-sm focus:outline-none focus:border-[#CAFF33]"
                  />
                </div>
                <div className="flex items-center space-x-2 mt-2">
                  {[500000, 1000000, 2500000, 5000000].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => setInjectAmount(preset)}
                      className="px-2.5 py-1 rounded-lg bg-[#262626] hover:bg-[#333333] text-[10px] font-mono text-[#CAFF33] transition"
                    >
                      +{preset / 100000}L
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs text-neutral-400 block mb-1.5 font-medium">
                  Ledger Remarks / Authorization Note
                </label>
                <input
                  type="text"
                  required
                  value={injectRemarks}
                  onChange={(e) => setInjectRemarks(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-[#141414] border border-[#2E2E2E] rounded-xl text-xs text-white focus:outline-none focus:border-[#CAFF33]"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setInjectModalOpen(false)}
                  className="px-4 py-2 rounded-full bg-[#262626] text-xs font-semibold text-neutral-300 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingInject}
                  className="px-5 py-2 rounded-full bg-[#CAFF33] hover:bg-[#b8eb2a] text-black text-xs font-bold transition shadow-md disabled:opacity-50"
                >
                  {submittingInject ? "Processing..." : "Confirm & Credit Vault"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
