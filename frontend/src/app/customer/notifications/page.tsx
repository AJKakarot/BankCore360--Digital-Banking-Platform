"use client";

import React, { useState, useEffect } from "react";
import { CheckCheck, Clock, RefreshCw, Inbox } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  NotificationItem,
  fetchNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  getActiveUser
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";

const SAMPLE_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 1001,
    title: "Loan Approved",
    notificationType: "LOAN_APPROVED",
    message: "Your loan application LN-2026-XY8899 has been approved for ₹5,00,000 at 9.50% p.a. for 60 months.",
    relatedReference: "LN-2026-XY8899",
    readStatus: false,
    createdAt: new Date(Date.now() - 1800000).toISOString(),
  },
  {
    id: 1002,
    title: "Document Verified",
    notificationType: "KYC_VERIFIED",
    message: "Your document PAN for application LN-2026-AB12CD was verified by Compliance Checker.",
    relatedReference: "LN-2026-AB12CD",
    readStatus: false,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 1003,
    title: "Application Submitted",
    notificationType: "LOAN_SUBMITTED",
    message: "Your loan application LN-2026-AB12CD of ₹2,00,000 (PERSONAL) has been submitted successfully to Kafka event pipeline.",
    relatedReference: "LN-2026-AB12CD",
    readStatus: true,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
];

export default function NotificationsPage() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDemoData, setIsDemoData] = useState(false);

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=CUSTOMER");
      return;
    }
  }, [router]);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const live = await fetchNotifications();
      if (live && live.length > 0) {
        setNotifications(live);
        setIsDemoData(false);
      } else {
        setNotifications(SAMPLE_NOTIFICATIONS);
        setIsDemoData(true);
      }
    } catch {
      setNotifications(SAMPLE_NOTIFICATIONS);
      setIsDemoData(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const handleMarkAllRead = async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, readStatus: true })));
    toast.success("All Caught Up!", "All notifications marked as read");
    if (!isDemoData) {
      await markAllNotificationsAsRead();
    }
  };

  const handleMarkSingleRead = async (id: number) => {
    const item = notifications.find((n) => n.id === id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, readStatus: true } : n))
    );
    if (item) {
      toast.info("Marked as Read", item.title);
    }
    if (!isDemoData) {
      await markNotificationAsRead(id);
    }
  };

  const unreadCount = notifications.filter((n) => !n.readStatus).length;

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto min-h-[80vh]">
      {/* Header */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33] animate-pulse"></span>
            <span>Real-time Stream</span>
            {isDemoData && (
              <span className="text-[10px] text-neutral-400 font-normal border-l border-neutral-600 pl-2">
                Demo Preview
              </span>
            )}
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight flex items-center gap-3">
            Notifications
            {unreadCount > 0 && (
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-[#CAFF33] text-black font-semibold">
                {unreadCount} new
              </span>
            )}
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Real-time updates delivered via BankCore360 Event Stream pipeline.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          <button
            onClick={loadNotifications}
            disabled={loading}
            className="p-2.5 rounded-full bg-[#262626] border border-[#333333] hover:border-[#CAFF33] text-neutral-300 hover:text-white transition disabled:opacity-50"
            title="Refresh feed"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin text-[#CAFF33]" : ""}`} />
          </button>

          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllRead}
              className="text-xs font-bold text-[#CAFF33] hover:underline flex items-center space-x-1.5 px-3 py-2 rounded-full bg-[#262626] border border-[#333333]"
            >
              <CheckCheck className="w-4 h-4" />
              <span>Mark All Read</span>
            </button>
          )}
        </div>
      </div>

      {/* List */}
      <div className="space-y-3">
        {loading && notifications.length === 0 ? (
          <div className="text-center py-12 bg-[#1C1C1C] rounded-2xl border border-[#262626] text-neutral-400 space-y-2">
            <RefreshCw className="w-6 h-6 animate-spin text-[#CAFF33] mx-auto" />
            <p className="text-xs">Connecting to notification stream...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="text-center py-16 bg-[#1C1C1C] rounded-2xl border border-[#262626] text-neutral-400 space-y-3">
            <Inbox className="w-10 h-10 text-neutral-500 mx-auto" />
            <p className="text-sm font-semibold text-white">No notifications yet</p>
            <p className="text-xs max-w-sm mx-auto text-neutral-400">
              When you submit a loan application or compliance checkers review your KYC, real-time alerts will appear here.
            </p>
            <Link
              href="/customer/apply"
              className="inline-block mt-2 px-4 py-2 rounded-full bg-[#CAFF33] text-black font-semibold text-xs hover:bg-[#b8ea2c] transition"
            >
              Apply for a Loan
            </Link>
          </div>
        ) : (
          notifications.map((item) => (
            <div
              key={item.id}
              onClick={() => !item.readStatus && handleMarkSingleRead(item.id)}
              className={`p-5 sm:p-6 rounded-2xl border transition shadow-lg cursor-pointer ${
                !item.readStatus
                  ? "bg-[#1C1C1C] border-[#CAFF33]/40 hover:border-[#CAFF33]"
                  : "bg-[#1C1C1C]/60 border-[#262626] hover:border-neutral-700"
              }`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="space-y-1.5">
                  <div className="flex items-center space-x-2">
                    <h4 className="font-bold text-sm text-white">{item.title}</h4>
                    {!item.readStatus ? (
                      <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
                    ) : (
                      <span className="text-[10px] text-neutral-500 font-medium">Read</span>
                    )}
                    {item.notificationType && (
                      <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-[#262626] text-neutral-400 border border-[#333333]">
                        {item.notificationType}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-neutral-300 leading-relaxed">{item.message}</p>
                  {item.relatedReference && (
                    <div className="text-[11px] font-mono text-[#CAFF33] font-medium pt-0.5">
                      REF: {item.relatedReference}
                    </div>
                  )}
                </div>

                <div className="text-[11px] text-neutral-500 whitespace-nowrap flex items-center space-x-1 shrink-0">
                  <Clock className="w-3 h-3" />
                  <span>
                    {item.createdAt
                      ? new Date(item.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                      : "Recent"}
                  </span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
