"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FileText,
  Upload,
  Bot,
  Bell,
  ArrowRight,
  TrendingUp,
  Clock,
  CheckCircle2
} from "lucide-react";
import { getActiveUser, getSavedSession, fetchMyApplications, LoanApplication, UserSession } from "@/lib/api";

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
    <div className="bg-[#141414] text-white space-y-8 py-6 px-4 sm:px-6 max-w-5xl mx-auto">
      {/* Welcome Banner */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl">
        <div className="space-y-2">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Customer Portal</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            Welcome back, {session?.username || "Valued Customer"}
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Manage your loan applications, upload required proofs, and track approvals.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Link
            href="/customer/apply"
            className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-2.5 rounded-full font-bold text-xs shadow-md transition flex items-center space-x-2 active:scale-95"
          >
            <span>Apply for Loan</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
          <Link
            href="/customer/ai-assistant"
            className="bg-[#262626] hover:bg-[#333333] text-white px-5 py-2.5 rounded-full font-medium text-xs transition border border-[#333333] flex items-center space-x-2 active:scale-95"
          >
            <Bot className="w-4 h-4 text-[#CAFF33]" />
            <span>AI Assistant</span>
          </Link>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Active Applications</div>
          <div className="text-3xl font-extrabold text-white mt-2">{applications.length}</div>
          <div className="text-[11px] text-[#CAFF33] mt-2 font-medium">In Process</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Documents</div>
          <div className="text-3xl font-extrabold text-white mt-2">Verified</div>
          <div className="text-[11px] text-neutral-400 mt-2">Identity & Financial Proofs</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Sanction Limit</div>
          <div className="text-3xl font-extrabold text-white mt-2">₹10.0L</div>
          <div className="text-[11px] text-[#CAFF33] mt-2">Pre-approved ceiling</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Assigned Branch</div>
          <div className="text-3xl font-extrabold text-white mt-2">Digital 01</div>
          <div className="text-[11px] text-neutral-400 mt-2">Instant Disbursal</div>
        </div>
      </div>

      {/* Applications Table */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl overflow-hidden shadow-lg">
        <div className="p-6 border-b border-[#262626] flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-white">Your Applications</h3>
            <p className="text-xs text-neutral-400 mt-0.5">Real-time status of your loan requests</p>
          </div>
          <Link
            href="/customer/applications"
            className="text-xs font-semibold text-[#CAFF33] hover:underline flex items-center"
          >
            <span>View All</span>
            <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs min-w-[550px]">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
              <tr>
                <th className="p-4 font-semibold">Reference</th>
                <th className="p-4 font-semibold">Loan Type</th>
                <th className="p-4 font-semibold">Requested</th>
                <th className="p-4 font-semibold">Tenure</th>
                <th className="p-4 font-semibold">Status</th>
                <th className="p-4 font-semibold text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#262626] text-neutral-300">
              {applications.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-500">
                    No loan applications yet. Click &quot;Apply for Loan&quot; to begin.
                  </td>
                </tr>
              ) : (
                applications.map((app) => (
                  <tr key={app.id} className="hover:bg-[#222222] transition">
                    <td className="p-4 font-mono font-bold text-white">{app.applicationReference}</td>
                    <td className="p-4 font-medium text-white">{app.loanType}</td>
                    <td className="p-4 font-semibold text-white">₹{app.requestedAmount.toLocaleString()}</td>
                    <td className="p-4">{app.tenureMonths} Months</td>
                    <td className="p-4">
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-[#262626] text-[#CAFF33] border border-[#333333]">
                        {app.status}
                      </span>
                    </td>
                    <td className="p-4 text-right">
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

      {/* Quick Action Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <Link
          href="/customer/apply"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl flex items-start space-x-4 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33] shrink-0">
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-sm text-white">Apply for a New Loan</h4>
            <p className="text-xs text-neutral-400 mt-1">Submit application for Personal, Home, Education, or Business loan.</p>
          </div>
        </Link>

        <Link
          href="/customer/documents"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl flex items-start space-x-4 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33] shrink-0">
            <Upload className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-sm text-white">Upload Documents</h4>
            <p className="text-xs text-neutral-400 mt-1">Submit test PAN, Aadhaar, salary slips, or bank statements.</p>
          </div>
        </Link>

        <Link
          href="/customer/notifications"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl flex items-start space-x-4 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33] shrink-0">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-sm text-white">Notifications Inbox</h4>
            <p className="text-xs text-neutral-400 mt-1">View real-time notifications about your loan status.</p>
          </div>
        </Link>
      </div>
    </div>
  );
}
