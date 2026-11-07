"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Search, RefreshCw, Radio } from "lucide-react";
import { fetchAdminAuditLogs, AuditLogItem, getActiveUser } from "@/lib/api";
import { toast } from "@/components/ui/Toast";

export default function AdminAuditLogsPage() {
  const router = useRouter();
  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("");

  const loadLogs = async (showToast = false) => {
    setLoading(true);
    try {
      const data = await fetchAdminAuditLogs();
      setLogs(data.content || []);
      if (showToast) {
        toast.success("Audit Logs Refreshed", `Loaded ${data.content?.length || 0} compliance records`);
      }
    } catch {
      if (showToast) {
        toast.error("Failed to Fetch Logs", "Could not load audit logs");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const user = getActiveUser();
    if (!user || user.role !== "ADMIN") {
      router.push("/login?role=ADMIN");
      return;
    }
    loadLogs();
  }, [router]);

  const filtered = logs.filter(
    (l) =>
      l.applicationReference?.toLowerCase().includes(filter.toLowerCase()) ||
      l.actorUsername?.toLowerCase().includes(filter.toLowerCase()) ||
      l.action?.toLowerCase().includes(filter.toLowerCase()) ||
      l.remarks?.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 max-w-6xl mx-auto">
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Compliance Trail</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">Audit Events</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Immutable log of all submission, verification, and sanction actions.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            href="/admin/events"
            className="px-3.5 py-2.5 rounded-full bg-[#262626] border border-[#333333] hover:border-[#CAFF33] text-xs font-semibold text-neutral-300 hover:text-white transition flex items-center space-x-1.5"
          >
            <Radio className="w-3.5 h-3.5 text-[#CAFF33]" />
            <span>Event Stream & Outbox</span>
          </Link>

          <div className="relative">
            <Search className="w-4 h-4 text-neutral-500 absolute left-3.5 top-3" />
            <input
              type="text"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Search ref, actor, action..."
              className="pl-9 pr-4 py-2.5 bg-[#141414] border border-[#262626] rounded-full text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33]"
            />
          </div>
          <button
            onClick={() => loadLogs(true)}
            className="p-3 rounded-full border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white transition"
            title="Refresh logs"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <table className="w-full text-left text-xs">
          <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
            <tr>
              <th className="p-4 font-semibold">Timestamp</th>
              <th className="p-4 font-semibold">Reference</th>
              <th className="p-4 font-semibold">Action</th>
              <th className="p-4 font-semibold">Actor</th>
              <th className="p-4 font-semibold">Remarks</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#262626] text-neutral-300">
            {filtered.map((item) => (
              <tr key={item.id} className="hover:bg-[#222222] transition">
                <td className="p-4 text-neutral-500 font-mono text-[11px]">
                  {item.timestamp ? new Date(item.timestamp).toLocaleString() : "Recent"}
                </td>
                <td className="p-4 font-mono font-bold text-white">{item.applicationReference}</td>
                <td className="p-4">
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-[#141414] text-[#CAFF33] border border-[#262626]">
                    {item.action}
                  </span>
                </td>
                <td className="p-4 font-medium text-white">{item.actorUsername}</td>
                <td className="p-4 text-neutral-400">{item.remarks}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
