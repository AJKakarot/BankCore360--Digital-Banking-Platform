"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FileText,
  Upload,
  Bot,
  ArrowRight,
  TrendingUp,
  Clock,
  CheckCircle2,
  FolderKanban
} from "lucide-react";
import { getActiveUser, fetchMyApplications, LoanApplication, UserSession } from "@/lib/api";

export default function CustomerDashboard() {
  const router = useRouter();
  const [session, setSession] = useState<UserSession | null>(null);
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=CUSTOMER");
      return;
    }
    setSession(user);
    fetchMyApplications().then((apps) => {
      setApplications(apps);
      setLoading(false);
    });
  }, [router]);

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
            Manage your loan applications, upload required proofs, and track approvals.
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

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs min-w-[500px]">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
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
                applications.map((app) => (
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
                      <Link
                        href={`/customer/applications?ref=${app.applicationReference}`}
                        className="text-xs font-semibold text-[#CAFF33] hover:underline"
                      >
                        Details &rarr;
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
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
    </div>
  );
}
