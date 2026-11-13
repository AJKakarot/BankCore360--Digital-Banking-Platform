"use client";

import React, { useState, useEffect } from "react";
import {
  ShieldAlert,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Play,
  RotateCcw,
  Search,
  Filter,
  Eye,
  FileText,
  Clock,
  Zap,
  Activity
} from "lucide-react";
import {
  fetchFraudAlerts,
  updateFraudAlertStatus,
  evaluateFraud,
  FraudAlertItem,
  FraudAssessmentResponse,
  getActiveUser
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";

export default function AdminFraudDashboard() {
  const [alerts, setAlerts] = useState<FraudAlertItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [selectedAlert, setSelectedAlert] = useState<FraudAlertItem | null>(null);
  const [reviewNotes, setReviewNotes] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);

  // Live Simulator state
  const [simAccount, setSimAccount] = useState("360100000001");
  const [simAmount, setSimAmount] = useState("750000");
  const [simType, setSimType] = useState("TRANSFER");
  const [simResult, setSimResult] = useState<FraudAssessmentResponse | null>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);

  const loadAlerts = async () => {
    try {
      const data = await fetchFraudAlerts(statusFilter === "ALL" ? undefined : statusFilter);
      setAlerts(data || []);
    } catch (err: any) {
      toast.error("Failed to load alerts", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAlerts();
  }, [statusFilter]);

  const handleStatusUpdate = async (alertId: number, newStatus: string) => {
    if (!reviewNotes.trim()) {
      toast.error("Validation Error", "Investigation notes are required before updating status.");
      return;
    }
    setIsUpdating(true);
    try {
      const updated = await updateFraudAlertStatus(alertId, newStatus, reviewNotes, "STATUS_CHANGE");
      toast.success("Alert Status Updated", `Alert #${updated.alertReference} transitioned to ${newStatus}.`);
      setSelectedAlert(null);
      setReviewNotes("");
      await loadAlerts();
    } catch (err: any) {
      toast.error("Update Failed", err.message);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleRunEvaluation = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsEvaluating(true);
    setSimResult(null);
    try {
      const res = await evaluateFraud({
        accountNumber: simAccount,
        amount: Number(simAmount),
        transactionType: simType,
      });
      setSimResult(res);
      if (res.decision === "BLOCK") {
        toast.error("🚨 CRITICAL FRAUD DETECTED", `Transaction blocked! Score: ${res.riskScore}/100`);
      } else if (res.decision === "MANUAL_REVIEW") {
        toast.info("⚠️ HELD FOR REVIEW", `Score: ${res.riskScore}/100. High risk detected.`);
      } else {
        toast.success("✅ Transaction Nominal", `Approved with Low/Medium score: ${res.riskScore}/100`);
      }
      await loadAlerts();
    } catch (err: any) {
      toast.error("Evaluation Error", err.message);
    } finally {
      setIsEvaluating(false);
    }
  };

  const getScoreBadge = (score: number) => {
    if (score >= 81) return "bg-red-500/20 text-red-400 border-red-500/40";
    if (score >= 61) return "bg-amber-500/20 text-amber-400 border-amber-500/40";
    if (score >= 31) return "bg-yellow-500/20 text-yellow-300 border-yellow-500/40";
    return "bg-emerald-500/20 text-emerald-400 border-emerald-500/40";
  };

  const pendingCount = alerts.filter((a) => a.status === "PENDING_REVIEW").length;
  const criticalCount = alerts.filter((a) => a.riskLevel === "CRITICAL").length;

  return (
    <div className="bg-[#141414] text-white space-y-6 sm:space-y-8 py-6 px-4 sm:px-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#262626] pb-6">
        <div>
          <div className="inline-flex items-center space-x-2 bg-red-500/10 border border-red-500/30 px-3 py-1 rounded-full text-[11px] font-bold text-red-400">
            <ShieldAlert className="w-3.5 h-3.5" />
            <span>Real-Time Fraud Engine (Module 2)</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight mt-2">
            Compliance & Fraud Monitoring Center
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400 mt-1">
            Pre-settlement velocity scoring, anomaly checks, Redis sliding-window tracking, and Kafka alert streams.
          </p>
        </div>
        <button
          onClick={loadAlerts}
          className="inline-flex items-center space-x-2 bg-[#262626] hover:bg-[#333] text-neutral-200 text-xs font-semibold px-4 py-2.5 rounded-xl border border-[#383838] transition shrink-0"
        >
          <RotateCcw className="w-4 h-4" />
          <span>Refresh Live Alerts</span>
        </button>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-[#1C1C1C] border border-[#2A2A2A] rounded-2xl p-4 sm:p-5 shadow-lg">
          <div className="text-[11px] uppercase tracking-wider text-neutral-400 font-semibold">Total Alerts</div>
          <div className="text-2xl sm:text-3xl font-extrabold text-white mt-1">{alerts.length}</div>
          <div className="text-[10px] text-neutral-400 mt-0.5">Historical flagged attempts</div>
        </div>
        <div className="bg-[#1C1C1C] border border-amber-500/30 rounded-2xl p-4 sm:p-5 shadow-lg">
          <div className="text-[11px] uppercase tracking-wider text-amber-400 font-semibold">Pending Review</div>
          <div className="text-2xl sm:text-3xl font-extrabold text-amber-300 mt-1">{pendingCount}</div>
          <div className="text-[10px] text-neutral-400 mt-0.5">Requires compliance intervention</div>
        </div>
        <div className="bg-[#1C1C1C] border border-red-500/30 rounded-2xl p-4 sm:p-5 shadow-lg">
          <div className="text-[11px] uppercase tracking-wider text-red-400 font-semibold">Critical Threat</div>
          <div className="text-2xl sm:text-3xl font-extrabold text-red-400 mt-1">{criticalCount}</div>
          <div className="text-[10px] text-neutral-400 mt-0.5">Risk score 81–100 (Hard blocked)</div>
        </div>
        <div className="bg-[#1C1C1C] border border-emerald-500/30 rounded-2xl p-4 sm:p-5 shadow-lg">
          <div className="text-[11px] uppercase tracking-wider text-emerald-400 font-semibold">Redis Velocity</div>
          <div className="text-2xl sm:text-3xl font-extrabold text-emerald-300 mt-1">ACTIVE</div>
          <div className="text-[10px] text-neutral-400 mt-0.5">Sliding window rate-limiter</div>
        </div>
      </div>

      {/* Interactive Fraud Rule Simulator */}
      <div className="bg-gradient-to-r from-[#1B1B1B] via-[#202020] to-[#1B1B1B] border border-[#2E2E2E] rounded-2xl p-5 sm:p-6 shadow-xl">
        <div className="flex items-center space-x-2.5">
          <Zap className="w-5 h-5 text-[#CAFF33]" />
          <h2 className="text-base sm:text-lg font-bold text-white">Live Pre-Settlement Fraud Simulator</h2>
        </div>
        <p className="text-xs text-neutral-400 mt-1">
          Execute real-time risk evaluation against active threshold rules, Redis sliding-window counters, and rapid burst detectors.
        </p>

        <form onSubmit={handleRunEvaluation} className="mt-4 grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div>
            <label className="text-[11px] text-neutral-300 block mb-1">Account Number</label>
            <input
              type="text"
              value={simAccount}
              onChange={(e) => setSimAccount(e.target.value)}
              className="w-full bg-[#141414] border border-[#333] rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-[#CAFF33]"
              required
            />
          </div>
          <div>
            <label className="text-[11px] text-neutral-300 block mb-1">Transaction Amount (₹)</label>
            <input
              type="number"
              value={simAmount}
              onChange={(e) => setSimAmount(e.target.value)}
              className="w-full bg-[#141414] border border-[#333] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#CAFF33]"
              required
            />
          </div>
          <div>
            <label className="text-[11px] text-neutral-300 block mb-1">Transaction Type</label>
            <select
              value={simType}
              onChange={(e) => setSimType(e.target.value)}
              className="w-full bg-[#141414] border border-[#333] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#CAFF33]"
            >
              <option value="TRANSFER">TRANSFER</option>
              <option value="WITHDRAWAL">WITHDRAWAL</option>
              <option value="DEPOSIT">DEPOSIT</option>
            </select>
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              disabled={isEvaluating}
              className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] text-black font-extrabold text-xs py-2.5 rounded-xl transition flex items-center justify-center space-x-1.5 disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5 fill-black" />
              <span>{isEvaluating ? "Evaluating..." : "Run Evaluation"}</span>
            </button>
          </div>
        </form>

        {simResult && (
          <div className="mt-4 p-4 rounded-xl bg-[#141414] border border-[#2E2E2E] flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs">
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-mono text-neutral-400">Ref: {simResult.assessmentReference}</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${getScoreBadge(simResult.riskScore)}`}>
                  Score: {simResult.riskScore} / 100 ({simResult.riskLevel})
                </span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                  simResult.decision === "BLOCK" ? "bg-red-500 text-white" :
                  simResult.decision === "MANUAL_REVIEW" ? "bg-amber-500 text-black" : "bg-emerald-500 text-black"
                }`}>
                  Decision: {simResult.decision}
                </span>
              </div>
              <p className="text-neutral-300 mt-1.5">{simResult.assessmentReasons}</p>
            </div>
            {simResult.triggeredRuleCodes && simResult.triggeredRuleCodes.length > 0 && (
              <div className="shrink-0 flex flex-wrap gap-1">
                {simResult.triggeredRuleCodes.map((c) => (
                  <span key={c} className="px-2 py-0.5 rounded bg-[#2A2A2A] text-neutral-300 text-[10px] font-mono">
                    {c}
                  </span>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Alerts Table */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-6 shadow-xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="text-base sm:text-lg font-bold text-white">Suspicious Transaction Alerts</h2>
            <p className="text-xs text-neutral-400">Transactions with Risk Score ≥ 61 queued for manual review.</p>
          </div>
          <div className="flex items-center space-x-2">
            <Filter className="w-3.5 h-3.5 text-neutral-400" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-[#141414] border border-[#333] text-neutral-200 text-xs rounded-xl px-3 py-1.5 focus:outline-none"
            >
              <option value="ALL">All Statuses</option>
              <option value="PENDING_REVIEW">Pending Review</option>
              <option value="UNDER_INVESTIGATION">Under Investigation</option>
              <option value="RESOLVED_CLEARED">Resolved / Cleared</option>
              <option value="CONFIRMED_FRAUD">Confirmed Fraud</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="py-12 text-center text-xs text-neutral-400">Loading live fraud alerts...</div>
        ) : alerts.length === 0 ? (
          <div className="py-12 text-center text-xs text-neutral-400">No fraud alerts matching filter.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-300">
              <thead className="bg-[#141414] text-neutral-400 font-semibold border-b border-[#2A2A2A]">
                <tr>
                  <th className="py-3 px-4">Alert Ref</th>
                  <th className="py-3 px-4">Account</th>
                  <th className="py-3 px-4">Risk Score</th>
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Created</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#262626]">
                {alerts.map((alert) => (
                  <tr key={alert.id} className="hover:bg-[#222] transition">
                    <td className="py-3 px-4 font-mono font-bold text-white">{alert.alertReference}</td>
                    <td className="py-3 px-4 font-mono text-neutral-300">{alert.accountNumber}</td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${getScoreBadge(alert.riskScore)}`}>
                        {alert.riskScore} / 100
                      </span>
                    </td>
                    <td className="py-3 px-4 font-semibold text-neutral-200">{alert.riskLevel}</td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        alert.status === "PENDING_REVIEW" ? "bg-amber-500/20 text-amber-300 border border-amber-500/40" :
                        alert.status === "CONFIRMED_FRAUD" ? "bg-red-500/20 text-red-300 border border-red-500/40" :
                        "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40"
                      }`}>
                        {alert.status.replace("_", " ")}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-neutral-400 text-[11px]">
                      {new Date(alert.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => setSelectedAlert(alert)}
                        className="bg-[#2A2A2A] hover:bg-[#333] text-neutral-200 font-semibold px-3 py-1.5 rounded-lg border border-[#3A3A3A] transition"
                      >
                        Investigate
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Investigation Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4">
          <div className="bg-[#1C1C1C] border border-[#2E2E2E] rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#2A2A2A] pb-3">
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <ShieldAlert className="w-5 h-5 text-amber-400" />
                <span>Investigate Alert {selectedAlert.alertReference}</span>
              </h3>
              <button
                onClick={() => setSelectedAlert(null)}
                className="text-neutral-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <div className="space-y-2 text-xs">
              <div className="grid grid-cols-2 gap-2 bg-[#141414] p-3 rounded-xl border border-[#262626]">
                <div>
                  <span className="text-neutral-400">Account:</span>
                  <div className="font-mono font-bold text-white">{selectedAlert.accountNumber}</div>
                </div>
                <div>
                  <span className="text-neutral-400">Risk Score:</span>
                  <div className="font-bold text-red-400">{selectedAlert.riskScore}/100 ({selectedAlert.riskLevel})</div>
                </div>
              </div>

              <div>
                <label className="text-[11px] font-semibold text-neutral-300 block mb-1">
                  Investigation Remarks & Evidence (Mandatory)
                </label>
                <textarea
                  rows={3}
                  value={reviewNotes}
                  onChange={(e) => setReviewNotes(e.target.value)}
                  placeholder="e.g. Conducted customer verification, confirmed device biometric pass..."
                  className="w-full bg-[#141414] border border-[#333] rounded-xl p-3 text-xs text-white focus:outline-none focus:border-[#CAFF33]"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2">
              <button
                onClick={() => handleStatusUpdate(selectedAlert.id, "RESOLVED_CLEARED")}
                disabled={isUpdating}
                className="bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs py-2.5 rounded-xl transition disabled:opacity-50"
              >
                Clear as Legitimate
              </button>
              <button
                onClick={() => handleStatusUpdate(selectedAlert.id, "CONFIRMED_FRAUD")}
                disabled={isUpdating}
                className="bg-red-600 hover:bg-red-500 text-white font-bold text-xs py-2.5 rounded-xl transition disabled:opacity-50"
              >
                Confirm Fraud & Block
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
