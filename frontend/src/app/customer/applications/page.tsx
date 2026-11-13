"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  ArrowDownLeft
} from "lucide-react";
import { fetchMyApplications, disburseCustomerLoan, LoanApplication, getActiveUser } from "@/lib/api";
import { toast } from "@/components/ui/Toast";

export default function ApplicationsPage() {
  const router = useRouter();
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [selectedApp, setSelectedApp] = useState<LoanApplication | null>(null);
  const [disbursingRef, setDisbursingRef] = useState<string | null>(null);

  const loadData = () => {
    fetchMyApplications().then(setApplications);
  };

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=CUSTOMER");
      return;
    }
    loadData();
  }, [router]);

  const handleDisburse = async (app: LoanApplication) => {
    setDisbursingRef(app.applicationReference);
    try {
      await disburseCustomerLoan(app.applicationReference, app.id);
      toast.success(
        "🎉 Funds Disbursed!",
        `₹${(app.approvedAmount || app.requestedAmount).toLocaleString()} credited to your Operative Account from Bank Vault!`
      );
      setApplications((prev) =>
        prev.map((a) => (a.applicationReference === app.applicationReference ? { ...a, status: "DISBURSED" } : a))
      );
      loadData();
      if (selectedApp?.id === app.id) {
        setSelectedApp(null);
      }
    } catch (err: any) {
      toast.error("Disbursement Failed", err.message || "Failed to disburse loan funds");
    } finally {
      setDisbursingRef(null);
    }
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 py-4 sm:py-6 px-3.5 sm:px-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-7 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-[11px] font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Portfolio</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">My Loan Applications</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Track your submitted loans and verification status.
          </p>
        </div>

        <Link
          href="/customer/apply"
          className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-5 py-2.5 rounded-full font-bold text-xs shadow-md transition flex items-center space-x-1.5 self-start sm:self-auto active:scale-95"
        >
          <span>Apply for New Loan</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Grid of Applications */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
        {applications.map((app) => (
          <div
            key={app.id}
            className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 rounded-xl p-4 sm:p-5 shadow-xl transition space-y-4"
          >
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[11px] font-mono text-neutral-500">REF: {app.applicationReference}</span>
                <h3 className="text-lg font-bold text-white">{app.loanType} LOAN</h3>
              </div>
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-[#141414] text-[#CAFF33] border border-[#262626]">
                {app.status}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs pt-3 border-t border-[#262626]">
              <div>
                <span className="text-neutral-500 block">Requested Quantum</span>
                <strong className="text-sm text-white">₹{app.requestedAmount.toLocaleString()}</strong>
              </div>
              <div>
                <span className="text-neutral-500 block">Tenure</span>
                <strong className="text-sm text-white">{app.tenureMonths} Months</strong>
              </div>
            </div>

            <div className="p-3 bg-[#141414] rounded-xl border border-[#262626] text-xs">
              <span className="text-neutral-500 block text-[10px]">Purpose:</span>
              <p className="text-neutral-300 italic text-[11px] mt-0.5">&quot;{app.purpose}&quot;</p>
            </div>

            {app.status === "APPROVED" && (
              <div className="pt-2">
                <button
                  onClick={() => handleDisburse(app)}
                  disabled={disbursingRef === app.applicationReference}
                  className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] text-black py-2 rounded-xl text-xs font-bold transition shadow flex items-center justify-center space-x-1.5 active:scale-95 disabled:opacity-50"
                >
                  <ArrowDownLeft className="w-3.5 h-3.5" />
                  <span>
                    {disbursingRef === app.applicationReference
                      ? "Disbursing Funds..."
                      : `⚡ Disburse ₹${(app.approvedAmount || app.requestedAmount).toLocaleString()} to Account`}
                  </span>
                </button>
              </div>
            )}

            <div className="flex justify-between items-center pt-2">
              <button
                onClick={() => setSelectedApp(app)}
                className="text-xs font-semibold text-[#CAFF33] hover:underline"
              >
                View Status Details &rarr;
              </button>
              <Link
                href="/customer/documents"
                className="text-xs text-neutral-400 hover:text-white transition"
              >
                Manage Proofs
              </Link>
            </div>
          </div>
        ))}
      </div>

      {/* Details Modal */}
      {selectedApp && (
        <div className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-4">
          <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl max-w-lg w-full p-8 space-y-5 shadow-2xl">
            <div className="flex justify-between items-start border-b border-[#262626] pb-4">
              <div>
                <span className="text-[11px] font-mono text-neutral-500">REF: {selectedApp.applicationReference}</span>
                <h3 className="text-xl font-bold text-white">{selectedApp.loanType} Loan Summary</h3>
              </div>
              <button
                onClick={() => setSelectedApp(null)}
                className="text-xs text-neutral-400 hover:text-white px-3 py-1.5 rounded-full bg-[#262626]"
              >
                Close
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between p-3 rounded-xl bg-[#141414] border border-[#262626]">
                <span className="text-neutral-400">Current Status:</span>
                <span className="font-bold text-[#CAFF33]">{selectedApp.status}</span>
              </div>
              <div className="flex justify-between p-3 rounded-xl bg-[#141414] border border-[#262626]">
                <span className="text-neutral-400">Principal Amount:</span>
                <span className="font-bold text-white">₹{selectedApp.requestedAmount.toLocaleString()}</span>
              </div>
              <div className="flex justify-between p-3 rounded-xl bg-[#141414] border border-[#262626]">
                <span className="text-neutral-400">Tenure:</span>
                <span className="font-bold text-white">{selectedApp.tenureMonths} Months</span>
              </div>
            </div>

            <div className="flex justify-end pt-3 space-x-2">
              {selectedApp.status === "APPROVED" && (
                <button
                  onClick={() => handleDisburse(selectedApp)}
                  disabled={disbursingRef === selectedApp.applicationReference}
                  className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-2.5 rounded-full font-bold text-xs transition flex items-center space-x-1.5 active:scale-95 disabled:opacity-50"
                >
                  <ArrowDownLeft className="w-3.5 h-3.5" />
                  <span>
                    {disbursingRef === selectedApp.applicationReference
                      ? "Disbursing..."
                      : `Disburse ₹${(selectedApp.approvedAmount || selectedApp.requestedAmount).toLocaleString()} Now`}
                  </span>
                </button>
              )}
              <button
                onClick={() => setSelectedApp(null)}
                className="bg-[#262626] hover:bg-[#333333] text-white px-6 py-2.5 rounded-full font-medium text-xs transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
