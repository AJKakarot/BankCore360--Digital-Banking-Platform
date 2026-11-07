"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  LogOut,
  LayoutDashboard,
  Bell,
  Menu,
  X
} from "lucide-react";
import {
  getActiveUser,
  clearSession,
  UserSession,
  fetchUnreadNotificationCount,
  fetchNotifications,
  fetchAdminAuditLogs
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState<UserSession | null>(null);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const initialMountRef = useRef(true);
  const seenNotifIdsRef = useRef<Set<number>>(new Set());
  const seenAuditIdsRef = useRef<Set<number>>(new Set());

  useEffect(() => {
    setMobileMenuOpen(false);
    const user = getActiveUser();
    setCurrentUser(user);
    if (user?.role === "CUSTOMER") {
      fetchUnreadNotificationCount().then((cnt) => setUnreadCount(cnt)).catch(() => {});
    }
  }, [pathname]);

  // Real-time Event Listener for Notification Alerts (Customer) and Audit Events (Admin)
  useEffect(() => {
    if (!currentUser) return;

    initialMountRef.current = true;
    seenNotifIdsRef.current.clear();
    seenAuditIdsRef.current.clear();

    const checkRealtimeEvents = async () => {
      try {
        if (currentUser.role === "CUSTOMER") {
          const list = await fetchNotifications();
          setUnreadCount(list.filter((n) => !n.readStatus).length);

          if (initialMountRef.current) {
            list.forEach((n) => seenNotifIdsRef.current.add(n.id));
          } else {
            const newNotifs = list.filter((n) => !n.readStatus && !seenNotifIdsRef.current.has(n.id));
            if (newNotifs.length > 0) {
              newNotifs.forEach((n) => {
                seenNotifIdsRef.current.add(n.id);
                toast.success(`🔔 ${n.title}`, n.message, 5000);
              });
            }
          }
        } else if (currentUser.role === "ADMIN") {
          const logsData = await fetchAdminAuditLogs(0, 5);
          const logs = logsData.content || [];

          if (initialMountRef.current) {
            logs.forEach((l) => seenAuditIdsRef.current.add(l.id));
          } else {
            const newLogs = logs.filter((l) => !seenAuditIdsRef.current.has(l.id));
            if (newLogs.length > 0) {
              newLogs.forEach((l) => {
                seenAuditIdsRef.current.add(l.id);
                toast.info(`🛡️ New Audit Log`, `${l.action} by ${l.actorUsername}: ${l.remarks || l.applicationReference}`, 5000);
              });
            }
          }
        }
      } catch {
        // Silent error
      } finally {
        initialMountRef.current = false;
      }
    };

    checkRealtimeEvents();
    const interval = setInterval(checkRealtimeEvents, 8000);
    return () => clearInterval(interval);
  }, [currentUser]);

  const handleLogout = () => {
    clearSession();
    setCurrentUser(null);
    toast.info("Logged Out", "Signed out of BankCore360");
    router.push("/login");
  };

  const navLinks = [
    { name: "Home", href: "/" },
    { name: "Loans", href: "/customer/apply" },
    { name: "AI Assistant", href: "/customer/ai-assistant" }
  ];

  return (
    <header className="sticky top-0 z-50 bg-[#1A1A1A]/95 backdrop-blur-md border-b border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 sm:h-20 flex items-center justify-between">
        {/* Brand Logo with 4-petal Lime Icon */}
        <Link href="/" className="flex items-center space-x-2 sm:space-x-2.5 group">
          <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-lg flex items-center justify-center relative">
            <svg viewBox="0 0 24 24" className="w-6 h-6 sm:w-7 sm:h-7 text-[#CAFF33]" fill="currentColor">
              <rect x="2" y="2" width="8" height="8" rx="2" className="opacity-90" />
              <rect x="14" y="2" width="8" height="8" rx="2" className="opacity-70" />
              <rect x="2" y="14" width="8" height="8" rx="2" className="opacity-70" />
              <rect x="14" y="14" width="8" height="8" rx="2" className="opacity-90" />
            </svg>
          </div>
          <span className="text-lg sm:text-xl font-bold tracking-tight text-white group-hover:text-[#CAFF33] transition-colors">
            BankCore360
          </span>
        </Link>

        {/* Center Menu Navigation */}
        <nav className="hidden md:flex items-center space-x-8">
          {navLinks.map((link) => {
            const isActive = pathname === link.href;
            return (
              <Link
                key={link.name}
                href={link.href}
                className={`text-sm font-medium transition-colors ${
                  isActive
                    ? "text-[#CAFF33]"
                    : "text-[#B3B3B3] hover:text-white"
                }`}
              >
                {link.name}
              </Link>
            );
          })}
        </nav>

        {/* Right CTA & Role Actions */}
        <div className="flex items-center space-x-2 sm:space-x-4">
          {currentUser ? (
            <>
              {/* Authenticated User Identity Badge - hidden on mobile to avoid overcrowding */}
              <div className="hidden md:flex items-center space-x-2 px-3 py-1.5 rounded-full bg-[#262626] border border-[#333333] text-xs text-neutral-300">
                <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
                <span className="font-semibold text-white">{currentUser.username}</span>
                <span className="text-[10px] text-[#CAFF33] font-bold uppercase tracking-wide">
                  [{currentUser.role}]
                </span>
              </div>

              {/* Notifications Icon for Customers */}
              {currentUser.role === "CUSTOMER" && (
                <Link
                  href="/customer/notifications"
                  className="relative p-2 rounded-full bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33] text-neutral-300 hover:text-white transition"
                  title="Notifications Feed"
                >
                  <Bell className="w-4 h-4 text-[#CAFF33]" />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-[#CAFF33] text-black text-[10px] font-bold flex items-center justify-center animate-pulse">
                      {unreadCount > 9 ? "9+" : unreadCount}
                    </span>
                  )}
                </Link>
              )}

              {/* Dashboard Link */}
              <Link
                href={
                  currentUser.role === "CUSTOMER"
                    ? "/customer/dashboard"
                    : currentUser.role === "CHECKER"
                    ? "/checker/dashboard"
                    : currentUser.role === "MANAGER"
                    ? "/manager/dashboard"
                    : "/admin/dashboard"
                }
                className="hidden sm:inline-flex items-center space-x-1 px-4 py-2 rounded-full bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33] text-xs font-semibold text-white transition"
              >
                <LayoutDashboard className="w-3.5 h-3.5 text-[#CAFF33]" />
                <span>Dashboard</span>
              </Link>

              {/* Logout Button */}
              <button
                onClick={handleLogout}
                className="hidden sm:inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-full bg-[#262626] hover:bg-red-950/60 text-neutral-400 hover:text-red-400 border border-[#333333] transition text-xs font-medium"
                title="Log out"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="text-xs sm:text-sm font-medium text-white hover:text-[#CAFF33] px-3 py-2 transition"
              >
                Sign In
              </Link>
              <Link
                href="/register"
                className="text-xs sm:text-sm font-semibold bg-[#CAFF33] text-black hover:bg-[#B8F020] px-4 py-2 sm:px-5 sm:py-2.5 rounded-full transition shadow-lg shadow-[#CAFF33]/10"
              >
                Open Account
              </Link>
            </>
          )}

          {/* Mobile Hamburger Toggle Button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-xl bg-[#262626] text-neutral-300 hover:text-white border border-[#333333] focus:outline-none"
            aria-label="Toggle navigation menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5 text-[#CAFF33]" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-[#262626] bg-[#1A1A1A] px-4 pt-3 pb-6 space-y-4 animate-fade-in shadow-2xl">
          {currentUser && (
            <div className="p-3 rounded-2xl bg-[#222222] border border-[#2D2D2D] flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-full bg-[#CAFF33]/20 flex items-center justify-center text-[#CAFF33] font-bold text-xs">
                  {currentUser.username.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <div className="text-xs font-bold text-white">{currentUser.username}</div>
                  <div className="text-[10px] text-neutral-400">{currentUser.email}</div>
                </div>
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#CAFF33]/15 text-[#CAFF33] border border-[#CAFF33]/30">
                {currentUser.role}
              </span>
            </div>
          )}

          <nav className="flex flex-col space-y-1">
            {navLinks.map((link) => (
              <Link
                key={link.name}
                href={link.href}
                onClick={() => setMobileMenuOpen(false)}
                className={`px-3.5 py-2.5 rounded-xl text-sm font-semibold transition ${
                  pathname === link.href
                    ? "bg-[#262626] text-[#CAFF33]"
                    : "text-neutral-300 hover:bg-[#222222] hover:text-white"
                }`}
              >
                {link.name}
              </Link>
            ))}

            {currentUser && (
              <Link
                href={
                  currentUser.role === "CUSTOMER"
                    ? "/customer/dashboard"
                    : currentUser.role === "CHECKER"
                    ? "/checker/dashboard"
                    : currentUser.role === "MANAGER"
                    ? "/manager/dashboard"
                    : "/admin/dashboard"
                }
                onClick={() => setMobileMenuOpen(false)}
                className="px-3.5 py-2.5 rounded-xl text-sm font-semibold text-[#CAFF33] hover:bg-[#222222] flex items-center space-x-2"
              >
                <LayoutDashboard className="w-4 h-4 text-[#CAFF33]" />
                <span>Dashboard ({currentUser.role})</span>
              </Link>
            )}
            {currentUser?.role === "CUSTOMER" && (
              <Link
                href="/customer/notifications"
                onClick={() => setMobileMenuOpen(false)}
                className="px-3.5 py-2.5 rounded-xl text-sm font-semibold text-neutral-300 hover:bg-[#222222] hover:text-white flex items-center justify-between"
              >
                <div className="flex items-center space-x-2">
                  <Bell className="w-4 h-4 text-[#CAFF33]" />
                  <span>Notifications</span>
                </div>
                {unreadCount > 0 && (
                  <span className="px-2 py-0.5 rounded-full bg-[#CAFF33] text-black text-xs font-bold">
                    {unreadCount} new
                  </span>
                )}
              </Link>
            )}
          </nav>

          {/* Mobile Auth Actions */}
          <div className="pt-3 border-t border-[#262626]">
            {currentUser ? (
              <button
                onClick={() => {
                  handleLogout();
                  setMobileMenuOpen(false);
                }}
                className="w-full py-2.5 rounded-full bg-[#262626] hover:bg-red-950/60 text-red-400 font-semibold text-xs border border-[#333333] transition flex items-center justify-center space-x-2"
              >
                <LogOut className="w-4 h-4" />
                <span>Sign Out ({currentUser.username})</span>
              </button>
            ) : (
              <div className="grid grid-cols-2 gap-3">
                <Link
                  href="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="py-2.5 rounded-full bg-[#262626] text-center text-xs font-semibold text-white border border-[#333333]"
                >
                  Open Account
                </Link>
                <Link
                  href="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="py-2.5 rounded-full bg-[#CAFF33] text-center text-xs font-semibold text-black hover:bg-[#B8F020]"
                >
                  Sign In
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
