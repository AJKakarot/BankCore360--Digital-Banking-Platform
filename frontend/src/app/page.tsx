import React from "react";
import Link from "next/link";
import {
  ArrowRight,
  Bot,
  ChevronRight,
  Shield,
  Zap,
  Clock,
  CheckCircle2
} from "lucide-react";

export default function LandingPage() {
  const loanProducts = [
    {
      type: "Personal Loan",
      tag: "Instant",
      rate: "10.50% - 14.50%",
      maxAmount: "₹10,00,000",
      tenure: "Up to 5 Years",
      desc: "Flexible financing for personal expenses with minimal documentation and quick sanction."
    },
    {
      type: "Home Loan",
      tag: "Best Rates",
      rate: "8.50% - 9.75%",
      maxAmount: "₹1,50,00,000",
      tenure: "Up to 30 Years",
      desc: "Competitive mortgage solutions for property purchase and construction with up to 90% LTV."
    },
    {
      type: "Education Loan",
      tag: "Student Concession",
      rate: "9.00% - 11.25%",
      maxAmount: "₹30,00,000",
      tenure: "Course + 10 Years",
      desc: "Comprehensive funding for top domestic and overseas degree programs with moratorium benefit."
    },
    {
      type: "Business Loan",
      tag: "Growth",
      rate: "11.00% - 15.50%",
      maxAmount: "₹2,00,00,000",
      tenure: "Up to 7 Years",
      desc: "Term loans and working capital solutions tailored for enterprise and MSME scaling."
    }
  ];

  return (
    <div className="bg-[#141414] text-white space-y-16 py-8 px-4 sm:px-6 max-w-6xl mx-auto">
      {/* 1. Hero Section */}
      <section className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 sm:p-14 relative overflow-hidden shadow-2xl">
        <div
          className="absolute top-0 right-0 w-80 h-80 opacity-20 pointer-events-none"
          style={{
            backgroundImage:
              "radial-gradient(#CAFF33 1.5px, transparent 1.5px), radial-gradient(#CAFF33 1.5px, #1C1C1C 1.5px)",
            backgroundSize: "20px 20px",
            backgroundPosition: "0 0, 10px 10px"
          }}
        />

        <div className="max-w-3xl space-y-6 relative z-10">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-4 py-1.5 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Digital Banking & Instant Loan Management</span>
          </div>

          <h1 className="text-4xl sm:text-6xl font-bold tracking-tight text-white leading-tight">
            Banking Made Smarter. <br />
            <span className="text-[#CAFF33]">Decisions Made Simpler.</span>
          </h1>

          <p className="text-neutral-400 text-base sm:text-lg leading-relaxed">
            BankCore360 provides a modern digital lending platform with transparent approvals, automated verification, and AI-powered loan assistance.
          </p>

          <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4 pt-3">
            <Link
              href="/customer/apply"
              className="w-full sm:w-auto bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-7 py-3.5 rounded-full font-bold flex items-center justify-center space-x-2 transition shadow-lg active:scale-95 text-sm"
            >
              <span>Explore Loans</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link
              href="/login"
              className="w-full sm:w-auto bg-[#262626] hover:bg-[#333333] text-white px-7 py-3.5 rounded-full font-medium flex items-center justify-center space-x-2 transition border border-[#333333] active:scale-95 text-sm"
            >
              <span>Get Started</span>
            </Link>

            <Link
              href="/customer/ai-assistant"
              className="w-full sm:w-auto bg-[#141414] hover:bg-[#222222] text-neutral-300 hover:text-white px-6 py-3.5 rounded-full font-medium flex items-center justify-center space-x-2 transition border border-[#2B2B2B] text-sm"
            >
              <Bot className="w-4 h-4 text-[#CAFF33]" />
              <span>AI Assistant</span>
            </Link>
          </div>
        </div>
      </section>

      {/* 2. Loan Products Section */}
      <section className="space-y-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl font-bold text-white tracking-tight">
              Our <span className="text-[#CAFF33]">Loan Products</span>
            </h2>
            <p className="text-neutral-400 text-sm mt-1">
              Transparent interest rates with reducing-balance calculations
            </p>
          </div>
          <Link
            href="/customer/apply"
            className="text-xs font-semibold text-[#CAFF33] hover:underline flex items-center self-start sm:self-auto"
          >
            <span>View All Loans</span>
            <ChevronRight className="w-4 h-4 ml-0.5" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {loanProducts.map((p) => (
            <div
              key={p.type}
              className="bg-[#1C1C1C] border border-[#262626] hover:border-[#CAFF33]/50 rounded-2xl p-6 flex flex-col justify-between space-y-4 transition-all hover:-translate-y-0.5"
            >
              <div className="space-y-2">
                <span className="text-[10px] font-bold uppercase tracking-wider text-[#CAFF33] px-2.5 py-1 rounded-full bg-[#CAFF33]/10 border border-[#CAFF33]/20 inline-block">
                  {p.tag}
                </span>
                <h3 className="text-lg font-bold text-white">{p.type}</h3>
                <p className="text-xs text-neutral-400 leading-relaxed">{p.desc}</p>
              </div>

              <div className="pt-3 border-t border-[#262626] space-y-1.5 text-xs text-neutral-300">
                <div className="flex justify-between">
                  <span className="text-neutral-500">Rate:</span>
                  <span className="font-semibold text-white">{p.rate}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500">Up To:</span>
                  <span className="font-semibold text-white">{p.maxAmount}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500">Tenure:</span>
                  <span className="font-semibold text-white">{p.tenure}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 3. Simple Process (No long verbose explanation) */}
      <section className="space-y-8">
        <div className="text-center max-w-xl mx-auto">
          <h2 className="text-3xl font-bold text-white tracking-tight">
            How It <span className="text-[#CAFF33]">Works</span>
          </h2>
          <p className="text-neutral-400 text-sm mt-1">
            4 simple steps to your digital loan approval
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            { step: "01", title: "Apply Online", desc: "Select loan product, tenure and requested amount." },
            { step: "02", title: "Upload Proofs", desc: "Attach required documents directly in your portal." },
            { step: "03", title: "Verification", desc: "Fast automated and officer document verification." },
            { step: "04", title: "Sanction", desc: "Quick decision and transparent loan disbursement." }
          ].map((item) => (
            <div
              key={item.step}
              className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-6 space-y-2 relative"
            >
              <div className="text-2xl font-mono font-bold text-[#CAFF33]">{item.step}</div>
              <h4 className="text-base font-bold text-white">{item.title}</h4>
              <p className="text-xs text-neutral-400 leading-relaxed">{item.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 4. AI Assistant Card */}
      <section className="bg-gradient-to-r from-[#1C1C1C] to-[#161616] border border-[#262626] rounded-3xl p-8 sm:p-12 flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="space-y-2 max-w-xl">
          <div className="inline-flex items-center space-x-2 text-[#CAFF33] text-xs font-semibold">
            <Bot className="w-4 h-4" />
            <span>24/7 Policy Assistant</span>
          </div>
          <h3 className="text-2xl sm:text-3xl font-bold text-white">
            Have questions about loan policies or documents?
          </h3>
          <p className="text-xs sm:text-sm text-neutral-400">
            Ask our AI Banking Assistant for instant eligibility, required proofs, and interest details.
          </p>
        </div>
        <Link
          href="/customer/ai-assistant"
          className="bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-7 py-3.5 rounded-full font-bold text-sm transition shadow-lg whitespace-nowrap active:scale-95"
        >
          Ask AI Assistant &rarr;
        </Link>
      </section>
    </div>
  );
}
