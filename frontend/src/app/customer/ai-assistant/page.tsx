"use client";

import React, { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import {
  Bot,
  Send,
  Sparkles,
  TrendingUp,
  Receipt,
  ShieldAlert,
  Calculator,
  BookOpen,
  RefreshCw,
  User,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle
} from "lucide-react";
import { askAiAssistant, AiChatResponse, getActiveUser } from "@/lib/api";

type AgentTab = "ALL" | "FINANCIAL_INSIGHTS" | "TRANSACTION_EXPLAINER" | "FRAUD_CLARIFICATION" | "LOAN_ASSISTANT" | "BANKING_KNOWLEDGE";

interface TabConfig {
  id: AgentTab;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  description: string;
  prompts: string[];
}

const AGENT_TABS: TabConfig[] = [
  {
    id: "FINANCIAL_INSIGHTS",
    label: "Financial Insights",
    icon: TrendingUp,
    description: "Real-time income, expenses, category spending breakdowns & savings rates",
    prompts: [
      "How much did I spend this month?",
      "Which category has the highest spending?",
      "Compare this month with last month.",
      "What is my current total available balance?"
    ]
  },
  {
    id: "TRANSACTION_EXPLAINER",
    label: "Transaction Explainer",
    icon: Receipt,
    description: "Detailed double-entry ledger breakdowns, debits, credits, and reversal entries",
    prompts: [
      "Explain my latest transaction.",
      "Why is this transaction pending?",
      "Explain my reversed transaction.",
      "Explain why funds were debited from my account."
    ]
  },
  {
    id: "FRAUD_CLARIFICATION",
    label: "Fraud Clarification",
    icon: ShieldAlert,
    description: "Transparent risk score explanations, velocity violations & security checks",
    prompts: [
      "Why was my transaction flagged?",
      "What factors increased my risk score?",
      "How do velocity checks protect my account?",
      "What is my current account security risk level?"
    ]
  },
  {
    id: "LOAN_ASSISTANT",
    label: "Loan Assistant",
    icon: Calculator,
    description: "Reducing-balance EMI calculations, product comparisons & eligibility checks",
    prompts: [
      "Calculate my EMI for ₹5,00,000 at 10.5% for 3 years",
      "Explain my loan eligibility criteria.",
      "What documents are required for a personal loan?",
      "What are the interest rates for home vs education loans?"
    ]
  },
  {
    id: "BANKING_KNOWLEDGE",
    label: "Banking Knowledge",
    icon: BookOpen,
    description: "Official schedule of charges, wire transfer fees, dispute rules & account limits",
    prompts: [
      "What are the account maintenance charges?",
      "Explain the bank's transfer policies.",
      "What is the dispute resolution process?",
      "What are the daily ATM withdrawal limits?"
    ]
  }
];

interface MessageItem {
  id: string;
  sender: "user" | "assistant";
  text: string;
  data?: AiChatResponse;
  agent?: string;
  timestamp: string;
  isTyping?: boolean;
}

// Markdown formatting helper for headers, bold, bullet points
function renderFormattedContent(text: string) {
  const lines = text.split("\n");

  return lines.map((line, lineIdx) => {
    const trimmed = line.trim();
    if (!trimmed) {
      return <div key={lineIdx} className="h-2" />;
    }

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
    if (trimmed.startsWith("#### ")) {
      return (
        <h5 key={lineIdx} className="font-semibold text-neutral-300 text-xs mt-2 mb-1">
          {formatInlineText(trimmed.replace(/^####\s+/, ""))}
        </h5>
      );
    }

    // Callout / blockquote alerts
    if (trimmed.startsWith("> [!NOTE]") || trimmed.startsWith("> [!IMPORTANT]") || trimmed.startsWith("> [!TIP]")) {
      const isTip = trimmed.includes("TIP");
      const isImportant = trimmed.includes("IMPORTANT");
      return (
        <div key={lineIdx} className={`my-2 p-2.5 rounded-lg border text-xs ${
          isImportant ? "bg-red-950/20 border-red-500/40 text-red-300" :
          isTip ? "bg-lime-950/20 border-lime-500/40 text-lime-300" :
          "bg-blue-950/20 border-blue-500/40 text-blue-300"
        }`}>
          <span className="font-bold uppercase tracking-wider text-[10px] block mb-0.5">
            {isImportant ? "⚠️ Important Compliance Notice" : isTip ? "💡 Banking Tip" : "ℹ️ Information"}
          </span>
        </div>
      );
    }

    if (trimmed.startsWith("> ")) {
      return (
        <p key={lineIdx} className="text-xs text-neutral-400 italic pl-3 border-l-2 border-neutral-700 my-1">
          {formatInlineText(trimmed.replace(/^>\s*/, ""))}
        </p>
      );
    }

    // Bullet lists
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

    // Numbered list
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

function formatInlineText(str: string): React.ReactNode[] {
  const cleaned = str.replace(/\*\s+\*/g, "").replace(/\*\*\*/g, "**");
  const parts: React.ReactNode[] = [];
  const regex = /(\*\*.*?\*\*|`.*?`)/g;
  let lastIndex = 0;
  let match;

  while ((match = regex.exec(cleaned)) !== null) {
    if (match.index > lastIndex) {
      parts.push(cleaned.substring(lastIndex, match.index));
    }
    const token = match[0];
    if (token.startsWith("**") && token.endsWith("**")) {
      parts.push(
        <strong key={match.index} className="font-bold text-white">
          {token.slice(2, -2).trim()}
        </strong>
      );
    } else if (token.startsWith("`") && token.endsWith("`")) {
      parts.push(
        <code key={match.index} className="px-1.5 py-0.5 rounded bg-[#262626] font-mono text-[#CAFF33] text-[11px]">
          {token.slice(1, -1)}
        </code>
      );
    }
    lastIndex = regex.lastIndex;
  }

  if (lastIndex < cleaned.length) {
    parts.push(cleaned.substring(lastIndex));
  }

  return parts;
}

export default function AiAssistantPage() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<AgentTab>("FINANCIAL_INSIGHTS");
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [inputQuery, setInputQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [threadId, setThreadId] = useState<string>("thread_init");

  useEffect(() => {
    const user = getActiveUser();
    if (user) {
      setCurrentUser(user);
      setThreadId(`thread_${user.username}_${Date.now()}`);
    } else {
      setThreadId(`thread_guest_${Date.now()}`);
    }
  }, []);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const streamResponse = (fullText: string, data?: AiChatResponse) => {
    const aiId = "a-" + Date.now();
    const agentName = data?.selectedAgent || "Supervisor";

    setMessages((prev) => [
      ...prev,
      {
        id: aiId,
        sender: "assistant",
        text: "",
        data,
        agent: agentName,
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        isTyping: true,
      },
    ]);

    let currentLength = 0;
    const chunkSize = 6;
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

  const handleSend = async (customQuery?: string) => {
    const q = customQuery || inputQuery;
    if (!q.trim() || loading) return;

    const userMsg: MessageItem = {
      id: "u-" + Date.now(),
      sender: "user",
      text: q.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!customQuery) setInputQuery("");
    setLoading(true);

    try {
      const responseData = await askAiAssistant(q.trim(), activeTab, threadId);
      const answerText = responseData.response || responseData.answer || "I processed your request.";
      streamResponse(answerText, responseData);
    } catch (err: any) {
      setLoading(false);
      setMessages((prev) => [
        ...prev,
        {
          id: "err-" + Date.now(),
          sender: "assistant",
          text: `⚠️ **Service Notice:** Unable to reach the AI Banking Assistant right now. Please ensure the backend services are running.`,
          timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        },
      ]);
    }
  };

  const handleResetConversation = () => {
    setMessages([]);
    setThreadId(`thread_${currentUser?.username || "guest"}_${Date.now()}`);
  };

  const currentTabConfig = AGENT_TABS.find((t) => t.id === activeTab) || AGENT_TABS[0];

  return (
    <div className="flex flex-col h-[calc(100vh-65px)] bg-[#0D0D0D] text-white">
      {/* Top Header Bar */}
      <div className="shrink-0 bg-[#141414] border-b border-[#262626] px-4 py-3 sm:px-6 z-20">
        <div className="max-w-6xl mx-auto flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-[#CAFF33]/15 text-[#CAFF33] flex items-center justify-center border border-[#CAFF33]/30 shadow-inner">
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-sm sm:text-base font-bold text-white tracking-wide">
                  BankCore360 Multi-Agent AI Assistant
                </h1>
                <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-mono bg-[#CAFF33]/10 text-[#CAFF33] border border-[#CAFF33]/20">
                  LangGraph Supervisor
                </span>
              </div>
              <p className="text-[11px] text-neutral-400">
                Five specialist agents • Customer-level data isolation • Zero hallucination
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {currentUser && (
              <span className="text-[11px] text-neutral-400 bg-[#1C1C1C] border border-[#2B2B2B] px-2.5 py-1 rounded-md flex items-center space-x-1.5">
                <User className="w-3.5 h-3.5 text-[#CAFF33]" />
                <span className="font-mono text-white font-medium">{currentUser.username}</span>
              </span>
            )}
            <button
              onClick={handleResetConversation}
              className="text-xs bg-[#1C1C1C] hover:bg-[#252525] border border-[#2E2E2E] hover:border-neutral-500 text-neutral-300 px-3 py-1.5 rounded-lg flex items-center space-x-1.5 transition active:scale-95"
              title="Reset conversation thread"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">New Chat</span>
            </button>
          </div>
        </div>

        {/* Quick Action Specialist Tabs */}
        <div className="max-w-6xl mx-auto mt-3 pt-2 border-t border-[#222222] flex items-center space-x-2 overflow-x-auto no-scrollbar">
          {AGENT_TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`whitespace-nowrap px-3 py-1.5 rounded-lg text-xs font-medium flex items-center space-x-2 transition ${
                  isActive
                    ? "bg-[#CAFF33] text-black font-semibold shadow-sm"
                    : "bg-[#1A1A1A] text-neutral-400 hover:text-white hover:bg-[#222222] border border-[#262626]"
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? "text-black" : "text-[#CAFF33]"}`} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Conversation Stream */}
      <div className="flex-1 overflow-y-auto px-4 py-4 sm:px-6 space-y-4">
        <div className="max-w-3xl mx-auto space-y-4">
          {messages.length === 0 ? (
            <div className="py-6 sm:py-10 text-center space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-[#CAFF33]/10 text-[#CAFF33] flex items-center justify-center mx-auto border border-[#CAFF33]/20 shadow-md">
                <Sparkles className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base sm:text-lg font-bold text-white">
                  {currentTabConfig.label}
                </h3>
                <p className="text-xs text-neutral-400 max-w-md mx-auto mt-1">
                  {currentTabConfig.description}
                </p>
              </div>

              {/* Contextual Suggested Questions */}
              <div className="pt-2 max-w-xl mx-auto grid grid-cols-1 sm:grid-cols-2 gap-2 text-left">
                {currentTabConfig.prompts.map((promptText, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSend(promptText)}
                    disabled={loading}
                    className="p-3 rounded-xl bg-[#141414] hover:bg-[#1A1A1A] border border-[#262626] hover:border-[#CAFF33]/40 text-xs text-neutral-300 hover:text-white transition group flex items-start space-x-2"
                  >
                    <span className="text-[#CAFF33] group-hover:scale-110 transition shrink-0">💬</span>
                    <span className="line-clamp-2">{promptText}</span>
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
                  <div className="max-w-[85%] bg-[#1E1E1E] text-white border border-[#2E2E2E] rounded-2xl rounded-tr-sm px-4 py-3 text-xs sm:text-sm shadow">
                    {m.text}
                  </div>
                ) : (
                  <div className="max-w-[92%] bg-[#141414] text-neutral-200 border border-[#262626] rounded-2xl rounded-tl-sm px-4 py-3.5 text-xs sm:text-sm shadow-md space-y-2">
                    {/* Agent Identification Badge */}
                    <div className="flex items-center justify-between pb-1.5 border-b border-[#222222]">
                      <span className="text-[10px] font-mono uppercase tracking-wider text-[#CAFF33] flex items-center space-x-1.5">
                        <Bot className="w-3.5 h-3.5" />
                        <span>{m.agent || "Specialist Banking Agent"}</span>
                      </span>
                      {m.data?.intent && (
                        <span className="text-[9px] font-mono px-2 py-0.5 rounded bg-[#1F1F1F] text-neutral-400 border border-[#2C2C2C]">
                          {m.data.intent}
                        </span>
                      )}
                    </div>

                    {/* Formatted Markdown Content */}
                    <div className="text-neutral-200">
                      {renderFormattedContent(m.text)}
                    </div>

                    {/* Grounded Policy Sources & Verifications */}
                    {m.data?.sources && m.data.sources.length > 0 && !m.isTyping && (
                      <div className="mt-3 pt-2.5 border-t border-[#222222]">
                        <span className="text-[10px] uppercase font-bold text-neutral-400 tracking-wider block mb-1.5">
                          Verified Audit Citations:
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {m.data.sources.map((s, idx) => (
                            <span
                              key={"src-" + idx}
                              className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-md bg-[#1C1C1C] border border-[#333333] text-[10px] font-mono text-[#CAFF33]"
                            >
                              <span>📄</span>
                              <span>{s.documentTitle || s.filename || "Audit Ledger"}</span>
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Clickable Suggested Next Steps */}
                    {m.data?.suggestedNextSteps && m.data.suggestedNextSteps.length > 0 && !m.isTyping && (
                      <div className="mt-2.5 pt-2 border-t border-[#222222] flex items-center flex-wrap gap-2">
                        <span className="text-[10px] uppercase font-bold text-neutral-400 tracking-wider mr-1">
                          Recommended Actions:
                        </span>
                        {m.data.suggestedNextSteps.map((step, idx) => (
                          <button
                            key={"step-" + idx}
                            onClick={() => handleSend(step)}
                            className="text-[11px] text-neutral-300 hover:text-white bg-[#1F1F1F] hover:bg-[#282828] px-2.5 py-1 rounded-full border border-[#333333] hover:border-[#CAFF33]/40 transition active:scale-95 text-left"
                          >
                            💬 {step}
                          </button>
                        ))}
                      </div>
                    )}

                    {/* Mandatory Disclaimer */}
                    {m.data?.disclaimer && !m.isTyping && (
                      <div className="mt-2 text-[10px] text-neutral-500 italic">
                        {m.data.disclaimer}
                      </div>
                    )}
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
              <span>LangGraph Supervisor is orchestrating specialist agent response...</span>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Pinned Bottom Input Bar */}
      <div className="shrink-0 bg-[#141414] border-t border-[#262626] px-4 py-3 sm:py-4 z-20">
        <div className="max-w-3xl mx-auto space-y-2">
          {/* Quick suggestions chips */}
          <div className="flex items-center space-x-2 overflow-x-auto no-scrollbar pb-1 text-[11px]">
            {currentTabConfig.prompts.map((promptText, idx) => (
              <button
                key={idx}
                onClick={() => handleSend(promptText)}
                disabled={loading}
                className="whitespace-nowrap px-3 py-1 rounded-full bg-[#1C1C1C] hover:bg-[#262626] hover:border-[#CAFF33]/50 text-neutral-400 hover:text-white border border-[#262626] transition active:scale-95 disabled:opacity-50"
              >
                {promptText}
              </button>
            ))}
          </div>

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
              placeholder={`Ask ${currentTabConfig.label.toLowerCase()} (e.g., '${currentTabConfig.prompts[0]}')...`}
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
            BankCore360 Multi-Agent System provides advisory analysis. All financial decisions rest solely with authorized human bank officials.
          </p>
        </div>
      </div>
    </div>
  );
}
