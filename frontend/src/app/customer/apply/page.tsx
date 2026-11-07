"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  FileText,
  Calculator,
  ArrowRight,
  Shield,
  CheckCircle2,
  AlertCircle,
  Lock,
  UserCheck,
  Loader2
} from "lucide-react";
import { toast } from "@/components/ui/Toast";
import { submitLoanApplication, getActiveUser, saveSession, UserSession } from "@/lib/api";

const PRODUCT_INFO: Record<string, { rate: number; minAmt: number; maxAmt: number; maxTenure: number }> = {
  PERSONAL: { rate: 11.5, minAmt: 25000, maxAmt: 1000000, maxTenure: 60 },
  HOME: { rate: 8.75, minAmt: 500000, maxAmt: 15000000, maxTenure: 360 },
  EDUCATION: { rate: 9.5, minAmt: 50000, maxAmt: 3000000, maxTenure: 120 },
  BUSINESS: { rate: 12.0, minAmt: 100000, maxAmt: 5000000, maxTenure: 84 },
};

export default function ApplyLoanPage() {
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState<UserSession | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

  const [loanType, setLoanType] = useState<"PERSONAL" | "HOME" | "EDUCATION" | "BUSINESS">("PERSONAL");
  const [amount, setAmount] = useState<number>(200000);
  const [tenure, setTenure] = useState<number>(24);
  const [purpose, setPurpose] = useState<string>("Higher education & training expenses");
  const [submitting, setSubmitting] = useState(false);
  const [successRef, setSuccessRef] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login?role=CUSTOMER");
      return;
    }
    if (user.role !== "CUSTOMER" && user.role !== "ADMIN") {
      router.push("/login");
      return;
    }
    setCurrentUser(user);
    setAuthChecked(true);
  }, [router]);

  const isCustomer = currentUser && (currentUser.role === "CUSTOMER" || currentUser.role === "ADMIN");

  // Reducing Balance EMI calculation
  const info = PRODUCT_INFO[loanType];
  const r = info.rate / 1200;
  const factor = Math.pow(1 + r, tenure);
  const emi = Math.round((amount * r * factor) / (factor - 1));
  const totalRepayment = emi * tenure;
  const totalInterest = totalRepayment - amount;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const user = getActiveUser();
    if (!user) {
      toast.error("Authentication Required", "Please log in to submit a loan application.");
      router.push("/login?role=CUSTOMER");
      return;
    }

    if (user.role !== "CUSTOMER" && user.role !== "ADMIN") {
      toast.error("Unauthorized Role", "Only registered customer accounts can submit loan applications.");
      return;
    }

    setSubmitting(true);
    setErrorMsg(null);
    const toastId = toast.loading("Submitting Application...", `Loan request of ₹${amount.toLocaleString()} (${loanType})`);

    try {
      const app = await submitLoanApplication({
        loanType,
        requestedAmount: amount,
        tenureMonths: tenure,
        purpose,
      });
      const generatedRef = app.applicationReference || "LN-2026-AB12CD";
      setSuccessRef(generatedRef);
      if (typeof window !== "undefined") {
        localStorage.setItem("bankcore360_active_loan_ref", generatedRef);
      }
      toast.dismiss(toastId);
      toast.success("Application Submitted Successfully!", `Reference ID: ${generatedRef}`);
    } catch (err: any) {
      toast.dismiss(toastId);
      const msg = err.message || "Failed to submit loan application. Please try again.";
      setErrorMsg(msg);
      toast.error("Application Failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 sm:space-y-8 py-4 sm:py-6 px-3.5 sm:px-6 lg:px-8 max-w-5xl mx-auto animate-fade-in">
      {/* Title */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-7 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-[11px] font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Digital Origination</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">Apply for a Loan</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Submit your loan application with instant monthly reducing-balance EMI preview.
          </p>
        </div>
        {isCustomer && (
          <Link
            href="/customer/dashboard"
            className="text-xs text-neutral-400 hover:text-white transition self-start sm:self-auto"
          >
            &larr; Back to Dashboard
          </Link>
        )}
      </div>

      {/* Login Requirement Banner if user is not logged in as Customer */}
      {authChecked && !isCustomer && (
        <div className="bg-[#1C1C1C] border border-amber-500/40 rounded-2xl p-5 sm:p-7 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-xl">
          <div className="space-y-1.5 text-center sm:text-left">
            <div className="inline-flex items-center space-x-2 text-amber-400 text-xs font-bold uppercase tracking-wider">
              <Lock className="w-4 h-4 text-amber-400" />
              <span>Customer Authentication Required</span>
            </div>
            <h3 className="text-lg font-bold text-white">Login required to apply for loan</h3>
          </div>
          <div className="flex flex-col sm:flex-row items-center gap-3 w-full sm:w-auto">
            <Link
              href="/login?role=CUSTOMER"
              className="w-full sm:w-auto bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-3 rounded-full font-bold text-xs shadow-md transition flex items-center justify-center space-x-2 whitespace-nowrap active:scale-95"
            >
              <UserCheck className="w-4 h-4" />
              <span>Login as Customer</span>
            </Link>
            <Link
              href="/register"
              className="w-full sm:w-auto bg-[#262626] hover:bg-[#333333] text-white px-5 py-3 rounded-full font-medium text-xs transition border border-[#333333] whitespace-nowrap text-center"
            >
              <span>Register</span>
            </Link>
          </div>
        </div>
      )}

      {successRef ? (
        <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-10 text-center space-y-6 max-w-xl mx-auto shadow-2xl">
          <div className="w-16 h-16 rounded-full bg-[#CAFF33]/15 text-[#CAFF33] mx-auto flex items-center justify-center border border-[#CAFF33]/30">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <div className="space-y-2">
            <h2 className="text-2xl font-bold text-white">Application Submitted!</h2>
            <p className="text-xs text-neutral-400">
              Your application reference is:
            </p>
            <div className="text-xl font-mono font-bold text-[#CAFF33] bg-[#141414] py-2 px-4 rounded-xl border border-[#262626] inline-block">
              {successRef}
            </div>
          </div>
          <p className="text-xs text-neutral-400">
            Next step: Upload supporting KYC & income proofs to move your file to the Checker queue.
          </p>
          <div className="pt-4 flex flex-col sm:flex-row justify-center gap-3">
            <Link
              href={`/customer/documents?ref=${encodeURIComponent(successRef)}`}
              className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-3 rounded-full font-bold text-xs shadow transition active:scale-95"
            >
              Upload Supporting Proofs &rarr;
            </Link>
            <Link
              href="/customer/dashboard"
              className="bg-[#262626] hover:bg-[#333333] text-white px-6 py-3 rounded-full font-medium text-xs transition border border-[#333333]"
            >
              Go to Dashboard
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Form (2 cols) */}
          <form onSubmit={handleSubmit} className="lg:col-span-2 bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 space-y-6 shadow-xl">
            {errorMsg && (
              <div className="p-3.5 rounded-xl bg-red-950/40 border border-red-800/60 text-red-300 text-xs flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Loan Type Selector */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-neutral-300">Select Loan Type</label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                {(["PERSONAL", "HOME", "EDUCATION", "BUSINESS"] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => {
                      setLoanType(type);
                      setAmount(PRODUCT_INFO[type].minAmt * 2);
                      setTenure(Math.min(tenure, PRODUCT_INFO[type].maxTenure));
                    }}
                    className={`py-3 px-3 rounded-2xl border text-xs font-bold transition text-center ${
                      loanType === type
                        ? "bg-[#CAFF33] text-black border-[#CAFF33] shadow-md"
                        : "bg-[#141414] text-neutral-300 border-[#262626] hover:border-neutral-500"
                    }`}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            {/* Requested Amount */}
            <div className="space-y-2">
              <div className="flex justify-between items-center text-xs">
                <label className="font-semibold text-neutral-300">Requested Amount</label>
                <span className="font-mono font-bold text-[#CAFF33] text-sm">₹{amount.toLocaleString()}</span>
              </div>
              <input
                type="range"
                min={info.minAmt}
                max={info.maxAmt}
                step={25000}
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="w-full accent-[#CAFF33] cursor-pointer"
              />
              <div className="flex justify-between text-[10px] text-neutral-500">
                <span>Min: ₹{info.minAmt.toLocaleString()}</span>
                <span>Max: ₹{info.maxAmt.toLocaleString()}</span>
              </div>
            </div>

            {/* Tenure */}
            <div className="space-y-2">
              <div className="flex justify-between items-center text-xs">
                <label className="font-semibold text-neutral-300">Tenure (Months)</label>
                <span className="font-mono font-bold text-white text-sm">{tenure} Months</span>
              </div>
              <input
                type="range"
                min={6}
                max={info.maxTenure}
                step={6}
                value={tenure}
                onChange={(e) => setTenure(Number(e.target.value))}
                className="w-full accent-[#CAFF33] cursor-pointer"
              />
              <div className="flex justify-between text-[10px] text-neutral-500">
                <span>6 Months</span>
                <span>Max: {info.maxTenure} Months</span>
              </div>
            </div>

            {/* Purpose */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-neutral-300">Loan Purpose</label>
              <textarea
                rows={3}
                required
                value={purpose}
                onChange={(e) => setPurpose(e.target.value)}
                placeholder="State the primary use of loan funds..."
                className="w-full bg-[#141414] border border-[#262626] rounded-2xl p-4 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
              />
            </div>

            {/* Submit Button or Login CTA */}
            {isCustomer ? (
              <button
                type="submit"
                disabled={submitting}
                className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] active:scale-[0.99] text-black font-bold py-4 rounded-full text-sm transition shadow-lg flex items-center justify-center space-x-2 disabled:opacity-50"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin text-black" />
                    <span>Submitting Application...</span>
                  </>
                ) : (
                  <>
                    <span>Submit Application</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            ) : (
              <div className="space-y-3">
                <Link
                  href="/login?role=CUSTOMER"
                  className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] active:scale-[0.99] text-black font-bold py-4 rounded-full text-sm transition shadow-lg flex items-center justify-center space-x-2"
                >
                  <Lock className="w-4 h-4" />
                  <span>Login as Customer to Apply</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            )}
          </form>

          {/* EMI Calculation Summary Card */}
          <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 space-y-6 h-fit shadow-xl">
            <div className="flex items-center space-x-2 text-white font-bold text-sm">
              <Calculator className="w-4 h-4 text-[#CAFF33]" />
              <span>EMI Preview</span>
            </div>

            <div className="p-5 rounded-2xl bg-[#141414] border border-[#262626] text-center space-y-1">
              <span className="text-[11px] text-neutral-400 uppercase tracking-wider font-semibold">Monthly EMI</span>
              <div className="text-3xl font-extrabold text-[#CAFF33]">₹{emi.toLocaleString()}</div>
              <span className="text-[10px] text-neutral-500">Reducing balance interest</span>
            </div>

            <div className="space-y-3 text-xs border-t border-[#262626] pt-4 text-neutral-300">
              <div className="flex justify-between">
                <span className="text-neutral-400">Indicative Rate:</span>
                <span className="font-semibold text-white">{info.rate}% p.a.</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-400">Principal:</span>
                <span className="font-semibold text-white">₹{amount.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-400">Total Interest:</span>
                <span className="font-semibold text-white">₹{totalInterest.toLocaleString()}</span>
              </div>
              <div className="flex justify-between pt-2 border-t border-[#262626] font-bold text-white">
                <span>Total Repayment:</span>
                <span className="text-[#CAFF33]">₹{totalRepayment.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
