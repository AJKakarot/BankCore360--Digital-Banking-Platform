"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Users,
  RefreshCw,
  CheckCircle2,
  ShieldCheck,
  UserCheck,
  Sparkles,
  ArrowRight,
  AlertCircle
} from "lucide-react";
import { fetchAdminUsers, updateUserRole, getActiveUser } from "@/lib/api";
import { toast } from "@/components/ui/Toast";

type RoleType = "CUSTOMER" | "CHECKER" | "MANAGER" | "ADMIN";

const ROLES: { role: RoleType; label: string; color: string }[] = [
  { role: "CUSTOMER", label: "Customer", color: "text-neutral-300 bg-[#262626] border-[#333333]" },
  { role: "CHECKER", label: "Checker", color: "text-amber-400 bg-amber-950/40 border-amber-800/60" },
  { role: "MANAGER", label: "Manager", color: "text-emerald-400 bg-emerald-950/40 border-emerald-800/60" },
  { role: "ADMIN", label: "Admin", color: "text-[#CAFF33] bg-[#CAFF33]/15 border-[#CAFF33]/30" },
];

export default function AdminUsersPage() {
  const router = useRouter();
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await fetchAdminUsers();
      if (data && data.length > 0) {
        setUsers(data);
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
    loadUsers();
  }, [router]);

  const handleRoleChange = async (user: any, newRole: RoleType) => {
    if (user.role === newRole) return;
    setUpdatingId(user.id);
    const toastId = toast.loading("Updating User Role...", `Assigning ${newRole} to ${user.username}`);

    try {
      await updateUserRole(user.id, newRole, user.username);
      setUsers((prev) =>
        prev.map((u) => (u.username === user.username ? { ...u, role: newRole } : u))
      );
      toast.dismiss(toastId);
      toast.success("Role Updated Successfully!", `User '${user.username}' is now '${newRole}'`);
      setToastMessage(
        `User '${user.username}' successfully assigned role '${newRole}' in database!`
      );
      setTimeout(() => setToastMessage(null), 4500);
    } catch {
      toast.dismiss(toastId);
      toast.error("Role Update Failed", "Could not update user role. Please try again.");
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 max-w-6xl mx-auto animate-fade-in">
      {/* Title */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Security & Directory</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            Users & Role Assignment
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Assign Manager, Checker, or Customer roles directly to new signup users in the database.
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={loadUsers}
            className="p-3 rounded-full border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white transition"
            title="Refresh user directory"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          </button>
          <Link
            href="/admin/dashboard"
            className="text-xs text-neutral-400 hover:text-white transition"
          >
            &larr; Dashboard
          </Link>
        </div>
      </div>

      {/* Success Notification Toast */}
      {toastMessage && (
        <div className="p-4 bg-[#1C1C1C] border border-[#CAFF33]/60 rounded-2xl text-xs text-[#CAFF33] flex items-center justify-between shadow-2xl animate-fade-in">
          <div className="flex items-center space-x-2.5">
            <CheckCircle2 className="w-4 h-4 text-[#CAFF33] shrink-0" />
            <span className="font-semibold">{toastMessage}</span>
          </div>
          <span className="text-[10px] text-neutral-500 font-mono">Persisted to Neon DB</span>
        </div>
      )}

      {/* Quick Explanatory Banner */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-lg">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center shrink-0 border border-[#CAFF33]/30">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-white">Direct Database Role Assignment</h4>
            <p className="text-xs text-neutral-400">
              When a new user signs up, their default role is <span className="text-[#CAFF33] font-semibold">CUSTOMER</span>. Click any role button below to instantly promote them to <span className="text-amber-400 font-semibold">CHECKER</span> or <span className="text-emerald-400 font-semibold">MANAGER</span>.
            </p>
          </div>
        </div>
        <Link
          href="/register"
          target="_blank"
          className="bg-[#262626] hover:bg-[#333333] text-white px-4 py-2.5 rounded-full text-xs font-semibold whitespace-nowrap transition border border-[#333333] flex items-center space-x-1.5"
        >
          <Sparkles className="w-3.5 h-3.5 text-[#CAFF33]" />
          <span>Test New Signup</span>
        </Link>
      </div>

      {/* User Directory Table */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <div className="p-6 border-b border-[#262626] flex items-center justify-between">
          <h3 className="text-base font-bold text-white">Platform User Directory ({users.length})</h3>
          <span className="text-xs text-neutral-400 font-medium">Dual-Control Enforcement Active</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
              <tr>
                <th className="p-4 font-semibold">User ID</th>
                <th className="p-4 font-semibold">Username</th>
                <th className="p-4 font-semibold">Email</th>
                <th className="p-4 font-semibold">Current Role</th>
                <th className="p-4 font-semibold">Assign Role (Direct DB Update)</th>
                <th className="p-4 font-semibold text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#262626] text-neutral-300">
              {users.map((u) => {
                const isUpdating = updatingId === u.id;
                return (
                  <tr key={u.id || u.username} className="hover:bg-[#222222] transition">
                    <td className="p-4 font-mono text-neutral-500">#{u.id || "NEW"}</td>
                    <td className="p-4">
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-white">{u.username}</span>
                        {u.isNewSignup && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#CAFF33]/20 text-[#CAFF33] border border-[#CAFF33]/40">
                            New Signup
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="p-4 text-neutral-400 font-mono text-[11px]">{u.email}</td>
                    <td className="p-4">
                      <span
                        className={`px-3 py-1 rounded-full text-[11px] font-bold border inline-block ${
                          u.role === "ADMIN"
                            ? "bg-[#CAFF33]/15 text-[#CAFF33] border-[#CAFF33]/30"
                            : u.role === "MANAGER"
                            ? "bg-emerald-950/40 text-emerald-400 border-emerald-800"
                            : u.role === "CHECKER"
                            ? "bg-amber-950/40 text-amber-400 border-amber-800"
                            : "bg-[#262626] text-neutral-300 border-[#333333]"
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex items-center space-x-1.5">
                        {ROLES.map((r) => {
                          const isCurrent = u.role === r.role;
                          return (
                            <button
                              key={r.role}
                              disabled={isUpdating}
                              onClick={() => handleRoleChange(u, r.role)}
                              className={`px-2.5 py-1 rounded-lg text-[10px] font-bold transition border ${
                                isCurrent
                                  ? `${r.color} ring-1 ring-white/20`
                                  : "bg-[#141414] text-neutral-400 border-[#262626] hover:text-white hover:border-neutral-500"
                              } ${isUpdating ? "opacity-50 cursor-not-allowed" : "active:scale-95"}`}
                              title={`Assign role ${r.label}`}
                            >
                              {r.label}
                            </button>
                          );
                        })}
                      </div>
                    </td>
                    <td className="p-4 text-right">
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-950/30 text-emerald-400 border border-emerald-900/40">
                        {u.enabled !== false ? "Active" : "Disabled"}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
