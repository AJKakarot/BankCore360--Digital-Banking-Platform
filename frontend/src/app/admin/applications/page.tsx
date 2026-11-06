"use client";

import React, { useState, useEffect } from "react";
import { Search, RefreshCw } from "lucide-react";
import { fetchAdminApplications, LoanApplication } from "@/lib/api";

const INITIAL_APPLICATIONS: LoanApplication[] = [
  { id: 101, applicationReference: "LN-2026-AB12CD", customerFullName: "John Doe", customerEmail: "john@example.com", loanType: "PERSONAL", requestedAmount: 200000, tenureMonths: 24, purpose: "Home renovation", status: "UNDER_REVIEW", submittedAt: "2026-11-06T10:00:00Z" },
  { id: 102, applicationReference: "LN-2026-XY8899", customerFullName: "John Doe", customerEmail: "john@example.com", loanType: "EDUCATION", requestedAmount: 500000, tenureMonths: 60, purpose: "Higher education", status: "APPROVED", submittedAt: "2026-11-01T09:00:00Z" },
  { id: 103, applicationReference: "LN-2026-CHK01", customerFullName: "Arjun Sharma", customerEmail: "arjun@example.com", loanType: "PERSONAL", requestedAmount: 150000, tenureMonths: 24, purpose: "Medical", status: "PENDING_MANAGER_APPROVAL", submittedAt: "2026-11-07T11:00:00Z" },
];

export default function AdminApplicationsPage() {
  const [applications, setApplications] = useState<LoanApplication[]>(INITIAL_APPLICATIONS);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState("");

  const loadApplications = async () => {
    setLoading(true);
    try {
      const data = await fetchAdminApplications();
      if (data && data.length > 0) {
        setApplications(data);
      }
    } catch {
      // Fallback
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadApplications();
  }, []);

  const filtered = applications.filter(
    (a) =>
      a.applicationReference?.toLowerCase().includes(filter.toLowerCase()) ||
      a.customerFullName?.toLowerCase().includes(filter.toLowerCase()) ||
      a.loanType?.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 max-w-6xl mx-auto">
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Master Registry</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">Loan Applications</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Complete registry of all customer applications and current statuses.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <div className="relative">
            <Search className="w-4 h-4 text-neutral-500 absolute left-3.5 top-3" />
            <input
              type="text"
              placeholder="Search reference, name..."
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="pl-9 pr-4 py-2.5 bg-[#141414] border border-[#262626] rounded-full text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33]"
            />
          </div>
          <button
            onClick={loadApplications}
            className="p-3 rounded-full border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <table className="w-full text-left text-xs">
          <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
            <tr>
              <th className="p-4 font-semibold">Reference</th>
              <th className="p-4 font-semibold">Customer</th>
              <th className="p-4 font-semibold">Product</th>
              <th className="p-4 font-semibold">Amount</th>
              <th className="p-4 font-semibold">Tenure</th>
              <th className="p-4 font-semibold text-right">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#262626] text-neutral-300">
            {filtered.map((app) => (
              <tr key={app.id} className="hover:bg-[#222222] transition">
                <td className="p-4 font-mono font-bold text-white">{app.applicationReference}</td>
                <td className="p-4">{app.customerFullName || "Customer"}</td>
                <td className="p-4 font-semibold text-white">{app.loanType}</td>
                <td className="p-4 font-bold text-white">₹{app.requestedAmount?.toLocaleString()}</td>
                <td className="p-4 text-neutral-400">{app.tenureMonths} Months</td>
                <td className="p-4 text-right">
                  <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-[#141414] text-[#CAFF33] border border-[#262626]">
                    {app.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
