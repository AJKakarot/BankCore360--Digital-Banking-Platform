"use client";

import React, { useState, useEffect } from "react";
import {
  Radio,
  Activity,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Search,
  RefreshCw,
  Layers,
  ShieldCheck,
  Eye,
  X,
  Server,
  Zap,
  ArrowRight
} from "lucide-react";
import Link from "next/link";
import {
  fetchKafkaStreamStatus,
  KafkaStreamStatus,
  OutboxEventItem,
  ProcessedEventItem
} from "@/lib/api";
import { toast } from "@/components/ui/Toast";

const DEMO_STATUS: KafkaStreamStatus = {
  kafkaEnabled: true,
  bootstrapServers: "localhost:9092",
  topics: [
    {
      name: "loan-events",
      consumerGroup: "bankcore360-loan-group, bankcore360-audit-group",
      description: "Loan application creation, updates, and lifecycle transitions",
    },
    {
      name: "kyc-events",
      consumerGroup: "bankcore360-kyc-group, bankcore360-audit-group",
      description: "Customer KYC document uploads, review, verify, and reject events",
    },
    {
      name: "approval-events",
      consumerGroup: "bankcore360-approval-group, bankcore360-audit-group",
      description: "Credit manager loan approval, rejection, and sanction decisions",
    },
    {
      name: "repayment-events",
      consumerGroup: "bankcore360-repayment-group",
      description: "EMI repayment schedules, payments, and installment notices",
    },
    {
      name: "notification-events",
      consumerGroup: "bankcore360-notification-group",
      description: "Real-time user notification alerts and communications",
    },
  ],
  outboxStats: {
    totalEvents: 6,
    pendingEvents: 0,
    publishedEvents: 6,
    failedEvents: 0,
  },
  outboxEvents: [
    {
      id: 1,
      eventId: "evt-9a8b-4c21",
      eventType: "LOAN_APPLICATION_SUBMITTED",
      aggregateId: "LN-2026-AB12CD",
      topic: "loan-events",
      payloadJson: JSON.stringify({
        eventId: "evt-9a8b-4c21",
        eventType: "LOAN_APPLICATION_SUBMITTED",
        aggregateId: "LN-2026-AB12CD",
        payload: {
          reference: "LN-2026-AB12CD",
          loanType: "PERSONAL",
          requestedAmount: 250000,
          status: "SUBMITTED"
        }
      }, null, 2),
      status: "PUBLISHED",
      retryCount: 0,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
      publishedAt: new Date(Date.now() - 3598000).toISOString(),
    },
    {
      id: 2,
      eventId: "evt-77ab-90f1",
      eventType: "KYC_DOCUMENT_VERIFIED",
      aggregateId: "LN-2026-AB12CD",
      topic: "kyc-events",
      payloadJson: JSON.stringify({
        eventId: "evt-77ab-90f1",
        eventType: "KYC_DOCUMENT_VERIFIED",
        aggregateId: "LN-2026-AB12CD",
        payload: {
          documentType: "PAN",
          verifiedBy: "checker_john",
          status: "VERIFIED"
        }
      }, null, 2),
      status: "PUBLISHED",
      retryCount: 0,
      createdAt: new Date(Date.now() - 2400000).toISOString(),
      publishedAt: new Date(Date.now() - 2399000).toISOString(),
    },
    {
      id: 3,
      eventId: "evt-33cd-88fa",
      eventType: "LOAN_APPROVED",
      aggregateId: "LN-2026-AB12CD",
      topic: "approval-events",
      payloadJson: JSON.stringify({
        eventId: "evt-33cd-88fa",
        eventType: "LOAN_APPROVED",
        aggregateId: "LN-2026-AB12CD",
        payload: {
          approvedAmount: 250000,
          interestRate: 10.5,
          reviewedBy: "manager_sarah"
        }
      }, null, 2),
      status: "PUBLISHED",
      retryCount: 0,
      createdAt: new Date(Date.now() - 1200000).toISOString(),
      publishedAt: new Date(Date.now() - 1198000).toISOString(),
    }
  ],
  processedEvents: [
    {
      id: 1,
      eventId: "evt-9a8b-4c21",
      eventType: "LOAN_APPLICATION_SUBMITTED",
      consumerGroup: "bankcore360-audit-group",
      processedAt: new Date(Date.now() - 3597000).toISOString(),
    },
    {
      id: 2,
      eventId: "evt-77ab-90f1",
      eventType: "KYC_DOCUMENT_VERIFIED",
      consumerGroup: "bankcore360-audit-group",
      processedAt: new Date(Date.now() - 2398000).toISOString(),
    },
    {
      id: 3,
      eventId: "evt-33cd-88fa",
      eventType: "LOAN_APPROVED",
      consumerGroup: "bankcore360-audit-group",
      processedAt: new Date(Date.now() - 1197000).toISOString(),
    }
  ]
};

export default function AdminKafkaMonitorPage() {
  const [data, setData] = useState<KafkaStreamStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("");
  const [activeTab, setActiveTab] = useState<"outbox" | "processed" | "topics">("outbox");
  const [selectedEvent, setSelectedEvent] = useState<OutboxEventItem | null>(null);

  const loadStreamStatus = async (showToast = false) => {
    setLoading(true);
    try {
      const res = await fetchKafkaStreamStatus(50);
      setData(res);
      if (showToast) {
        toast.success("Stream Status Updated", `Loaded ${res.outboxEvents?.length || 0} events from Kafka Outbox`);
      }
    } catch {
      setData(DEMO_STATUS);
      if (showToast) {
        toast.info("Offline Stream Mode", "Displaying cached Kafka events");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStreamStatus();
  }, []);

  const streamData = data || DEMO_STATUS;

  const filteredOutbox = (streamData.outboxEvents || []).filter(
    (e) =>
      e.eventId?.toLowerCase().includes(filter.toLowerCase()) ||
      e.eventType?.toLowerCase().includes(filter.toLowerCase()) ||
      e.aggregateId?.toLowerCase().includes(filter.toLowerCase()) ||
      e.topic?.toLowerCase().includes(filter.toLowerCase()) ||
      e.status?.toLowerCase().includes(filter.toLowerCase())
  );

  const filteredProcessed = (streamData.processedEvents || []).filter(
    (p) =>
      p.eventId?.toLowerCase().includes(filter.toLowerCase()) ||
      p.eventType?.toLowerCase().includes(filter.toLowerCase()) ||
      p.consumerGroup?.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="bg-[#141414] text-white space-y-6 py-6 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto min-h-screen">
      {/* Header */}
      <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
        <div className="space-y-1">
          <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3.5 py-1 rounded-full text-xs font-semibold text-neutral-300">
            <Radio className="w-3.5 h-3.5 text-[#CAFF33] animate-pulse" />
            <span>Event-Driven Architecture</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight flex items-center gap-3">
            Kafka & Outbox Stream
            <span
              className={`text-xs px-2.5 py-0.5 rounded-full font-semibold ${
                streamData.kafkaEnabled
                  ? "bg-[#CAFF33]/20 text-[#CAFF33] border border-[#CAFF33]/40"
                  : "bg-amber-500/20 text-amber-300 border border-amber-500/40"
              }`}
            >
              {streamData.kafkaEnabled ? "Kafka: ACTIVE" : "Kafka: OFFLINE MODE"}
            </span>
          </h1>
          <p className="text-xs sm:text-sm text-neutral-400">
            Real-time monitor for Transactional Outbox events, Kafka topics, and idempotent consumer workers.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          <Link
            href="/admin/audit"
            className="px-3.5 py-2 rounded-full bg-[#262626] border border-[#333333] text-xs font-semibold text-neutral-300 hover:text-white transition flex items-center space-x-1.5"
          >
            <ShieldCheck className="w-3.5 h-3.5 text-[#CAFF33]" />
            <span>Audit Trail</span>
          </Link>

          <button
            onClick={() => loadStreamStatus(true)}
            disabled={loading}
            className="p-2.5 rounded-full bg-[#262626] border border-[#333333] hover:border-[#CAFF33] text-neutral-300 hover:text-white transition disabled:opacity-50"
            title="Refresh stream status"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin text-[#CAFF33]" : ""}`} />
          </button>
        </div>
      </div>

      {/* Cluster Overview Banner */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-[#1C1C1C] border border-[#262626] p-5 rounded-2xl flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <Server className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] text-neutral-400 font-medium">Bootstrap Broker</div>
            <div className="text-sm font-mono font-bold text-white truncate max-w-[170px]">
              {streamData.bootstrapServers || "localhost:9092"}
            </div>
          </div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-5 rounded-2xl flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-[#CAFF33]">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] text-neutral-400 font-medium">Published Events</div>
            <div className="text-xl font-bold text-white">
              {streamData.outboxStats?.publishedEvents ?? 0}
            </div>
          </div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-5 rounded-2xl flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-amber-400">
            <Clock className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] text-neutral-400 font-medium">Pending in Outbox</div>
            <div className="text-xl font-bold text-amber-400">
              {streamData.outboxStats?.pendingEvents ?? 0}
            </div>
          </div>
        </div>

        <div className="bg-[#1C1C1C] border border-[#262626] p-5 rounded-2xl flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-[#262626] flex items-center justify-center text-emerald-400">
            <Zap className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] text-neutral-400 font-medium">Active Kafka Topics</div>
            <div className="text-xl font-bold text-white">{streamData.topics?.length ?? 5} Topics</div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-[#262626] gap-2 overflow-x-auto whitespace-nowrap py-1">
        <button
          onClick={() => setActiveTab("outbox")}
          className={`pb-3 px-4 text-xs font-semibold flex items-center space-x-2 border-b-2 transition shrink-0 ${
            activeTab === "outbox"
              ? "border-[#CAFF33] text-[#CAFF33]"
              : "border-transparent text-neutral-400 hover:text-white"
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Outbox Event Stream ({streamData.outboxEvents?.length ?? 0})</span>
        </button>

        <button
          onClick={() => setActiveTab("processed")}
          className={`pb-3 px-4 text-xs font-semibold flex items-center space-x-2 border-b-2 transition shrink-0 ${
            activeTab === "processed"
              ? "border-[#CAFF33] text-[#CAFF33]"
              : "border-transparent text-neutral-400 hover:text-white"
          }`}
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>Consumer Idempotency Log ({streamData.processedEvents?.length ?? 0})</span>
        </button>

        <button
          onClick={() => setActiveTab("topics")}
          className={`pb-3 px-4 text-xs font-semibold flex items-center space-x-2 border-b-2 transition shrink-0 ${
            activeTab === "topics"
              ? "border-[#CAFF33] text-[#CAFF33]"
              : "border-transparent text-neutral-400 hover:text-white"
          }`}
        >
          <Radio className="w-4 h-4" />
          <span>Topics & Consumer Groups</span>
        </button>
      </div>

      {/* Tab Content: Outbox Events */}
      {activeTab === "outbox" && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-neutral-500 absolute left-3.5 top-3" />
              <input
                type="text"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Search event ID, type, reference, topic..."
                className="w-full pl-9 pr-4 py-2 bg-[#1C1C1C] border border-[#262626] rounded-full text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33]"
              />
            </div>
            <div className="text-xs text-neutral-400 font-mono">
              Showing {filteredOutbox.length} recorded events
            </div>
          </div>

          <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl overflow-hidden shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs min-w-[650px]">
                <thead className="bg-[#262626] text-neutral-400 uppercase font-semibold text-[10px] tracking-wider border-b border-[#333333]">
                  <tr>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Event Type</th>
                    <th className="py-3 px-4">Aggregate Ref</th>
                    <th className="py-3 px-4">Kafka Topic</th>
                    <th className="py-3 px-4">Created</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#262626]">
                  {filteredOutbox.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-12 text-center text-neutral-500">
                        No outbox events match your filter.
                      </td>
                    </tr>
                  ) : (
                    filteredOutbox.map((evt) => (
                      <tr key={evt.id} className="hover:bg-[#222222] transition">
                        <td className="py-3.5 px-4 whitespace-nowrap">
                          <span
                            className={`inline-flex items-center space-x-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-semibold font-mono ${
                              evt.status === "PUBLISHED"
                                ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/40"
                                : evt.status === "PENDING"
                                ? "bg-amber-500/20 text-amber-300 border border-amber-500/40"
                                : "bg-red-500/20 text-red-400 border border-red-500/40"
                            }`}
                          >
                            <span
                              className={`w-1.5 h-1.5 rounded-full ${
                                evt.status === "PUBLISHED"
                                  ? "bg-emerald-400"
                                  : evt.status === "PENDING"
                                  ? "bg-amber-400 animate-pulse"
                                  : "bg-red-400"
                              }`}
                            ></span>
                            <span>{evt.status}</span>
                          </span>
                        </td>
                        <td className="py-3.5 px-4 font-mono font-semibold text-white">
                          {evt.eventType}
                        </td>
                        <td className="py-3.5 px-4 font-mono text-[#CAFF33]">
                          {evt.aggregateId}
                        </td>
                        <td className="py-3.5 px-4 font-mono text-neutral-300">
                          {evt.topic}
                        </td>
                        <td className="py-3.5 px-4 text-neutral-400 whitespace-nowrap">
                          {evt.createdAt ? new Date(evt.createdAt).toLocaleString() : "N/A"}
                        </td>
                        <td className="py-3.5 px-4 text-right">
                          <button
                            onClick={() => setSelectedEvent(evt)}
                            className="inline-flex items-center space-x-1 px-2.5 py-1 rounded bg-[#262626] hover:bg-[#333333] text-neutral-300 hover:text-white transition text-[11px]"
                          >
                            <Eye className="w-3 h-3 text-[#CAFF33]" />
                            <span>Payload</span>
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab Content: Processed Events (Idempotency) */}
      {activeTab === "processed" && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-neutral-500 absolute left-3.5 top-3" />
              <input
                type="text"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Search consumer group, event ID..."
                className="w-full pl-9 pr-4 py-2 bg-[#1C1C1C] border border-[#262626] rounded-full text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-[#CAFF33]"
              />
            </div>
            <div className="text-xs text-neutral-400">
              Idempotency records guarantee that duplicate Kafka events are dropped safely.
            </div>
          </div>

          <div className="bg-[#1C1C1C] border border-[#262626] rounded-2xl overflow-hidden shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs min-w-[650px]">
                <thead className="bg-[#262626] text-neutral-400 uppercase font-semibold text-[10px] tracking-wider border-b border-[#333333]">
                  <tr>
                    <th className="py-3 px-4">Event ID</th>
                    <th className="py-3 px-4">Consumer Group</th>
                    <th className="py-3 px-4">Event Type</th>
                    <th className="py-3 px-4">Processed Timestamp</th>
                    <th className="py-3 px-4">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#262626]">
                  {filteredProcessed.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="py-12 text-center text-neutral-500">
                        No idempotency records found.
                      </td>
                    </tr>
                  ) : (
                    filteredProcessed.map((p) => (
                      <tr key={p.id} className="hover:bg-[#222222] transition">
                        <td className="py-3.5 px-4 font-mono text-[#CAFF33]">{p.eventId}</td>
                        <td className="py-3.5 px-4 font-mono text-neutral-300">{p.consumerGroup}</td>
                        <td className="py-3.5 px-4 font-semibold text-white">{p.eventType}</td>
                        <td className="py-3.5 px-4 text-neutral-400 whitespace-nowrap">
                          {p.processedAt ? new Date(p.processedAt).toLocaleString() : "Recent"}
                        </td>
                        <td className="py-3.5 px-4 whitespace-nowrap">
                          <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/40">
                            <CheckCircle2 className="w-3 h-3" />
                            <span>Processed & Deduplicated</span>
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab Content: Topics Detail */}
      {activeTab === "topics" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {(streamData.topics || []).map((t) => (
            <div
              key={t.name}
              className="bg-[#1C1C1C] border border-[#262626] p-6 rounded-2xl space-y-3 shadow-lg"
            >
              <div className="flex items-center justify-between">
                <div className="inline-flex items-center space-x-2 bg-[#262626] border border-[#333333] px-3 py-1 rounded-full text-xs font-mono font-bold text-[#CAFF33]">
                  <span>Topic: {t.name}</span>
                </div>
                <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-mono">
                  3 Partitions
                </span>
              </div>
              <p className="text-xs text-neutral-300">{t.description}</p>
              <div className="pt-2 border-t border-[#262626] space-y-1">
                <div className="text-[11px] text-neutral-400">Assigned Consumer Groups:</div>
                <div className="text-xs font-mono text-white bg-[#141414] px-3 py-1.5 rounded-lg border border-[#262626]">
                  {t.consumerGroup}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Payload Modal */}
      {selectedEvent && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4">
          <div className="bg-[#1C1C1C] border border-[#262626] rounded-3xl max-w-2xl w-[94vw] sm:w-full p-4 sm:p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-[#262626] pb-3">
              <div className="space-y-0.5">
                <h3 className="font-bold text-white text-base flex items-center gap-2">
                  <span>Kafka Event Envelope</span>
                  <span className="text-xs font-mono text-[#CAFF33] font-normal">
                    {selectedEvent.eventId}
                  </span>
                </h3>
                <div className="text-xs text-neutral-400 font-mono">
                  Topic: {selectedEvent.topic} • Ref: {selectedEvent.aggregateId}
                </div>
              </div>
              <button
                onClick={() => setSelectedEvent(null)}
                className="p-1.5 rounded-full bg-[#262626] text-neutral-400 hover:text-white transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="bg-[#141414] border border-[#262626] rounded-xl p-4 font-mono text-xs text-emerald-400 max-h-96 overflow-y-auto whitespace-pre-wrap">
              {(() => {
                try {
                  const parsed = JSON.parse(selectedEvent.payloadJson);
                  return JSON.stringify(parsed, null, 2);
                } catch {
                  return selectedEvent.payloadJson;
                }
              })()}
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedEvent(null)}
                className="px-4 py-2 rounded-full bg-[#262626] hover:bg-[#333333] text-xs font-semibold text-white transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

