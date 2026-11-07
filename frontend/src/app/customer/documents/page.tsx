"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Upload,
  FileText,
  CheckCircle2,
  ArrowRight,
  Paperclip,
  X,
  FileCheck,
  Loader2
} from "lucide-react";
import { toast } from "@/components/ui/Toast";
import {
  fetchApplicationDocuments,
  fetchMyApplications,
  uploadLoanDocument,
  LoanDocument,
  getActiveUser,
  clearSession
} from "@/lib/api";

const DOC_TYPES = [
  { type: "PAN", label: "PAN Card" },
  { type: "AADHAAR", label: "Aadhaar Card" },
  { type: "INCOME_PROOF", label: "Salary Slips (Recent 3 Months)" },
  { type: "BANK_STATEMENT", label: "Bank Statement (6 Months)" },
  { type: "ADDRESS_PROOF", label: "Utility Bill / Address Proof" },
];

export default function DocumentsPage() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [activeRef, setActiveRef] = useState<string>("LN-2026-AB12CD");
  const [documents, setDocuments] = useState<LoanDocument[]>([]);
  const [selectedType, setSelectedType] = useState("PAN");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [currentUser, setCurrentUser] = useState<any>(null);

  useEffect(() => {
    const user = getActiveUser();
    if (!user) {
      router.push("/login");
      return;
    }
    setCurrentUser(user);

    let resolvedRef = "LN-2026-AB12CD";
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const queryRef = params.get("ref");
      const storedRef = localStorage.getItem("bankcore360_active_loan_ref");
      if (queryRef) {
        resolvedRef = queryRef;
        localStorage.setItem("bankcore360_active_loan_ref", queryRef);
      } else if (storedRef) {
        resolvedRef = storedRef;
      }
    }

    setActiveRef(resolvedRef);
    fetchApplicationDocuments(resolvedRef).then((docs) => {
      setDocuments(docs);
      if (docs.length === 0 && resolvedRef === "LN-2026-AB12CD") {
        fetchMyApplications().then((apps) => {
          if (apps && apps.length > 0) {
            const first = apps[0].applicationReference;
            setActiveRef(first);
            if (typeof window !== "undefined") {
              localStorage.setItem("bankcore360_active_loan_ref", first);
            }
            fetchApplicationDocuments(first).then(setDocuments);
          }
        });
      }
    });
  }, [router]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      toast.error("No File Selected", "Please select a PDF or image from your device first.");
      return;
    }

    setUploading(true);
    const toastId = toast.loading("Uploading Document...", `Encrypting ${selectedFile.name} for vault storage`);

    try {
      const newDoc = await uploadLoanDocument(activeRef, selectedType, selectedFile);
      setDocuments([newDoc, ...documents]);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      toast.dismiss(toastId);
      toast.success("Document Uploaded!", `'${newDoc.fileName}' successfully attached to ${activeRef}`);
    } catch {
      toast.dismiss(toastId);
      toast.error("Upload Failed", "Could not upload document. Please check your network and retry.");
    } finally {
      setUploading(false);
    }
  };

  const handleSwitchToChecker = () => {
    clearSession();
    router.push("/login?role=CHECKER");
  };

  return (
    <div className="bg-[#141414] text-white space-y-6 py-4 sm:py-6 px-3.5 sm:px-6 max-w-5xl mx-auto animate-fade-in">
      {/* Title */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl p-5 sm:p-7 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-[11px] font-semibold text-neutral-300">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33]"></span>
            <span>Document Portal</span>
            <span className="text-neutral-500">•</span>
            <span className="font-mono text-[#CAFF33]">{activeRef}</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">Supporting Documents</h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Upload files directly from your computer or device.
          </p>
        </div>
        {currentUser && (
          <Link
            href="/customer/dashboard"
            className="text-xs text-neutral-400 hover:text-white transition self-start sm:self-auto"
          >
            &larr; Back to Dashboard
          </Link>
        )}
      </div>

      {notice && (
        <div className="p-4 bg-[#1C1C1C] border border-[#CAFF33]/50 rounded-2xl text-xs text-[#CAFF33] flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{notice}</span>
        </div>
      )}

      {/* Role Handoff Banner: Move to Checker */}
      <div className="bg-[#1C1C1C] border border-[#CAFF33]/40 rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-xl">
        <div className="space-y-1 text-center sm:text-left">
          <div className="inline-flex items-center space-x-2 text-[#CAFF33] text-xs font-bold uppercase tracking-wider">
            <span className="w-2 h-2 rounded-full bg-[#CAFF33] animate-pulse"></span>
            <span>Workflow Handoff</span>
          </div>
          <h3 className="text-lg font-bold text-white">Proofs Submitted — Ready for Verification</h3>
          <p className="text-xs text-neutral-400">
            Switch persona to Checker to review and approve submitted documents.
          </p>
        </div>
        <button
          onClick={handleSwitchToChecker}
          className="w-full sm:w-auto bg-[#CAFF33] hover:bg-[#b8eb2a] text-black px-6 py-3 rounded-full font-bold text-xs shadow-md transition flex items-center justify-center space-x-2 whitespace-nowrap active:scale-95"
        >
          <span>Switch to Checker Login</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Real File Upload Box */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 space-y-6 shadow-xl">
        <div className="flex items-center space-x-2 text-white font-bold text-base">
          <Upload className="w-5 h-5 text-[#CAFF33]" />
          <span>Upload File from Your Device</span>
        </div>

        <form onSubmit={handleUpload} className="space-y-5">
          {/* Document Type Picker */}
          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1.5">Document Category</label>
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              className="w-full sm:max-w-md px-4 py-3 bg-[#141414] border border-[#262626] rounded-xl text-xs text-white focus:outline-none focus:border-[#CAFF33]"
            >
              {DOC_TYPES.map((d) => (
                <option key={d.type} value={d.type}>
                  {d.type} - {d.label}
                </option>
              ))}
            </select>
          </div>

          {/* Hidden native file input */}
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept=".pdf,.png,.jpg,.jpeg"
            className="hidden"
          />

          {/* Drag & Drop Area */}
          <div
            onClick={() => fileInputRef.current?.click()}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            className={`border-2 border-dashed rounded-3xl p-8 text-center cursor-pointer transition-all ${
              isDragging
                ? "border-[#CAFF33] bg-[#CAFF33]/5"
                : selectedFile
                ? "border-[#CAFF33] bg-[#141414]"
                : "border-[#262626] bg-[#141414] hover:border-neutral-500 hover:bg-[#181818]"
            }`}
          >
            {selectedFile ? (
              <div className="flex flex-col items-center space-y-2">
                <div className="w-12 h-12 rounded-full bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30">
                  <FileCheck className="w-6 h-6" />
                </div>
                <div className="font-bold text-white text-sm">{selectedFile.name}</div>
                <div className="text-xs text-neutral-400">
                  {(selectedFile.size / 1024).toFixed(1)} KB • Ready to upload
                </div>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedFile(null);
                    if (fileInputRef.current) fileInputRef.current.value = "";
                  }}
                  className="mt-2 text-xs text-red-400 hover:text-red-300 underline inline-flex items-center space-x-1"
                >
                  <X className="w-3.5 h-3.5" />
                  <span>Choose a different file</span>
                </button>
              </div>
            ) : (
              <div className="flex flex-col items-center space-y-3">
                <div className="w-12 h-12 rounded-full bg-[#262626] text-[#CAFF33] flex items-center justify-center">
                  <Paperclip className="w-6 h-6" />
                </div>
                <div>
                  <span className="text-sm font-semibold text-white">
                    Click to browse files from your computer
                  </span>
                  <p className="text-xs text-neutral-500 mt-1">
                    Supports PDF, PNG, JPG, or JPEG (Max 5 MB)
                  </p>
                </div>
                <span className="px-4 py-2 rounded-full bg-[#262626] text-xs font-semibold text-neutral-300 hover:text-white transition">
                  Browse Device Files
                </span>
              </div>
            )}
          </div>

          {/* Submit Upload Button */}
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={uploading || !selectedFile}
              className="w-full sm:w-auto bg-[#CAFF33] hover:bg-[#b8eb2a] disabled:opacity-40 text-black px-8 py-3.5 rounded-full font-bold text-xs shadow-md transition flex items-center justify-center space-x-2 active:scale-95"
            >
              {uploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-black" />
                  <span>Uploading & Encrypting...</span>
                </>
              ) : (
                <>
                  <Upload className="w-4 h-4" />
                  <span>Upload Selected File</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>

      {/* Uploaded Documents List */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl shadow-xl overflow-hidden">
        <div className="p-5 sm:p-6 border-b border-[#262626]">
          <h3 className="text-base font-bold text-white">Attached Documents ({documents.length})</h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs min-w-[550px]">
            <thead className="bg-[#141414] text-neutral-400 border-b border-[#262626]">
              <tr>
                <th className="p-4 font-semibold">Type</th>
                <th className="p-4 font-semibold">Filename</th>
                <th className="p-4 font-semibold">Size</th>
                <th className="p-4 font-semibold">Uploaded</th>
                <th className="p-4 font-semibold text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#262626] text-neutral-300">
              {documents.map((doc) => (
                <tr key={doc.id} className="hover:bg-[#222222] transition">
                  <td className="p-4 font-bold text-white">{doc.documentType}</td>
                  <td className="p-4 font-mono text-neutral-300 flex items-center space-x-2">
                    <FileText className="w-4 h-4 text-[#CAFF33]" />
                    <span>{doc.fileName}</span>
                  </td>
                  <td className="p-4 text-neutral-400">{(doc.fileSize / 1024).toFixed(1)} KB</td>
                  <td className="p-4 text-neutral-400">
                    {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleDateString() : "Today"}
                  </td>
                  <td className="p-4 text-right">
                    <span
                      className={`px-3 py-1 rounded-full text-[10px] font-bold ${
                        doc.documentStatus === "VERIFIED"
                          ? "bg-[#CAFF33]/15 text-[#CAFF33] border border-[#CAFF33]/30"
                          : doc.documentStatus === "REJECTED"
                          ? "bg-red-950/40 text-red-400 border border-red-800"
                          : "bg-[#262626] text-neutral-300"
                      }`}
                    >
                      {doc.documentStatus}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
