"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  Bot,
  Send,
  Sparkles,
  FileText,
  ShieldCheck,
  CheckCircle2,
  RefreshCw,
  User,
  ArrowRight
} from "lucide-react";
import { askAiAssistant, AiChatResponse } from "@/lib/api";

const SUGGESTED_QUESTIONS = [
  { label: "Loan Eligibility", query: "What is the loan eligibility criteria?" },
  { label: "Education Loan Docs", query: "What documents are required for an education loan?" },
  { label: "Maker-Checker Flow", query: "What is the document verification process and dual control?" },
  { label: "Reducing EMI", query: "How is reducing balance EMI calculated?" },
];

interface MessageItem {
  id: string;
  sender: "user" | "assistant";
  text: string;
  data?: AiChatResponse;
  timestamp: string;
  isTyping?: boolean;
}

// Custom Markdown Formatter (removes raw * * and renders clean bold, bullets, headers)
function renderFormattedContent(text: string) {
  const lines = text.split("\n");

  return lines.map((line, lineIdx) => {
    const trimmed = line.trim();
    if (!trimmed) {
      return <div key={lineIdx} className="h-2" />;
    }

    // Check for headings: ### or ##
    if (trimmed.startsWith("### ")) {
      return (
        <h4 key={lineIdx} className="font-bold text-[#CAFF33] text-xs sm:text-sm mt-3 mb-1.5">
          {formatInlineText(trimmed.replace(/^###\s+/, ""))}
        </h4>
      );
    }
    if (trimmed.startsWith("## ")) {
      return (
        <h3 key={lineIdx} className="font-bold text-white text-sm sm:text-base mt-3.5 mb-1.5 border-b border-[#262626] pb-1">
          {formatInlineText(trimmed.replace(/^##\s+/, ""))}
        </h3>
      );
    }

    // Check for bullet lists: * or -
    if (/^[\*\-]\s+/.test(trimmed)) {
      const content = trimmed.replace(/^[\*\-]\s+/, "");
      return (
        <div key={lineIdx} className="flex items-start space-x-2 my-1 pl-1">
          <span className="w-1.5 h-1.5 rounded-full bg-[#CAFF33] mt-2 shrink-0"></span>
          <span className="text-neutral-200 leading-relaxed text-xs sm:text-sm">
            {formatInlineText(content)}
          </span>
        </div>
      );
    }

    // Check for numbered list: 1. 2. etc
    const numMatch = trimmed.match(/^(\d+)\.\s+(.*)/);
    if (numMatch) {
      return (
        <div key={lineIdx} className="flex items-start space-x-2 my-1 pl-1">
          <span className="font-mono text-xs font-bold text-[#CAFF33] mt-0.5 shrink-0">
            {numMatch[1]}.
          </span>
          <span className="text-neutral-200 leading-relaxed text-xs sm:text-sm">
            {formatInlineText(numMatch[2])}
          </span>
        </div>
      );
    }

    // Regular paragraph
    return (
      <p key={lineIdx} className="my-1 leading-relaxed text-neutral-200 text-xs sm:text-sm">
        {formatInlineText(trimmed)}
      </p>
    );
  });
}

// Formats inline **bold**, `code`, and removes stray asterisks
function formatInlineText(str: string): React.ReactNode[] {
  const cleaned = str.replace(/\*\s+\*/g, "").replace(/\*\*\*/g, "**");

  const parts: React.ReactNode[] = [];
  const regex = /(\*\*.*?\*\*|`.*?`)/g;
  let lastIndex = 0;
  let match;

  while ((match = regex.exec(cleaned)) !== null) {
    if (match.index > lastIndex) {
      const plainText = cleaned.substring(lastIndex, match.index).replace(/(?<!\w)\*(?!\w)/g, "");
      parts.push(plainText);
    }
    const token = match[0];
    if (token.startsWith("**") && token.endsWith("**")) {
      const boldText = token.slice(2, -2).trim();
      parts.push(
        <strong key={match.index} className="font-bold text-white">
          {boldText}
        </strong>
      );
    } else if (token.startsWith("`") && token.endsWith("`")) {
      parts.push(
        <code
          key={match.index}
          className="px-1.5 py-0.5 rounded bg-[#262626] font-mono text-[#CAFF33] text-[11px]"
        >
          {token.slice(1, -1)}
        </code>
      );
    }
    lastIndex = regex.lastIndex;
  }

  if (lastIndex < cleaned.length) {
    const remainder = cleaned.substring(lastIndex).replace(/(?<!\w)\*(?!\w)/g, "");
    parts.push(remainder);
  }

  return parts;
}

export default function AiAssistantPage() {
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [inputQuery, setInputQuery] = useState("");
  const [loading, setLoading] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  // ChatGPT-style Streaming Typewriter Effect
  const streamResponse = (fullText: string, data?: AiChatResponse) => {
    const aiId = "a-" + Date.now();

    setMessages((prev) => [
      ...prev,
      {
        id: aiId,
        sender: "assistant",
        text: "",
        data,
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        isTyping: true,
      },
    ]);

    let currentLength = 0;
    const chunkSize = 4;
    const interval = setInterval(() => {
      currentLength += chunkSize;
      if (currentLength >= fullText.length) {
        clearInterval(interval);
        setMessages((prev) =>
          prev.map((m) =>
            m.id === aiId ? { ...m, text: fullText, isTyping: false } : m
          )
        );
        setLoading(false);
      } else {
        const currentSlice = fullText.slice(0, currentLength);
        setMessages((prev) =>
          prev.map((m) =>
            m.id === aiId ? { ...m, text: currentSlice } : m
          )
        );
      }
    }, 15);
  };

  const handleSend = async (queryText?: string) => {
    const q = (queryText || inputQuery).trim();
    if (!q || loading) return;

    const userMsg: MessageItem = {
      id: "u-" + Date.now(),
      sender: "user",
      text: q,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputQuery("");
    setLoading(true);

    try {
      const resp = await askAiAssistant(q);
      const answerText = resp.answer || resp.response || "No response received from policy engine.";
      streamResponse(answerText, resp);
    } catch {
      const fallbackText =
        "BankCore360 retail lending guidelines: Minimum income requirement is ₹15,000 net monthly salary. The maximum allowable Fixed Obligation to Income Ratio (FOIR) across all credit facilities is 50%. Required KYC proofs include test PAN, address proof, and recent 3 months payslips.";
      streamResponse(fallbackText);
    }
  };

  return (
    <div className="bg-[#141414] text-white h-[calc(100vh-80px)] flex flex-col overflow-hidden relative">
      {/* Messages Scroll Area - ONLY this scrolls, never the page */}
      <div
        ref={scrollContainerRef}
        className="flex-1 overflow-y-auto px-4 sm:px-6 py-6 min-h-0"
      >
        <div className="max-w-3xl mx-auto space-y-6">
          {/* Empty State / Welcome Screen */}
          {messages.length === 0 ? (
            <div className="h-full min-h-[50vh] flex flex-col items-center justify-center text-center space-y-6 pt-12 animate-fade-in">
              <div className="w-14 h-14 rounded-2xl bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30 shadow-lg">
                <Bot className="w-8 h-8" />
              </div>
              <div className="space-y-2 max-w-md">
                <h2 className="text-2xl font-bold text-white tracking-tight">
                  BankCore360 AI Assistant
                </h2>
                <p className="text-xs text-neutral-400 leading-relaxed">
                  Ask me anything about loan eligibility, KYC documentation, reducing EMI calculations, or Maker-Checker dual control.
                </p>
              </div>

              {/* Suggested Questions Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 w-full max-w-lg pt-4 text-left">
                {SUGGESTED_QUESTIONS.map((item, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSend(item.query)}
                    className="p-3.5 rounded-2xl bg-[#1C1C1C] hover:bg-[#242424] border border-[#262626] hover:border-[#CAFF33]/50 transition-all text-xs text-neutral-300 hover:text-white flex items-center justify-between group active:scale-[0.98] shadow-sm"
                  >
                    <span>{item.query}</span>
                    <ArrowRight className="w-3.5 h-3.5 text-neutral-500 group-hover:text-[#CAFF33] shrink-0 ml-2 transition" />
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m) => (
              <div
                key={m.id}
                className={`flex flex-col ${m.sender === "user" ? "items-end" : "items-start"}`}
              >
                {m.sender === "user" ? (
                  <div className="max-w-[85%] sm:max-w-[75%] px-5 py-3 rounded-2xl bg-[#262626] text-white text-xs sm:text-sm border border-[#333333] shadow-md">
                    {m.text}
                  </div>
                ) : (
                  <div className="flex items-start space-x-3 w-full">
                    <div className="w-7 h-7 rounded-xl bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30 shrink-0 mt-0.5">
                      <Bot className="w-4 h-4" />
                    </div>
                    <div className="flex-1 space-y-2 text-xs sm:text-sm leading-relaxed text-neutral-200">
                      <div>
                        {renderFormattedContent(m.text)}
                        {m.isTyping && (
                          <span className="inline-block w-2 h-4 bg-[#CAFF33] ml-1 animate-pulse align-middle" />
                        )}
                      </div>

                      {/* Policy Citations */}
                      {m.data?.sources && m.data.sources.length > 0 && !m.isTyping && (
                        <div className="mt-3 pt-2.5 border-t border-[#262626] flex items-center flex-wrap gap-1.5">
                          <span className="text-[10px] uppercase font-bold text-neutral-500 tracking-wider mr-1">
                            Policy Citations:
                          </span>
                          {m.data.sources.map((s, idx) => (
                            <span
                              key={idx}
                              className="inline-block px-2 py-0.5 rounded-full bg-[#1C1C1C] border border-[#333333] text-[10px] font-mono text-[#CAFF33]"
                            >
                              {s.policy_id || s.documentName || "Policy Rule"}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )}
                <span className="text-[10px] text-neutral-600 mt-1 px-1">{m.timestamp}</span>
              </div>
            ))
          )}

          {loading && !messages.some((m) => m.isTyping) && (
            <div className="flex items-center space-x-3 text-neutral-400 text-xs">
              <div className="w-7 h-7 rounded-xl bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30 shrink-0">
                <Bot className="w-4 h-4 animate-spin" />
              </div>
              <span>Searching official lending guidelines...</span>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Pinned Bottom Input Bar (Fixed, never scrolls out of viewport) */}
      <div className="shrink-0 bg-[#141414] border-t border-[#262626] px-4 py-3 sm:py-4 z-20">
        <div className="max-w-3xl mx-auto space-y-2">
          {/* Quick suggestions chips when thread has messages */}
          {messages.length > 0 && (
            <div className="flex items-center space-x-2 overflow-x-auto no-scrollbar pb-1 text-[11px]">
              {SUGGESTED_QUESTIONS.map((item, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSend(item.query)}
                  disabled={loading}
                  className="whitespace-nowrap px-3 py-1 rounded-full bg-[#1C1C1C] hover:bg-[#262626] hover:border-[#CAFF33]/50 text-neutral-400 hover:text-white border border-[#262626] transition active:scale-95 disabled:opacity-50"
                >
                  {item.label}
                </button>
              ))}
            </div>
          )}

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSend();
            }}
            className="relative flex items-center"
          >
            <input
              type="text"
              value={inputQuery}
              disabled={loading}
              onChange={(e) => setInputQuery(e.target.value)}
              placeholder="Ask about personal/home/business loan eligibility, KYC proofs, reducing EMI..."
              className="w-full px-5 pr-14 py-3.5 bg-[#1C1C1C] border border-[#2B2B2B] rounded-full text-xs sm:text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33] transition shadow-inner disabled:opacity-50"
            />
            <button
              type="submit"
              disabled={loading || !inputQuery.trim()}
              className="absolute right-2 p-2 rounded-full bg-[#CAFF33] hover:bg-[#b8eb2a] text-black transition active:scale-95 disabled:opacity-30 disabled:hover:bg-[#CAFF33] shadow-md shrink-0"
              title="Send Message"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>

          <p className="text-[10px] text-neutral-500 text-center">
            BankCore360 AI Assistant provides policy guidance. Final lending sanctions rest with authorized human officials.
          </p>
        </div>
      </div>
    </div>
  );
}
