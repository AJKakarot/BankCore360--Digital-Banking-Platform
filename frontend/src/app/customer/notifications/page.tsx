"use client";

import React, { useState, useEffect } from "react";
import { CheckCheck, Clock, RefreshCw, Inbox, CreditCard, AlertTriangle, CheckCircle2, Zap, ArrowRight } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  NotificationItem,
  LoanRepaymentItem,
  fetchNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  payEmiInstallment,
  triggerOverdueAndReminders,
  getActiveUser
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";
import PaymentGatewayModal from "@/components/PaymentGatewayModal";

const SAMPLE_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 1001,
    title: "🚨 Overdue Warning: Late Penalty ₹500 Added",
    notificationType: "EMI_OVERDUE",
    message: "Your monthly EMI installment #1 of ₹8,884.88 for loan LN-2026-AB12CD was due on 2026-10-15. A late fee penalty of ₹500.00 has been added. Total payable: ₹9,384.88. Pay immediately to prevent CIBIL degradation.",
    relatedReference: "LN-2026-AB12CD",
    actionUrl: "/customer/dashboard",
    actionLabel: "Pay Now (₹9,384.88)",
    actionPayload: '{"repaymentId":1,"applicationReference":"LN-2026-AB12CD","amount":9384.88}',
    readStatus: false,
    createdAt: new Date(Date.now() - 1800000).toISOString(),
  },
  {
    id: 1002,
    title: "📅 Monthly EMI Reminder (Due Soon)",
    notificationType: "EMI_DUE",
    message: "Your monthly installment #2 of ₹8,884.88 for loan LN-2026-XY8899 is due on 2026-11-15. Pay on time to maintain a 100% compliant credit score.",
    relatedReference: "LN-2026-XY8899",
    actionUrl: "/customer/dashboard",
    actionLabel: "Pay EMI (₹8,884.88)",
    actionPayload: '{"repaymentId":2,"applicationReference":"LN-2026-XY8899","amount":8884.88}',
    readStatus: false,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 1003,
    title: "Loan Approved & Disbursed",
    notificationType: "LOAN_APPROVED",
    message: "Your loan application LN-2026-AB12CD of ₹1,00,000 has been disbursed from Central Bank Vault into your savings account. Monthly repayment schedule initialized.",
    relatedReference: "LN-2026-AB12CD",
    readStatus: true,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
];

export default function NotificationsPage() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingId, setPayingId] = useState<number | null>(null);
  const [triggeringCheck, setTriggeringCheck] = useState(false);
  const [filterMode, setFilterMode] = useState<"ALL" | "PAYMENTS_ONLY">("ALL");
  const [isDemoData, setIsDemoData] = useState(false);
  const [modalRepayment, setModalRepayment] = useState<LoanRepaymentItem | null>(null);
  const [isGatewayOpen, setIsGatewayOpen] = useState(false);
  const [activeNotificationId, setActiveNotificationId] = useState<number | null>(null);

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
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, readStatus: true } : n))
    );
    if (!isDemoData) {
      await markNotificationAsRead(id);
    }
  };

  const handleOpenGatewayForNotification = (item: NotificationItem, e: React.MouseEvent) => {
    e.stopPropagation();
    setActiveNotificationId(item.id);

    let repaymentId = 1;
    let appRef = item.relatedReference || "LN-2026";
    let amount = 8884.88;
    let lateFee = 0;

    if (item.actionPayload) {
      try {
        const parsed = JSON.parse(item.actionPayload);
        if (parsed.repaymentId) repaymentId = parsed.repaymentId;
        if (parsed.applicationReference) appRef = parsed.applicationReference;
        if (parsed.amount) amount = parsed.amount;
      } catch {
        // ignore
      }
    }

    if (item.notificationType === "EMI_OVERDUE") {
      lateFee = 500;
    }

    const rep: LoanRepaymentItem = {
      id: repaymentId,
      installmentNumber: 1,
      dueDate: new Date().toISOString().split("T")[0],
      emiAmount: lateFee > 0 ? Math.max(amount - lateFee, 1000) : amount,
      principalComponent: amount * 0.85,
      interestComponent: amount * 0.15,
      outstandingBalance: 80000,
      status: lateFee > 0 ? "OVERDUE" : "UPCOMING",
      latePenaltyAmount: lateFee,
      totalPayableAmount: amount,
      applicationReference: appRef,
      isOverdue: lateFee > 0,
    };

    setModalRepayment(rep);
    setIsGatewayOpen(true);
  };

  const handleConfirmGatewayPayment = async (repayment: LoanRepaymentItem, method: string) => {
    try {
      if (!isDemoData) {
        await payEmiInstallment(repayment.id, repayment.applicationReference, method);
      }
      toast.success(
        "🎉 Payment Received!",
        `Paid ₹${(repayment.totalPayableAmount || repayment.emiAmount).toLocaleString()} via ${method}! Funds credited to Central Bank Vault.`
      );
      if (activeNotificationId) {
        await handleMarkSingleRead(activeNotificationId);
      }
      await loadNotifications();
    } catch (err: any) {
      toast.error("Payment Failed", err.message || "Could not process repayment.");
      throw err;
    }
  };

  const handleTriggerOverdueCycle = async () => {
    setTriggeringCheck(true);
    try {
      if (!isDemoData) {
        const msg = await triggerOverdueAndReminders();
        toast.info("Overdue & Reminder Engine", msg || "Monthly check executed successfully.");
      } else {
        toast.info("Demo Overdue Check", "Scanned schedule: 1 Overdue EMI detected, ₹500 late fee calculated.");
      }
      await loadNotifications();
    } catch (err: any) {
      toast.error("Overdue Scan", err.message || "Failed to trigger overdue check");
    } finally {
      setTriggeringCheck(false);
    }
  };

  const isPaymentNotification = (n: NotificationItem) => {
    const type = (n.notificationType || "").toUpperCase();
    return (
      type === "EMI_DUE" ||
      type === "EMI_OVERDUE" ||
      type === "EMI_PAID" ||
      type === "PAYMENT_SUCCESS" ||
      Boolean(n.actionLabel)
    );
  };

  const displayedNotifications = notifications.filter((n) => {
    if (filterMode === "PAYMENTS_ONLY") {
      return isPaymentNotification(n);
    }
    return true;
  });

  const unreadCount = notifications.filter((n) => !n.readStatus).length;
  const paymentAlertsCount = notifications.filter((n) => isPaymentNotification(n) && !n.readStatus).length;

  return (
    <div className="bg-[#141414] text-white space-y-6 py-4 sm:py-6 px-3.5 sm:px-6 lg:px-8 max-w-4xl mx-auto min-h-[80vh]">
      {/* Header */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-7 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-[11px] font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33] animate-pulse"></span>
            <span>Monthly Payment & Overdue Center</span>
            {isDemoData && (
              <span className="text-[10px] text-neutral-400 font-normal border-l border-neutral-600 pl-2">
                Demo Preview
              </span>
            )}
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight flex items-center gap-3">
            Payment Notifications
            {unreadCount > 0 && (
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-[#CAFF33] text-black font-semibold">
                {unreadCount} new
              </span>
            )}
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Monthly EMI alerts, overdue penalty warnings, and direct 1-click repayments.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Overdue Generator Trigger */}
          <button
            onClick={handleTriggerOverdueCycle}
            disabled={triggeringCheck}
            className="px-3.5 py-2 rounded-full bg-[#262626] hover:bg-[#333333] text-neutral-200 border border-[#3A3A3A] hover:border-[#CAFF33] transition text-xs font-semibold flex items-center space-x-1.5 shadow-sm active:scale-95 disabled:opacity-50"
            title="Scan EMIs and calculate late penalties"
          >
            <Zap className={`w-3.5 h-3.5 text-[#CAFF33] ${triggeringCheck ? "animate-spin" : ""}`} />
            <span>{triggeringCheck ? "Scanning..." : "Check Overdue & Remind"}</span>
          </button>

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

      {/* Filter Tabs (All vs Payment Alerts Only) */}
      <div className="flex items-center space-x-2 bg-[#1C1C1C] p-1.5 rounded-xl border border-[#262626] w-fit">
        <button
          onClick={() => setFilterMode("ALL")}
          className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition ${
            filterMode === "ALL"
              ? "bg-[#262626] text-white shadow-sm"
              : "text-neutral-400 hover:text-white"
          }`}
        >
          All ({notifications.length})
        </button>
        <button
          onClick={() => setFilterMode("PAYMENTS_ONLY")}
          className={`px-4 py-1.5 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition ${
            filterMode === "PAYMENTS_ONLY"
              ? "bg-[#CAFF33] text-black shadow-sm"
              : "text-[#CAFF33] hover:bg-[#262626]"
          }`}
        >
          <CreditCard className="w-3.5 h-3.5" />
          <span>Payment & EMI Alerts ({paymentAlertsCount})</span>
        </button>
      </div>

      {/* Notifications Feed */}
      <div className="space-y-3">
        {loading && displayedNotifications.length === 0 ? (
          <div className="text-center py-12 bg-[#1C1C1C] rounded-2xl border border-[#262626] text-neutral-400 space-y-2">
            <RefreshCw className="w-6 h-6 animate-spin text-[#CAFF33] mx-auto" />
            <p className="text-xs">Connecting to payment notification stream...</p>
          </div>
        ) : displayedNotifications.length === 0 ? (
          <div className="text-center py-16 bg-[#1C1C1C] rounded-2xl border border-[#262626] text-neutral-400 space-y-3">
            <Inbox className="w-10 h-10 text-neutral-500 mx-auto" />
            <p className="text-sm font-semibold text-white">No notifications in this filter</p>
            <p className="text-xs max-w-sm mx-auto text-neutral-400">
              When an EMI installment is due or late penalty is applied, direct repayment notifications will appear here.
            </p>
            <button
              onClick={handleTriggerOverdueCycle}
              className="inline-flex items-center space-x-1.5 mt-2 px-4 py-2 rounded-full bg-[#CAFF33] text-black font-semibold text-xs hover:bg-[#b8ea2c] transition"
            >
              <Zap className="w-3.5 h-3.5" />
              <span>Simulate EMI Overdue Check</span>
            </button>
          </div>
        ) : (
          displayedNotifications.map((item) => {
            const isOverdue = item.notificationType === "EMI_OVERDUE" || item.title.toLowerCase().includes("overdue");
            const isDue = item.notificationType === "EMI_DUE" || item.title.toLowerCase().includes("due");
            const isSuccess = item.notificationType === "PAYMENT_SUCCESS" || item.notificationType === "EMI_PAID";
            const canPay = isOverdue || isDue || Boolean(item.actionLabel);

            return (
              <div
                key={item.id}
                onClick={() => !item.readStatus && handleMarkSingleRead(item.id)}
                className={`p-5 sm:p-6 rounded-2xl border transition shadow-lg relative ${
                  isOverdue
                    ? "bg-[#1C1414] border-red-500/40 hover:border-red-500"
                    : !item.readStatus
                    ? "bg-[#1C1C1C] border-[#CAFF33]/40 hover:border-[#CAFF33]"
                    : "bg-[#1C1C1C]/60 border-[#262626] hover:border-neutral-700"
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                  <div className="space-y-2 max-w-2xl">
                    <div className="flex flex-wrap items-center gap-2">
                      {isOverdue && (
                        <span className="px-2.5 py-0.5 rounded-full bg-red-500/20 text-red-400 border border-red-500/40 text-[10px] font-extrabold uppercase tracking-wide flex items-center space-x-1">
                          <AlertTriangle className="w-3 h-3 text-red-400" />
                          <span>Overdue Penalty Applied</span>
                        </span>
                      )}

                      {isDue && (
                        <span className="px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 text-[10px] font-bold uppercase tracking-wide flex items-center space-x-1">
                          <Clock className="w-3 h-3 text-amber-300" />
                          <span>EMI Due Soon</span>
                        </span>
                      )}

                      {isSuccess && (
                        <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 text-[10px] font-bold uppercase tracking-wide flex items-center space-x-1">
                          <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                          <span>Paid & Cleared</span>
                        </span>
                      )}

                      <h4 className="font-bold text-sm text-white">{item.title}</h4>

                      {!item.readStatus ? (
                        <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
                      ) : (
                        <span className="text-[10px] text-neutral-500 font-medium">Read</span>
                      )}
                    </div>

                    <p className="text-xs text-neutral-300 leading-relaxed">{item.message}</p>

                    <div className="flex items-center space-x-3 text-[11px] font-mono text-neutral-400">
                      {item.relatedReference && (
                        <span className="text-[#CAFF33] font-semibold">REF: {item.relatedReference}</span>
                      )}
                      <span>•</span>
                      <span className="flex items-center space-x-1">
                        <Clock className="w-3 h-3" />
                        <span>
                          {item.createdAt
                            ? new Date(item.createdAt).toLocaleDateString() + " " + new Date(item.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                            : "Recent"}
                        </span>
                      </span>
                    </div>

                    {/* DIRECT 1-CLICK PAY EMI ACTION BAR */}
                    {canPay && !isSuccess && (
                      <div className="pt-2 flex flex-wrap items-center gap-3">
                        <button
                          onClick={(e) => handleOpenGatewayForNotification(item, e)}
                          className={`px-4 py-2 rounded-full font-bold text-xs transition flex items-center space-x-2 shadow-lg active:scale-95 ${
                            isOverdue
                              ? "bg-red-500 hover:bg-red-600 text-white shadow-red-500/20"
                              : "bg-[#CAFF33] hover:bg-[#b8eb2a] text-black shadow-[#CAFF33]/20"
                          }`}
                        >
                          <CreditCard className="w-3.5 h-3.5" />
                          <span>{item.actionLabel || "Pay EMI Now"}</span>
                        </button>

                        <Link
                          href="/customer/dashboard"
                          className="text-xs text-neutral-400 hover:text-white flex items-center space-x-1 underline"
                        >
                          <span>Open Amortization Schedule</span>
                          <ArrowRight className="w-3 h-3" />
                        </Link>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Simulated Banking Payment Gateway Modal */}
      <PaymentGatewayModal
        isOpen={isGatewayOpen}
        onClose={() => setIsGatewayOpen(false)}
        repayment={modalRepayment}
        onConfirmPayment={handleConfirmGatewayPayment}
      />
    </div>
  );
}
