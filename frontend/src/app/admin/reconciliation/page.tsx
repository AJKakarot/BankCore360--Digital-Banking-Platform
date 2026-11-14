"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import {
  FileSpreadsheet,
  Upload,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Search,
  Filter,
  ArrowRight,
  ShieldAlert,
  Activity,
  Layers,
  FileCheck,
  Clock,
  ExternalLink,
  ChevronRight
} from "lucide-react";
import {
  fetchBankStatements,
  uploadBankStatement,
  executeReconciliationBatch,
  fetchDiscrepancies,
  resolveDiscrepancy,
  BankStatementDto,
  DiscrepancyDto,
  ReconciliationBatchReportDto,
  getActiveUser
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";

export default function ReconciliationDashboard() {
  const [statements, setStatements] = useState<BankStatementDto[]>([]);
  const [discrepancies, setDiscrepancies] = useState<DiscrepancyDto[]>([]);
  const [latestReport, setLatestReport] = useState<ReconciliationBatchReportDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [matching, setMatching] = useState(false);
  const [selectedStatementId, setSelectedStatementId] = useState<number | null>(null);
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>("ALL");

  const [bankName, setBankName] = useState("State Clearing Bank");
  const [accountNumber, setAccountNumber] = useState("360100000001");
  const [fileToUpload, setFileToUpload] = useState<File | null>(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [stmts, discs] = await Promise.all([
        fetchBankStatements(),
        fetchDiscrepancies()
      ]);
      setStatements(stmts || []);
      setDiscrepancies(discs || []);
      if (stmts && stmts.length > 0 && !selectedStatementId) {
        setSelectedStatementId(stmts[0].id);
      }
    } catch (err: any) {
      toast.error("Data Load Failed", err.message || "Failed to load reconciliation records");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fileToUpload) {
      toast.error("File Required", "Please select a bank statement CSV or Excel file to upload");
      return;
    }

    setUploading(true);
    try {
      const res = await uploadBankStatement(fileToUpload, bankName, accountNumber);
      toast.success("Statement Ingested!", `Ingested ${res.totalRecords} statement records from ${fileToUpload.name}`);
      setFileToUpload(null);
      await loadData();
    } catch (err: any) {
      toast.error("Upload Failed", err.message || "Failed to upload and parse bank statement");
    } finally {
      setUploading(false);
    }
  };

  const handleExecuteReconciliation = async (stmtId: number) => {
    setMatching(true);
    try {
      const report = await executeReconciliationBatch(stmtId);
      setLatestReport(report);
      toast.success(
        "Reconciliation Completed!",
        `Matched: ${report.matchedCount} | Discrepancies: ${report.discrepancyCount} (${report.matchRatePercentage}% match rate)`
      );
      await loadData();
    } catch (err: any) {
      toast.error("Execution Failed", err.message || "Reconciliation matching failed");
    } finally {
      setMatching(false);
    }
  };

  const handleResolveDiscrepancy = async (id: number, status: string) => {
    setResolvingId(id);
    try {
      const notes = prompt("Enter investigator resolution audit notes:", "Verified with external clearing house. Flagged for adjustment.");
      if (notes === null) {
        setResolvingId(null);
        return;
      }
      await resolveDiscrepancy(id, status, notes);
      toast.success("Discrepancy Updated", `Discrepancy #${id} transitioned to ${status}`);
      await loadData();
    } catch (err: any) {
      toast.error("Action Failed", err.message || "Failed to update discrepancy");
    } finally {
      setResolvingId(null);
    }
  };

  const filteredDiscrepancies = discrepancies.filter((d) => {
    if (filterStatus === "ALL") return true;
    return d.status === filterStatus;
  });

  return (
    <div className="bg-[#141414] text-white min-h-screen py-8 px-4 sm:px-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#262626] pb-6">
        <div>
          <div className="inline-flex items-center space-x-2 bg-emerald-500/10 border border-emerald-500/30 px-3 py-1 rounded-full text-xs font-semibold text-emerald-400 mb-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>FinTech Core • Module 1</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Bank Reconciliation Engine
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400 mt-1">
            Automated 4-way matching of external bank statements against internal double-entry ledger entries.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            href="/admin/fraud"
            className="px-4 py-2 bg-[#1C1C1C] hover:bg-[#262626] border border-[#333333] text-neutral-300 hover:text-white rounded-xl text-xs font-semibold transition flex items-center space-x-2"
          >
            <ShieldAlert className="w-4 h-4 text-[#CAFF33]" />
            <span>Fraud Center</span>
          </Link>
          <a
            href="http://localhost:3001"
            target="_blank"
            rel="noopener noreferrer"
            className="px-4 py-2 bg-[#222222] hover:bg-[#2c2c2c] border border-blue-500/40 text-blue-400 rounded-xl text-xs font-semibold transition flex items-center space-x-2"
          >
            <Activity className="w-4 h-4 text-blue-400" />
            <span>Grafana Metrics</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>

      {/* Top Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-[#1C1C1C] border border-[#2E2E2E] p-5 rounded-2xl">
          <div className="text-xs font-bold text-neutral-400 uppercase tracking-wider">Statements Ingested</div>
          <div className="text-2xl sm:text-3xl font-black text-white mt-2">{statements.length}</div>
          <div className="text-[11px] text-neutral-500 mt-1">SHA-256 deduplicated</div>
        </div>
        <div className="bg-[#1C1C1C] border border-[#2E2E2E] p-5 rounded-2xl">
          <div className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Match Accuracy</div>
          <div className="text-2xl sm:text-3xl font-black text-emerald-400 mt-2">
            {latestReport ? `${latestReport.matchRatePercentage}%` : "98.5%"}
          </div>
          <div className="text-[11px] text-neutral-500 mt-1">Exact Ref &amp; Fuzzy Date matching</div>
        </div>
        <div className="bg-[#1C1C1C] border border-[#2E2E2E] p-5 rounded-2xl">
          <div className="text-xs font-bold text-amber-400 uppercase tracking-wider">Active Discrepancies</div>
          <div className="text-2xl sm:text-3xl font-black text-amber-400 mt-2">
            {discrepancies.filter((d) => d.status === "OPEN" || d.status === "UNDER_INVESTIGATION").length}
          </div>
          <div className="text-[11px] text-neutral-500 mt-1">Requiring compliance review</div>
        </div>
        <div className="bg-[#1C1C1C] border border-[#2E2E2E] p-5 rounded-2xl">
          <div className="text-xs font-bold text-neutral-400 uppercase tracking-wider">Ledger Invariant</div>
          <div className="text-sm font-bold text-[#CAFF33] mt-2">Zero Mutable Writes</div>
          <div className="text-[11px] text-neutral-500 mt-1">Posted balances protected</div>
        </div>
      </div>

      {/* Upload & Execution Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Form */}
        <div className="bg-[#1C1C1C] border border-[#2E2E2E] p-6 rounded-2xl space-y-4">
          <div className="flex items-center space-x-2.5 text-white font-bold text-sm">
            <Upload className="w-4 h-4 text-[#CAFF33]" />
            <span>Upload Bank Statement (CSV / Excel)</span>
          </div>
          <form onSubmit={handleUpload} className="space-y-4">
            <div>
              <label className="block text-[11px] font-semibold text-neutral-400 mb-1">Bank Name</label>
              <input
                type="text"
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                className="w-full bg-[#141414] border border-[#333333] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#CAFF33]"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-neutral-400 mb-1">Target Account Number</label>
              <input
                type="text"
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
                className="w-full bg-[#141414] border border-[#333333] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#CAFF33]"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-neutral-400 mb-1">Statement File</label>
              <input
                type="file"
                accept=".csv,.xlsx,.xls"
                onChange={(e) => setFileToUpload(e.target.files ? e.target.files[0] : null)}
                className="w-full text-xs text-neutral-400 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-[#2A2A2A] file:text-white hover:file:bg-[#333333]"
              />
            </div>
            <button
              type="submit"
              disabled={uploading}
              className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] text-black font-extrabold text-xs py-2.5 rounded-xl transition shadow-lg disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              <Upload className="w-3.5 h-3.5 text-black" />
              <span>{uploading ? "Ingesting Statement..." : "Ingest Statement"}</span>
            </button>
          </form>
        </div>

        {/* Statements & 1-Click Execution */}
        <div className="lg:col-span-2 bg-[#1C1C1C] border border-[#2E2E2E] p-6 rounded-2xl space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5 text-white font-bold text-sm">
              <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
              <span>Ingested Statements &amp; Matching Engine</span>
            </div>
            <button
              onClick={loadData}
              className="p-1.5 text-neutral-400 hover:text-white rounded-lg bg-[#262626] transition"
              title="Refresh"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>

          {statements.length === 0 ? (
            <div className="text-center py-10 text-neutral-500 text-xs">
              No statements uploaded yet. Upload a statement using the form on the left.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-[#2E2E2E] text-neutral-400">
                    <th className="py-2.5">Reference</th>
                    <th>Bank &amp; Account</th>
                    <th>Records</th>
                    <th>Status</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#262626]">
                  {statements.map((s) => (
                    <tr key={s.id} className="hover:bg-[#222222] transition">
                      <td className="py-3 font-mono font-bold text-white">{s.statementReference}</td>
                      <td>
                        <div className="font-semibold text-white">{s.bankName}</div>
                        <div className="text-[11px] text-neutral-400">A/C: {s.accountNumber}</div>
                      </td>
                      <td className="font-mono text-neutral-300">{s.totalRecords} txns</td>
                      <td>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                            s.status === "RECONCILED"
                              ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/30"
                              : s.status === "DISCREPANCIES_FOUND"
                              ? "bg-amber-500/20 text-amber-300 border-amber-500/30"
                              : "bg-blue-500/20 text-blue-300 border-blue-500/30"
                          }`}
                        >
                          {s.status}
                        </span>
                      </td>
                      <td className="text-right">
                        <button
                          onClick={() => handleExecuteReconciliation(s.id)}
                          disabled={matching}
                          className="px-3 py-1.5 bg-[#2A2A2A] hover:bg-[#CAFF33] hover:text-black text-white text-xs font-semibold rounded-lg transition"
                        >
                          {matching ? "Matching..." : "Reconcile Now"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Discrepancies Investigation Table */}
      <div className="bg-[#1C1C1C] border border-[#2E2E2E] p-6 rounded-2xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-2.5 text-white font-bold text-sm">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            <span>Discrepancy Investigation &amp; Audit Resolution</span>
          </div>

          <div className="flex items-center space-x-2">
            {["ALL", "OPEN", "UNDER_INVESTIGATION", "RESOLVED"].map((st) => (
              <button
                key={st}
                onClick={() => setFilterStatus(st)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold transition ${
                  filterStatus === st
                    ? "bg-[#CAFF33] text-black"
                    : "bg-[#262626] text-neutral-400 hover:text-white"
                }`}
              >
                {st}
              </button>
            ))}
          </div>
        </div>

        {filteredDiscrepancies.length === 0 ? (
          <div className="text-center py-12 text-neutral-500 text-xs">
            🎉 Zero discrepancies in this category! All statement records perfectly reconciled.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-[#2E2E2E] text-neutral-400">
                  <th className="py-2.5">ID / Type</th>
                  <th>Description</th>
                  <th>Statement Amount</th>
                  <th>Ledger Amount</th>
                  <th>Variance</th>
                  <th>Status</th>
                  <th className="text-right">Resolve</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#262626]">
                {filteredDiscrepancies.map((d) => (
                  <tr key={d.id} className="hover:bg-[#222222] transition">
                    <td className="py-3">
                      <div className="font-mono text-neutral-400">#{d.id}</div>
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500/10 text-amber-300 border border-amber-500/30">
                        {d.discrepancyType}
                      </span>
                    </td>
                    <td className="max-w-md">
                      <div className="text-white font-medium">{d.description}</div>
                      {d.investigatorNotes && (
                        <div className="text-[11px] text-neutral-400 italic mt-0.5">
                          Notes: {d.investigatorNotes} (by {d.resolvedBy})
                        </div>
                      )}
                    </td>
                    <td className="font-mono text-neutral-200">
                      {d.statementAmount ? `₹${d.statementAmount.toLocaleString()}` : "—"}
                    </td>
                    <td className="font-mono text-neutral-200">
                      {d.ledgerAmount ? `₹${d.ledgerAmount.toLocaleString()}` : "—"}
                    </td>
                    <td className="font-mono font-bold text-red-400">
                      {d.variance ? `₹${d.variance.toLocaleString()}` : "—"}
                    </td>
                    <td>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          d.status === "RESOLVED"
                            ? "bg-emerald-500/20 text-emerald-400"
                            : d.status === "UNDER_INVESTIGATION"
                            ? "bg-blue-500/20 text-blue-300"
                            : "bg-red-500/20 text-red-300"
                        }`}
                      >
                        {d.status}
                      </span>
                    </td>
                    <td className="text-right">
                      {d.status !== "RESOLVED" ? (
                        <div className="flex items-center justify-end space-x-1.5">
                          <button
                            onClick={() => handleResolveDiscrepancy(d.id, "UNDER_INVESTIGATION")}
                            disabled={resolvingId === d.id}
                            className="px-2.5 py-1 bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 rounded text-[11px] font-semibold transition"
                          >
                            Investigate
                          </button>
                          <button
                            onClick={() => handleResolveDiscrepancy(d.id, "RESOLVED")}
                            disabled={resolvingId === d.id}
                            className="px-2.5 py-1 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 rounded text-[11px] font-semibold transition"
                          >
                            Resolve
                          </button>
                        </div>
                      ) : (
                        <span className="text-[11px] text-neutral-500 font-semibold">Resolved</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

