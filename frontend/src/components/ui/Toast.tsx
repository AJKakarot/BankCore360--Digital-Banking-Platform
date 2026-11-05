"use client";

import React, { useState, useEffect } from "react";
import { CheckCircle2, AlertCircle, Info, Loader2, X } from "lucide-react";

export type ToastType = "success" | "error" | "info" | "loading";

export interface ToastItem {
  id: string;
  type: ToastType;
  title: string;
  message?: string;
  duration?: number;
}

type ToastListener = (toast: ToastItem) => void;
type DismissListener = (id: string) => void;

const listeners: Set<ToastListener> = new Set();
const dismissListeners: Set<DismissListener> = new Set();

export const toast = {
  success: (title: string, message?: string, duration = 4000) => {
    const id = "t-" + Date.now() + "-" + Math.random().toString(36).substring(2, 6);
    listeners.forEach((fn) => fn({ id, type: "success", title, message, duration }));
    return id;
  },
  error: (title: string, message?: string, duration = 5000) => {
    const id = "t-" + Date.now() + "-" + Math.random().toString(36).substring(2, 6);
    listeners.forEach((fn) => fn({ id, type: "error", title, message, duration }));
    return id;
  },
  info: (title: string, message?: string, duration = 3500) => {
    const id = "t-" + Date.now() + "-" + Math.random().toString(36).substring(2, 6);
    listeners.forEach((fn) => fn({ id, type: "info", title, message, duration }));
    return id;
  },
  loading: (title: string, message?: string) => {
    const id = "t-" + Date.now() + "-" + Math.random().toString(36).substring(2, 6);
    listeners.forEach((fn) => fn({ id, type: "loading", title, message, duration: 0 }));
    return id;
  },
  dismiss: (id: string) => {
    dismissListeners.forEach((fn) => fn(id));
  },
};

export function ToastContainer() {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    const handleAdd = (item: ToastItem) => {
      setToasts((prev) => [...prev, item]);
      if (item.duration && item.duration > 0) {
        setTimeout(() => {
          setToasts((prev) => prev.filter((t) => t.id !== item.id));
        }, item.duration);
      }
    };

    const handleDismiss = (id: string) => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    };

    listeners.add(handleAdd);
    dismissListeners.add(handleDismiss);

    return () => {
      listeners.delete(handleAdd);
      dismissListeners.delete(handleDismiss);
    };
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-20 sm:top-24 left-3 right-3 sm:left-auto sm:right-6 z-[9999] flex flex-col space-y-2.5 pointer-events-none sm:max-w-sm sm:w-full">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`pointer-events-auto transform transition-all duration-300 ease-out translate-x-0 bg-[#1C1C1C] border rounded-2xl shadow-2xl p-4 flex items-start space-x-3 backdrop-blur-md animate-fade-in ${
            t.type === "success"
              ? "border-[#CAFF33]/50 shadow-[0_4px_24px_rgba(202,255,51,0.18)]"
              : t.type === "error"
              ? "border-red-600/60 shadow-[0_4px_24px_rgba(220,38,38,0.2)]"
              : t.type === "loading"
              ? "border-[#333333] shadow-lg"
              : "border-[#333333] text-neutral-300"
          }`}
        >
          {t.type === "success" && (
            <CheckCircle2 className="w-5 h-5 text-[#CAFF33] shrink-0 mt-0.5" />
          )}
          {t.type === "error" && (
            <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
          )}
          {t.type === "info" && (
            <Info className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
          )}
          {t.type === "loading" && (
            <Loader2 className="w-5 h-5 text-[#CAFF33] animate-spin shrink-0 mt-0.5" />
          )}

          <div className="flex-1 min-w-0 pr-1">
            <h5 className="text-xs sm:text-sm font-bold text-white tracking-tight leading-tight">
              {t.title}
            </h5>
            {t.message && (
              <p
                className={`text-[11px] sm:text-xs mt-1 leading-relaxed ${
                  t.type === "error" ? "text-red-300" : "text-neutral-400"
                }`}
              >
                {t.message}
              </p>
            )}
          </div>

          <button
            onClick={() => setToasts((prev) => prev.filter((item) => item.id !== t.id))}
            className="text-neutral-500 hover:text-white p-1 rounded-full transition shrink-0"
            title="Dismiss"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
    </div>
  );
}
