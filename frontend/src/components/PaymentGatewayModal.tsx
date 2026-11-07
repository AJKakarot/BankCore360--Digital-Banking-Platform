"use client";

import React, { useState } from "react";
import {
  X,
  ShieldCheck,
  CreditCard,
  Smartphone,
  Landmark,
  Zap,
  CheckCircle2,
  Lock,
  ArrowRight,
  AlertTriangle,
  QrCode
} from "lucide-react";
import { LoanRepaymentItem } from "@/lib/api";

interface PaymentGatewayModalProps {
  isOpen: boolean;
  onClose: () => void;
  repayment: LoanRepaymentItem | null;
  onConfirmPayment: (repayment: LoanRepaymentItem, paymentMode: string) => Promise<void>;
}

type PaymentMethodType = "UPI" | "CARD" | "ACCOUNT_DEBIT" | "SANDBOX_FAST";

export default function PaymentGatewayModal({
  isOpen,
  onClose,
  repayment,
  onConfirmPayment,
}: PaymentGatewayModalProps) {
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethodType>("UPI");
  const [upiId, setUpiId] = useState("dev_customer@okhdfcbank");
  const [cardNumber] = useState("4532 •••• •••• 8890");
  const [cardExpiry] = useState("12/28");
  const [cardCvv] = useState("789");
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingStage, setProcessingStage] = useState<string>("");

  if (!isOpen || !repayment) return null;

  const totalAmount =
    repayment.totalPayableAmount ||
    repayment.emiAmount + (repayment.latePenaltyAmount || 0);

  const handlePay = async () => {
    setIsProcessing(true);

    try {
      setProcessingStage("Connecting to BankCore360 FastPay Network...");
      await new Promise((r) => setTimeout(r, 600));

      setProcessingStage("Authorizing Settlement & Clearing Idempotency Key...");
      await new Promise((r) => setTimeout(r, 600));

      setProcessingStage("Depositing Funds into Central Bank ₹100L Vault...");
      await onConfirmPayment(repayment, selectedMethod);

      setProcessingStage("Payment Cleared & Vault Replenished!");
      await new Promise((r) => setTimeout(r, 400));
      onClose();
    } catch {
      // Error handled by parent
    } finally {
      setIsProcessing(false);
      setProcessingStage("");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="bg-[#1C1C1C] border border-[#2E2E2E] rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden text-white transition-all transform animate-scaleUp">
        {/* Gateway Security Header */}
        <div className="bg-[#141414] px-6 py-4 border-b border-[#262626] flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-bold text-white flex items-center space-x-1.5">
                <span>BankCore360 FastPay Gateway</span>
                <span className="px-1.5 py-0.2 rounded bg-[#262626] text-[#CAFF33] text-[9px] font-mono border border-[#333333]">
                  SANDBOX
                </span>
              </div>
              <div className="text-[10px] text-neutral-400 flex items-center space-x-1">
                <Lock className="w-2.5 h-2.5 text-emerald-400" />
                <span>256-Bit SSL Encrypted Simulated Inflow</span>
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            disabled={isProcessing}
            className="p-1.5 rounded-full bg-[#262626] hover:bg-[#333333] text-neutral-400 hover:text-white transition disabled:opacity-50"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Repayment Summary Banner */}
        <div className="p-6 bg-gradient-to-b from-[#1F1F1F] to-[#1C1C1C] border-b border-[#262626] space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-xs text-neutral-400">
              Loan Ref: <span className="font-mono text-white font-semibold">{repayment.applicationReference || "LN-2026"}</span>
            </div>
            <div className="text-xs text-neutral-400">
              Installment <span className="font-bold text-white">#{repayment.installmentNumber}</span>
            </div>
          </div>

          <div className="bg-[#141414] p-4 rounded-2xl border border-[#262626] flex items-center justify-between">
            <div>
              <div className="text-[11px] text-neutral-400">Total Payable to Vault</div>
              <div className="text-2xl font-black text-[#CAFF33] font-mono mt-0.5">
                ₹{totalAmount.toLocaleString()}
              </div>
            </div>

            <div className="text-right space-y-1 text-xs">
              <div className="text-neutral-300 font-mono">
                Base EMI: ₹{repayment.emiAmount.toLocaleString()}
              </div>
              {repayment.latePenaltyAmount && repayment.latePenaltyAmount > 0 ? (
                <div className="text-red-400 font-mono font-bold flex items-center space-x-1 justify-end">
                  <AlertTriangle className="w-3 h-3" />
                  <span>Late Fee: +₹{repayment.latePenaltyAmount.toLocaleString()}</span>
                </div>
              ) : (
                <div className="text-emerald-400 font-mono text-[11px]">No Late Fee (₹0)</div>
              )}
            </div>
          </div>
        </div>

        {/* Simulated Payment Methods */}
        <div className="p-6 space-y-4">
          <label className="text-xs font-bold text-neutral-300 uppercase tracking-wider block">
            Select Simulated Payment Method
          </label>

          <div className="grid grid-cols-2 gap-2.5">
            {/* Method 1: UPI */}
            <button
              type="button"
              onClick={() => setSelectedMethod("UPI")}
              disabled={isProcessing}
              className={`p-3 rounded-2xl border text-left transition flex items-start space-x-3 ${
                selectedMethod === "UPI"
                  ? "bg-[#CAFF33]/10 border-[#CAFF33] text-white ring-1 ring-[#CAFF33]"
                  : "bg-[#141414] border-[#2A2A2A] text-neutral-400 hover:text-white hover:border-[#383838]"
              }`}
            >
              <Smartphone className={`w-4 h-4 mt-0.5 shrink-0 ${selectedMethod === "UPI" ? "text-[#CAFF33]" : ""}`} />
              <div>
                <div className="text-xs font-bold text-white">Instant UPI / QR</div>
                <div className="text-[10px] text-neutral-400">GPay, PhonePe, Paytm</div>
              </div>
            </button>

            {/* Method 2: Fake Card */}
            <button
              type="button"
              onClick={() => setSelectedMethod("CARD")}
              disabled={isProcessing}
              className={`p-3 rounded-2xl border text-left transition flex items-start space-x-3 ${
                selectedMethod === "CARD"
                  ? "bg-[#CAFF33]/10 border-[#CAFF33] text-white ring-1 ring-[#CAFF33]"
                  : "bg-[#141414] border-[#2A2A2A] text-neutral-400 hover:text-white hover:border-[#383838]"
              }`}
            >
              <CreditCard className={`w-4 h-4 mt-0.5 shrink-0 ${selectedMethod === "CARD" ? "text-[#CAFF33]" : ""}`} />
              <div>
                <div className="text-xs font-bold text-white">Test Debit Card</div>
                <div className="text-[10px] text-neutral-400">Visa / Mastercard Test</div>
              </div>
            </button>

            {/* Method 3: Direct Account */}
            <button
              type="button"
              onClick={() => setSelectedMethod("ACCOUNT_DEBIT")}
              disabled={isProcessing}
              className={`p-3 rounded-2xl border text-left transition flex items-start space-x-3 ${
                selectedMethod === "ACCOUNT_DEBIT"
                  ? "bg-[#CAFF33]/10 border-[#CAFF33] text-white ring-1 ring-[#CAFF33]"
                  : "bg-[#141414] border-[#2A2A2A] text-neutral-400 hover:text-white hover:border-[#383838]"
              }`}
            >
              <Landmark className={`w-4 h-4 mt-0.5 shrink-0 ${selectedMethod === "ACCOUNT_DEBIT" ? "text-[#CAFF33]" : ""}`} />
              <div>
                <div className="text-xs font-bold text-white">BankCore Account</div>
                <div className="text-[10px] text-neutral-400">Direct Operative Debit</div>
              </div>
            </button>

            {/* Method 4: 1-Click Sandbox */}
            <button
              type="button"
              onClick={() => setSelectedMethod("SANDBOX_FAST")}
              disabled={isProcessing}
              className={`p-3 rounded-2xl border text-left transition flex items-start space-x-3 ${
                selectedMethod === "SANDBOX_FAST"
                  ? "bg-[#CAFF33]/10 border-[#CAFF33] text-white ring-1 ring-[#CAFF33]"
                  : "bg-[#141414] border-[#2A2A2A] text-neutral-400 hover:text-white hover:border-[#383838]"
              }`}
            >
              <Zap className={`w-4 h-4 mt-0.5 shrink-0 ${selectedMethod === "SANDBOX_FAST" ? "text-[#CAFF33]" : ""}`} />
              <div>
                <div className="text-xs font-bold text-white">Instant Sandbox</div>
                <div className="text-[10px] text-neutral-400">1-Click Auto Clearance</div>
              </div>
            </button>
          </div>

          {/* Payment Method Details Preview */}
          <div className="bg-[#141414] border border-[#262626] p-4 rounded-2xl space-y-3">
            {selectedMethod === "UPI" && (
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-neutral-400">Virtual Payment Address (VPA):</span>
                  <span className="font-mono text-emerald-400 font-semibold flex items-center space-x-1">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>Verified Test UPI</span>
                  </span>
                </div>
                <input
                  type="text"
                  value={upiId}
                  onChange={(e) => setUpiId(e.target.value)}
                  className="w-full bg-[#1C1C1C] border border-[#333333] rounded-xl px-3.5 py-2 text-xs font-mono text-white focus:outline-none focus:border-[#CAFF33]"
                  placeholder="name@upi"
                />
                <div className="flex items-center justify-center space-x-4 pt-1">
                  <span className="px-3 py-1 rounded-lg bg-[#262626] text-[10px] font-semibold text-neutral-300 border border-[#333333]">
                    Google Pay
                  </span>
                  <span className="px-3 py-1 rounded-lg bg-[#262626] text-[10px] font-semibold text-neutral-300 border border-[#333333]">
                    PhonePe
                  </span>
                  <span className="px-3 py-1 rounded-lg bg-[#262626] text-[10px] font-semibold text-neutral-300 border border-[#333333]">
                    Paytm UPI
                  </span>
                </div>
              </div>
            )}

            {selectedMethod === "CARD" && (
              <div className="space-y-2.5">
                <div className="text-[11px] text-neutral-400">Pre-filled Sandbox Test Card:</div>
                <div className="p-3 bg-[#1C1C1C] rounded-xl border border-[#333333] space-y-2">
                  <div className="font-mono font-bold text-white text-xs">{cardNumber}</div>
                  <div className="flex justify-between text-[11px] font-mono text-neutral-400">
                    <span>EXP: {cardExpiry}</span>
                    <span>CVV: {cardCvv}</span>
                    <span className="text-[#CAFF33] font-bold">VISA PLATINUM</span>
                  </div>
                </div>
              </div>
            )}

            {selectedMethod === "ACCOUNT_DEBIT" && (
              <div className="space-y-1 text-xs">
                <div className="text-neutral-400">Primary BankCore360 Savings Account:</div>
                <div className="font-mono text-white font-bold">A/C #360100000001</div>
                <div className="text-[11px] text-neutral-500">
                  Amount ₹{totalAmount.toLocaleString()} will be debited and credited to Vault Treasury.
                </div>
              </div>
            )}

            {selectedMethod === "SANDBOX_FAST" && (
              <div className="space-y-1 text-xs">
                <div className="text-emerald-400 font-semibold flex items-center space-x-1.5">
                  <Zap className="w-3.5 h-3.5" />
                  <span>Zero-delay Sandbox Payment Simulator</span>
                </div>
                <div className="text-[11px] text-neutral-400">
                  Simulates immediate external clearing and executes atomic bank vault deposit.
                </div>
              </div>
            )}
          </div>

          {/* Processing Status Banner */}
          {isProcessing && (
            <div className="p-3 rounded-xl bg-[#CAFF33]/10 border border-[#CAFF33]/40 flex items-center space-x-2.5 text-xs text-[#CAFF33]">
              <div className="w-3.5 h-3.5 border-2 border-[#CAFF33] border-t-transparent rounded-full animate-spin"></div>
              <span className="font-medium animate-pulse">{processingStage}</span>
            </div>
          )}

          {/* Action CTA Button */}
          <button
            type="button"
            onClick={handlePay}
            disabled={isProcessing}
            className="w-full py-3.5 rounded-full bg-[#CAFF33] hover:bg-[#b8eb2a] text-black font-extrabold text-xs uppercase tracking-wider shadow-lg shadow-[#CAFF33]/20 transition flex items-center justify-center space-x-2 active:scale-98 disabled:opacity-50"
          >
            <span>{isProcessing ? "Authorizing Payment..." : `Authorize & Pay ₹${totalAmount.toLocaleString()}`}</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
