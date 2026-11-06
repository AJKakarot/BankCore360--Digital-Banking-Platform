"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Users,
  FileText,
  ShieldCheck,
  BookOpen,
  ArrowRight,
  RotateCcw,
  Radio
} from "lucide-react";
import { getActiveUser, clearSession } from "@/lib/api";

export default function AdminDashboard() {
  const router = useRouter();

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=ADMIN");
    }
  }, [router]);

  const handleRestartDemo = () => {
    clearSession();
    router.push("/login?role=CUSTOMER");
  };

  return (
    <div className="bg-[#141414] text-white space-y-8 py-6 px-4 sm:px-6 max-w-6xl mx-auto animate-fade-in">
      {/* Banner */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Admin Console</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">Admin Overview</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            System overview of loan applications, user accounts, audit trails, and policy indexing.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleRestartDemo}
            className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-5 py-2.5 rounded-full font-bold text-xs shadow transition flex items-center space-x-1.5 active:scale-95"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Restart Demo as Customer</span>
          </button>
          <Link
            href="/admin/audit"
            className="bg-[#262626] hover:bg-[#333333] text-white px-5 py-2.5 rounded-full font-medium text-xs transition border border-[#333333] flex items-center space-x-1.5 active:scale-95"
          >
            <ShieldCheck className="w-3.5 h-3.5 text-[#CAFF33]" />
            <span>View Audit Logs</span>
          </Link>
        </div>
      </div>

      {/* Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Registered Users</div>
          <div className="text-3xl font-extrabold text-white mt-2">4 Personas</div>
          <div className="text-[11px] text-[#CAFF33] mt-2 font-medium">Active</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Total Applications</div>
          <div className="text-3xl font-extrabold text-white mt-2">12 Files</div>
          <div className="text-[11px] text-neutral-400 mt-2">Across 4 Products</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Sanctioned Volume</div>
          <div className="text-3xl font-extrabold text-[#CAFF33] mt-2">₹1.85 Cr</div>
          <div className="text-[11px] text-neutral-400 mt-2">Approved Loans</div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl">
          <div className="text-xs text-neutral-400 font-medium">Policy Directives</div>
          <div className="text-3xl font-extrabold text-white mt-2">8 Indexed</div>
          <div className="text-[11px] text-[#CAFF33] mt-2 font-medium">RAG Ready</div>
        </div>
      </div>

      {/* Navigation Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
        <Link
          href="/admin/users"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl space-y-3 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <Users className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-white text-sm">User Directory</h3>
          <p className="text-xs text-neutral-400">View user accounts and role assignments.</p>
        </Link>

        <Link
          href="/admin/applications"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl space-y-3 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <FileText className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-white text-sm">Loan Applications</h3>
          <p className="text-xs text-neutral-400">Search and inspect all customer applications.</p>
        </Link>

        <Link
          href="/admin/kafka"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl space-y-3 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <Radio className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-white text-sm">Kafka & Outbox</h3>
          <p className="text-xs text-neutral-400">Monitor topics, outbox events, and consumer idempotency.</p>
        </Link>

        <Link
          href="/admin/audit"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl space-y-3 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-white text-sm">Audit Trail</h3>
          <p className="text-xs text-neutral-400">Review Kafka compliance audit events.</p>
        </Link>

        <Link
          href="/admin/policies"
          className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 p-6 rounded-2xl space-y-3 transition hover:-translate-y-0.5"
        >
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <BookOpen className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-white text-sm">Policy Manager</h3>
          <p className="text-xs text-neutral-400">Upload and re-index loan policies for AI.</p>
        </Link>
      </div>
    </div>
  );
}
