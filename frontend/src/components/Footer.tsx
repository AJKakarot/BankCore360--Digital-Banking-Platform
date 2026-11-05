"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Mail, Phone, MapPin, Globe } from "lucide-react";

export default function Footer() {
  const pathname = usePathname();

  // Hide footer completely on AI Assistant chat page for full-screen ChatGPT-style experience
  if (pathname?.startsWith("/customer/ai-assistant")) {
    return null;
  }

  return (
    <footer className="bg-[#141414] border-t border-[#262626] text-neutral-400 text-xs mt-20 pt-14 pb-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        {/* Top Logo & Brand */}
        <div className="flex flex-col items-center justify-center text-center space-y-2">
          <Link href="/" className="flex items-center space-x-2.5 group">
            <svg viewBox="0 0 24 24" className="w-8 h-8 text-[#CAFF33]" fill="currentColor">
              <rect x="2" y="2" width="8" height="8" rx="2" className="opacity-90" />
              <rect x="14" y="2" width="8" height="8" rx="2" className="opacity-70" />
              <rect x="2" y="14" width="8" height="8" rx="2" className="opacity-70" />
              <rect x="14" y="14" width="8" height="8" rx="2" className="opacity-90" />
            </svg>
            <span className="text-xl font-bold tracking-tight text-white group-hover:text-[#CAFF33] transition-colors">
              BankCore360
            </span>
          </Link>
          <p className="text-xs text-neutral-500">
            Digital Loan Origination & Dual-Control Banking Platform
          </p>
        </div>

        {/* Contact Info Row */}
        <div className="flex flex-wrap items-center justify-center gap-6 sm:gap-10 pt-4 border-t border-[#212121] text-xs text-neutral-300">
          <a
            href="mailto:gajeet031@gmail.com"
            className="flex items-center space-x-2 hover:text-[#CAFF33] transition"
          >
            <Mail className="w-4 h-4 text-[#CAFF33]" />
            <span>gajeet031@gmail.com</span>
          </a>
          <a
            href="tel:+918840713812"
            className="flex items-center space-x-2 hover:text-[#CAFF33] transition"
          >
            <Phone className="w-4 h-4 text-[#CAFF33]" />
            <span>+91 8840713812</span>
          </a>
          <div className="flex items-center space-x-2 text-neutral-400">
            <MapPin className="w-4 h-4 text-[#CAFF33]" />
            <span>Sharda Nagar, Kanpur, India</span>
          </div>
        </div>

        {/* Bottom Social & Copyright Bar */}
        <div className="pt-8 border-t border-[#212121] flex flex-col sm:flex-row items-center justify-between gap-4 text-neutral-500 text-[11px]">
          {/* Social Icons: Medium, LinkedIn, and ajeetgupta.com with black icon */}
          <div className="flex items-center space-x-3">
            {/* Medium */}
            <a
              href="https://medium.com/@2301661530002"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Medium Profile"
              className="w-8 h-8 rounded-full bg-[#CAFF33] text-black flex items-center justify-center font-bold hover:opacity-90 hover:scale-105 transition shadow"
              title="Medium"
            >
              M
            </a>

            {/* LinkedIn */}
            <a
              href="https://www.linkedin.com/in/ajeet-gupta-99aa6b281/"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="LinkedIn Profile"
              className="w-8 h-8 rounded-full bg-[#CAFF33] text-black flex items-center justify-center font-bold hover:opacity-90 hover:scale-105 transition text-xs shadow"
              title="LinkedIn"
            >
              in
            </a>

            {/* ajeetgupta.com - right next to LinkedIn */}
            <a
              href="https://www.ajeetgupta.com/"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Ajeet Gupta Personal Website"
              className="w-8 h-8 rounded-full bg-[#CAFF33] text-black flex items-center justify-center hover:opacity-90 hover:scale-105 transition shadow"
              title="ajeetgupta.com"
            >
              <Globe className="w-4 h-4 text-black" strokeWidth={2.5} />
            </a>
          </div>

          {/* Copyright */}
          <div>BankCore360 • Ajeet Gupta • All Rights Reserved</div>

          {/* Policy & Terms */}
          <div className="space-x-4">
            <span className="text-neutral-400 hover:text-white cursor-pointer transition">
              Privacy Policy
            </span>
            <span>|</span>
            <span className="text-neutral-400 hover:text-white cursor-pointer transition">
              Terms of Service
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
