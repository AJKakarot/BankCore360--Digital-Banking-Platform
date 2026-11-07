"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Eye,
  EyeOff,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  ArrowRight,
  Loader2
} from "lucide-react";
import { toast } from "@/components/ui/Toast";
import { loginUser, UserSession, getActiveUser } from "@/lib/api";
import { InfiniteMovingCards } from "@/components/ui/infinite-moving-cards";

type RoleType = "CUSTOMER" | "CHECKER" | "MANAGER" | "ADMIN";

const DEMO_PERSONAS = [
  { role: "CUSTOMER" as RoleType, username: "dev_customer", label: "Customer" },
  { role: "CHECKER" as RoleType, username: "dev_checker", label: "Checker" },
  { role: "MANAGER" as RoleType, username: "dev_manager", label: "Manager" },
  { role: "ADMIN" as RoleType, username: "dev_admin", label: "Admin" }
];

const TESTIMONIALS = [
  {
    id: 1,
    quote: "BankCore360 has been my trusted financial partner. The loan application was processed smoothly and transparently with zero branch visits.",
    name: "Sara T",
    role: "Personal Loan Borrower",
    category: "INDIVIDUAL"
  },
  {
    id: 2,
    quote: "Securing working capital for our business was straightforward and fast. Transparent reducing-balance interest with zero hidden clauses.",
    name: "John D",
    role: "MSME Business Owner",
    category: "BUSINESS"
  },
  {
    id: 3,
    quote: "The digital document upload and status tracking made my education loan approval completely stress-free. Received sanction in 24 hours.",
    name: "Emily G",
    role: "Education Loan Borrower",
    category: "INDIVIDUAL"
  },
  {
    id: 4,
    quote: "Transparent underwriting and automated verification gave us full visibility into our commercial equipment financing schedule.",
    name: "Vikram R",
    role: "Enterprise Director",
    category: "BUSINESS"
  },
  {
    id: 5,
    quote: "The reducing-balance EMI preview accurately calculated our home loan repayment. Approval was swift and hassle-free.",
    name: "Ananya M",
    role: "Home Loan Borrower",
    category: "INDIVIDUAL"
  },
  {
    id: 6,
    quote: "Exceptional digital lending experience. Real-time notifications kept us informed at every step from review to disbursement.",
    name: "Rohit K",
    role: "Tech Startup Founder",
    category: "BUSINESS"
  }
];

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialRoleParam = searchParams.get("role") as RoleType | null;

  const [selectedRole, setSelectedRole] = useState<RoleType>(initialRoleParam || "CUSTOMER");
  const [emailOrUsername, setEmailOrUsername] = useState(
    initialRoleParam === "CHECKER"
      ? "dev_checker"
      : initialRoleParam === "MANAGER"
      ? "dev_manager"
      : initialRoleParam === "ADMIN"
      ? "dev_admin"
      : "dev_customer"
  );
  const [password, setPassword] = useState("Password@123");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"ALL" | "INDIVIDUAL" | "BUSINESS">("ALL");

  const filteredTestimonials = activeTab === "ALL"
    ? TESTIMONIALS
    : TESTIMONIALS.filter((t) => t.category === activeTab);

  const movingCardItems = filteredTestimonials.map((t) => ({
    quote: t.quote,
    name: t.name,
    title: t.role,
    category: t.category,
  }));

  useEffect(() => {
    const user = getActiveUser();
    if (user) {
      routeToRoleDashboard(user.role);
    }
  }, []);

  useEffect(() => {
    if (initialRoleParam) {
      setSelectedRole(initialRoleParam);
      setEmailOrUsername(`dev_${initialRoleParam.toLowerCase()}`);
      setPassword("Password@123");
    }
  }, [initialRoleParam]);

  const routeToRoleDashboard = (role: RoleType) => {
    if (role === "CUSTOMER") router.push("/customer/dashboard");
    else if (role === "CHECKER") router.push("/checker/dashboard");
    else if (role === "MANAGER") router.push("/manager/dashboard");
    else if (role === "ADMIN") router.push("/admin/dashboard");
    else router.push("/customer/dashboard");
  };

  const handleQuickPersona = (persona: (typeof DEMO_PERSONAS)[0]) => {
    setSelectedRole(persona.role);
    setEmailOrUsername(persona.username);
    setPassword("Password@123");
    setErrorMsg(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!emailOrUsername.trim() || !password.trim()) {
      setErrorMsg("Please enter both your email/username and password.");
      return;
    }

    setLoading(true);
    setErrorMsg(null);
    const toastId = toast.loading("Authenticating...", `Verifying credentials for ${emailOrUsername}`);

    try {
      const session = await loginUser(emailOrUsername.trim(), password.trim());
      const role = (session.role as RoleType) || selectedRole || "CUSTOMER";
      toast.dismiss(toastId);
      toast.success("Login Successful!", `Welcome back, ${session.username}!`);
      routeToRoleDashboard(role);
    } catch (err: any) {
      toast.dismiss(toastId);
      const msg = err.message || "Invalid credentials. Please verify and try again.";
      setErrorMsg(msg);
      toast.error("Login Failed", msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-[#141414] text-white min-h-screen py-8 px-4 sm:px-6 space-y-12 sm:space-y-16 animate-fade-in">
      {/* 1. Main Hero Login Card */}
      <section className="max-w-md sm:max-w-lg mx-auto pt-2">
        <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-9 relative overflow-hidden shadow-2xl">
          {/* Subtle Decorative Dot Matrix in Top-Right Corner */}
          <div
            className="absolute top-0 right-0 w-48 h-48 opacity-15 pointer-events-none"
            style={{
              backgroundImage:
                "radial-gradient(#CAFF33 1.5px, transparent 1.5px), radial-gradient(#CAFF33 1.5px, #1C1C1C 1.5px)",
              backgroundSize: "16px 16px",
              backgroundPosition: "0 0, 8px 8px"
            }}
          />

          {/* Heading */}
          <div className="text-center relative z-10 mb-6">
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">Login</h1>
            <p className="text-neutral-400 text-xs sm:text-sm mt-1.5">
              Welcome back! Please log in to access your account.
            </p>

            {/* Role Transition Notice Banner */}
            {initialRoleParam && (
              <div className="mt-3 inline-flex items-center space-x-2 px-3.5 py-1 rounded-full bg-[#CAFF33]/15 text-[#CAFF33] border border-[#CAFF33]/30 text-[11px] font-semibold animate-pulse">
                <span className="w-1.5 h-1.5 rounded-full bg-[#CAFF33]" />
                <span>
                  Ready to sign in as <strong>{initialRoleParam}</strong>
                </span>
              </div>
            )}
          </div>

          {errorMsg && (
            <div className="mb-5 p-3.5 rounded-xl bg-red-950/40 border border-red-800/60 text-red-300 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="relative z-10 space-y-4">
            {/* Vertically stacked inputs for clean, compact proportion */}
            <div className="space-y-3.5">
              {/* Email / Username */}
              <div>
                <label className="block text-xs font-medium text-neutral-300 mb-1.5 text-left">
                  Email or Username
                </label>
                <input
                  type="text"
                  value={emailOrUsername}
                  onChange={(e) => setEmailOrUsername(e.target.value)}
                  placeholder="Enter your Email / Username"
                  required
                  className="w-full bg-[#141414] border border-[#262626] rounded-full px-5 py-3 text-xs sm:text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                />
              </div>

              {/* Password with Eye Icon */}
              <div>
                <label className="block text-xs font-medium text-neutral-300 mb-1.5 text-left">
                  Password
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your Password"
                    required
                    className="w-full bg-[#141414] border border-[#262626] rounded-full px-5 pr-11 py-3 text-xs sm:text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-4 flex items-center text-neutral-400 hover:text-white"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
            </div>

            {/* Forgot Password Link */}
            <div className="text-center pt-1">
              <button
                type="button"
                onClick={() => alert("Credentials for all demo roles use: Password@123")}
                className="text-xs text-neutral-400 hover:text-white underline transition"
              >
                Forgot Password?
              </button>
            </div>

            {/* Primary Action: Neon Lime Pill Button */}
            <div className="pt-1">
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] active:scale-[0.99] text-black font-bold py-3.5 rounded-full text-xs sm:text-sm transition-all shadow-lg flex items-center justify-center space-x-2 disabled:opacity-50"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin text-black" />
                    <span>Authenticating...</span>
                  </>
                ) : (
                  <>
                    <span>Login as {selectedRole}</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>

            {/* Secondary Action: Dark Sign Up Pill */}
            <div>
              <Link
                href="/register"
                className="w-full bg-[#262626] hover:bg-[#333333] active:scale-[0.99] text-white font-medium py-3 rounded-full text-xs sm:text-sm transition-all text-center block border border-[#333333]"
              >
                Sign Up
              </Link>
            </div>

            {/* Role Persona Switcher Tabs */}
            <div className="pt-5 border-t border-[#262626] text-center space-y-2">
              <p className="text-[11px] text-neutral-400">Select Role Persona:</p>
              <div className="flex flex-wrap items-center justify-center gap-2">
                {DEMO_PERSONAS.map((p) => {
                  const isSelected = selectedRole === p.role;
                  return (
                    <button
                      key={p.role}
                      type="button"
                      onClick={() => handleQuickPersona(p)}
                      className={`px-3 py-1.5 rounded-full text-[11px] font-semibold transition ${
                        isSelected
                          ? "bg-[#CAFF33] text-black shadow-md scale-105"
                          : "bg-[#141414] text-neutral-400 border border-[#262626] hover:text-white"
                      }`}
                    >
                      {p.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </form>
        </div>
      </section>

      {/* 2. "Our Testimonials" Section with Infinite Loop Marquee */}
      <section className="max-w-4xl mx-auto space-y-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 px-2">
          <div>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Our <span className="text-[#CAFF33]">Testimonials</span>
            </h2>
            <p className="text-neutral-400 text-xs sm:text-sm max-w-lg mt-1.5 leading-relaxed">
              Discover how BankCore360 empowers borrowers with instant eligibility and seamless loans.
            </p>
          </div>

          <div className="flex items-center space-x-1.5 bg-[#1C1C1C] border border-[#262626] p-1 rounded-full self-start md:self-auto">
            <button
              onClick={() => setActiveTab("ALL")}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition ${
                activeTab === "ALL"
                  ? "bg-[#CAFF33] text-black shadow-sm"
                  : "text-neutral-400 hover:text-white"
              }`}
            >
              All
            </button>
            <button
              onClick={() => setActiveTab("INDIVIDUAL")}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition ${
                activeTab === "INDIVIDUAL"
                  ? "bg-[#CAFF33] text-black shadow-sm"
                  : "text-neutral-400 hover:text-white"
              }`}
            >
              For Individuals
            </button>
            <button
              onClick={() => setActiveTab("BUSINESS")}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition ${
                activeTab === "BUSINESS"
                  ? "bg-[#CAFF33] text-black shadow-sm"
                  : "text-neutral-400 hover:text-white"
              }`}
            >
              For Businesses
            </button>
          </div>
        </div>

        {/* Aceternity UI Infinite Moving Cards */}
        <InfiniteMovingCards
          items={movingCardItems}
          direction="left"
          speed="slow"
          pauseOnHover={true}
          className="py-1"
        />
      </section>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-[#141414] text-white p-8 text-center">Loading portal...</div>}>
      <LoginForm />
    </Suspense>
  );
}
