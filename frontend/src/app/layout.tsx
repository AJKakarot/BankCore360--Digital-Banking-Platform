import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { ToastContainer } from "@/components/ui/Toast";

export const metadata: Metadata = {
  title: "BankCore360 — Simple Digital Loan Management Platform",
  description: "Enterprise Digital Loan Platform featuring Maker-Checker-Manager workflow, Apache Kafka event tracking, and Grok AI Policy Assistant.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full antialiased dark">
      <body className="min-h-full flex flex-col bg-[#141414] text-white">
        <Navbar />
        <main className="flex-1 w-full">
          {children}
        </main>
        <Footer />
        <ToastContainer />
      </body>
    </html>
  );
}
