"use client";

import React, { useState, useRef } from "react";
import {
  Upload,
  CheckCircle2,
  Trash2,
  FileCheck,
  Paperclip
} from "lucide-react";
import { toast } from "@/components/ui/Toast";

interface PolicyItem {
  id: number;
  title: string;
  filename: string;
  category: string;
  loanType?: string;
  version: string;
  isActive: boolean;
  ingestionStatus: string;
  chunkCount: number;
  uploadedAt: string;
}

const INITIAL_POLICIES: PolicyItem[] = [
  { id: 1, title: "BankCore360 Personal Loan Policy", filename: "personal_loan_policy.txt", category: "Personal Loan Policy", loanType: "PERSONAL", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 6, uploadedAt: "2026-01-01" },
  { id: 2, title: "BankCore360 Home Loan Policy", filename: "home_loan_policy.txt", category: "Home Loan Policy", loanType: "HOME", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 8, uploadedAt: "2026-01-01" },
  { id: 3, title: "BankCore360 Education Loan Policy", filename: "education_loan_policy.txt", category: "Education Loan Policy", loanType: "EDUCATION", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 5, uploadedAt: "2026-01-01" },
  { id: 4, title: "BankCore360 Business Loan Policy", filename: "business_loan_policy.txt", category: "Business Loan Policy", loanType: "BUSINESS", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 7, uploadedAt: "2026-01-01" },
  { id: 5, title: "BankCore360 KYC & Document Checklist", filename: "kyc_and_document_policy.txt", category: "KYC and Document Policy", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 6, uploadedAt: "2026-01-01" },
  { id: 6, title: "BankCore360 Pricing & Interest Rates", filename: "interest_rate_policy.txt", category: "Interest Rate Policy", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 5, uploadedAt: "2026-01-01" },
  { id: 7, title: "BankCore360 Repayment & Amortization Policy", filename: "repayment_and_emi_policy.txt", category: "Repayment and EMI Policy", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 5, uploadedAt: "2026-01-01" },
  { id: 8, title: "BankCore360 Credit Underwriting & FOIR Rules", filename: "loan_eligibility_policy.txt", category: "Loan Eligibility Policy", version: "1.0", isActive: true, ingestionStatus: "INDEXED", chunkCount: 6, uploadedAt: "2026-01-01" },
];

const CATEGORIES = [
  "Personal Loan Policy",
  "Home Loan Policy",
  "Education Loan Policy",
  "Business Loan Policy",
  "KYC and Document Policy",
  "Interest Rate Policy",
  "Repayment and EMI Policy",
  "Loan Eligibility Policy",
];

export default function AdminPoliciesPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [policies, setPolicies] = useState<PolicyItem[]>(INITIAL_POLICIES);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      if (!title) {
        setTitle(file.name.replace(/\.[^/.]+$/, "").replace(/_/g, " "));
      }
    }
  };

  const handleUploadPolicy = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      toast.error("No File Selected", "Please select a policy file from your computer.");
      return;
    }

    setUploading(true);
    const toastId = toast.loading("Uploading & Indexing Policy...", `Parsing chunks for ${selectedFile.name}`);
    setTimeout(() => {
      const newDoc: PolicyItem = {
        id: Date.now(),
        title: title.trim() || selectedFile.name,
        filename: selectedFile.name,
        category,
        version: "1.0",
        isActive: true,
        ingestionStatus: "INDEXED",
        chunkCount: Math.floor(Math.random() * 5) + 4,
        uploadedAt: new Date().toISOString().split("T")[0],
      };
      setPolicies([newDoc, ...policies]);
      setTitle("");
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      setUploading(false);
      toast.dismiss(toastId);
      toast.success("Policy Indexed Successfully!", `'${selectedFile.name}' ready for AI Assistant retrieval`);
      setNotice(`Policy file '${selectedFile.name}' indexed for AI Assistant.`);
      setTimeout(() => setNotice(null), 4000);
    }, 700);
  };

  const handleDelete = (id: number) => {
    const target = policies.find((p) => p.id === id);
    setPolicies(policies.filter((p) => p.id !== id));
    toast.info("Policy Directive Removed", target ? `'${target.title}' archived` : "Policy deleted");
    setNotice("Policy directive removed.");
    setTimeout(() => setNotice(null), 3000);
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 max-w-6xl mx-auto animate-fade-in">
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>RAG Knowledge Base</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">Loan Policies</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Upload loan policy files directly from your computer.
          </p>
        </div>
      </div>

      {notice && (
        <div className="p-4 bg-[#1C1C1C] border border-[#CAFF33]/50 rounded-2xl text-xs text-[#CAFF33] flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{notice}</span>
        </div>
      )}

      {/* Upload Form */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-8 space-y-6 shadow-xl">
        <div className="flex items-center space-x-2 text-white font-bold text-base">
          <Upload className="w-5 h-5 text-[#CAFF33]" />
          <span>Index Policy from Your Computer</span>
        </div>

        <form onSubmit={handleUploadPolicy} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1.5">Policy Title</label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-4 py-3 bg-[#141414] border border-[#262626] rounded-xl text-xs text-white focus:outline-none focus:border-[#CAFF33]"
                placeholder="e.g. Retail Micro-Lending Policy"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1.5">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-4 py-3 bg-[#141414] border border-[#262626] rounded-xl text-xs text-white focus:outline-none focus:border-[#CAFF33]"
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Hidden File Input */}
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept=".txt,.pdf"
            className="hidden"
          />

          {/* Dropzone / Picker */}
          <div
            onClick={() => fileInputRef.current?.click()}
            className="border-2 border-dashed border-[#262626] hover:border-[#CAFF33] bg-[#141414] hover:bg-[#181818] rounded-2xl p-6 text-center cursor-pointer transition"
          >
            {selectedFile ? (
              <div className="flex items-center justify-center space-x-2 text-xs text-white">
                <FileCheck className="w-4 h-4 text-[#CAFF33]" />
                <span className="font-bold">{selectedFile.name}</span>
                <span className="text-neutral-500">({(selectedFile.size / 1024).toFixed(1)} KB)</span>
              </div>
            ) : (
              <div className="flex flex-col items-center space-y-2">
                <Paperclip className="w-5 h-5 text-[#CAFF33]" />
                <span className="text-xs text-neutral-300">Click to choose a .txt or .pdf policy file from your device</span>
              </div>
            )}
          </div>

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={uploading || !selectedFile}
              className="bg-[#CAFF33] hover:bg-[#b8eb2a] disabled:opacity-40 text-black px-8 py-3.5 rounded-full font-bold text-xs shadow transition active:scale-95"
            >
              <span>{uploading ? "Indexing in pgvector..." : "Upload & Index Policy"}</span>
            </button>
          </div>
        </form>
      </div>

      {/* Policies Table */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <table className="w-full text-left text-xs">
          <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
            <tr>
              <th className="p-4 font-semibold">Policy Title</th>
              <th className="p-4 font-semibold">Category</th>
              <th className="p-4 font-semibold">Filename</th>
              <th className="p-4 font-semibold">Chunks</th>
              <th className="p-4 font-semibold">Status</th>
              <th className="p-4 font-semibold text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#262626] text-neutral-300">
            {policies.map((p) => (
              <tr key={p.id} className="hover:bg-[#222222] transition">
                <td className="p-4 font-bold text-white">{p.title}</td>
                <td className="p-4 text-neutral-400">{p.category}</td>
                <td className="p-4 font-mono text-[11px] text-neutral-400">{p.filename}</td>
                <td className="p-4 text-white font-mono">{p.chunkCount}</td>
                <td className="p-4">
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-[#141414] text-[#CAFF33] border border-[#262626]">
                    {p.ingestionStatus}
                  </span>
                </td>
                <td className="p-4 text-right">
                  <button
                    onClick={() => handleDelete(p.id)}
                    className="p-1.5 text-neutral-500 hover:text-red-400 transition"
                    title="Delete Policy"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
