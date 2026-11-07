"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  Sparkles
} from "lucide-react";
import { registerCustomer, saveSession, UserSession } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
    phoneNumber: ""
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setLoading(true);

    try {
      await registerCustomer(formData);
      setSuccess(true);
      setTimeout(() => {
        router.push("/login?registered=true&role=CUSTOMER");
      }, 1500);
    } catch (err: any) {
      setErrorMsg(err.message || "Registration failed. Please check backend connection and credentials.");
      setLoading(false);
    }
  };

  return (
    <div className="bg-[#141414] text-white min-h-[85vh] py-12 px-4 sm:px-6 lg:px-8 flex flex-col justify-center">
      <div className="max-w-xl mx-auto w-full">
        <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 sm:p-12 relative overflow-hidden shadow-2xl">
          {/* Subtle Dot Glow Accent */}
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
          <div className="text-center relative z-10 mb-8">
            <h1 className="text-3xl font-bold tracking-tight text-white">Open Account</h1>
            <p className="text-neutral-400 text-sm mt-2">
              Apply for Personal, Home, Education, or Business loans with instant tracking.
            </p>
          </div>

          {success ? (
            <div className="p-8 text-center space-y-4">
              <div className="w-16 h-16 rounded-full bg-[#CAFF33]/15 text-[#CAFF33] mx-auto flex items-center justify-center border border-[#CAFF33]/30">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <h3 className="text-xl font-bold text-white">Registration Successful!</h3>
              <p className="text-xs text-neutral-400 max-w-xs mx-auto">
                Welcome to BankCore360. Preparing your customer portal...
              </p>
              <div className="pt-2">
                <span className="inline-block w-6 h-6 border-2 border-[#CAFF33] border-t-transparent rounded-full animate-spin"></span>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="relative z-10 space-y-4">
              {errorMsg && (
                <div className="p-3.5 rounded-xl bg-red-950/40 border border-red-800/60 text-red-300 text-xs flex items-center space-x-2">
                  <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
                  <span>{errorMsg}</span>
                </div>
              )}

              <div>
                <input
                  type="text"
                  required
                  value={formData.fullName}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                  placeholder="Enter your Full Name"
                  className="w-full bg-[#141414] border border-[#262626] rounded-full px-6 py-3.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                />
              </div>

              <div>
                <input
                  type="text"
                  required
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  placeholder="Choose Username"
                  className="w-full bg-[#141414] border border-[#262626] rounded-full px-6 py-3.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                />
              </div>

              <div>
                <input
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  placeholder="Enter your Email"
                  className="w-full bg-[#141414] border border-[#262626] rounded-full px-6 py-3.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                />
              </div>

              <div>
                <input
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  placeholder="Phone Number (e.g. 9876543210)"
                  className="w-full bg-[#141414] border border-[#262626] rounded-full px-6 py-3.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                />
              </div>

              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  placeholder="Choose Password"
                  className="w-full bg-[#141414] border border-[#262626] rounded-full px-6 pr-12 py-3.5 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-5 flex items-center text-neutral-400 hover:text-white"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>

              <div className="p-3 bg-[#141414] rounded-2xl border border-[#262626] flex items-start space-x-2 text-[11px] text-neutral-400">
                <Sparkles className="w-4 h-4 text-[#CAFF33] flex-shrink-0 mt-0.5" />
                <span>
                  Accounts created through registration are granted <strong>CUSTOMER</strong> permissions. Privileged roles (Checker/Manager) are provisioned via dual-control internal systems.
                </span>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-[#CAFF33] hover:bg-[#b8eb2a] active:scale-[0.99] text-black font-semibold py-4 rounded-full text-sm transition-all shadow-lg mt-3"
              >
                {loading ? "Creating Account..." : "Sign Up"}
              </button>
            </form>
          )}

          <div className="p-4 mt-6 border-t border-[#262626] text-center text-xs text-neutral-400">
            Already have an account?{" "}
            <Link href="/login" className="text-[#CAFF33] hover:underline font-semibold">
              Login here &rarr;
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
