"use client";

import { useCallback, useEffect, useState } from "react";
import { RefreshCw, Server } from "lucide-react";
import type { Locale } from "@/lib/i18n";

type BackendHealth = {
  backendBaseUrl: string;
  checkedAt: string;
  error?: string;
  online: boolean;
  service: string;
  status: string;
};

type BackendHealthCardProps = {
  locale: Locale;
};

export function BackendHealthCard({ locale }: BackendHealthCardProps) {
  const [health, setHealth] = useState<BackendHealth | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const requestHealth = useCallback(async () => {
    try {
      const response = await fetch("/api/backend-health", {
        cache: "no-store",
      });
      const data = (await response.json()) as BackendHealth;

      return data;
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Frontend health check failed";

      return {
        backendBaseUrl: "unknown",
        checkedAt: new Date().toISOString(),
        error: message,
        online: false,
        service: "backend",
        status: "DOWN",
      };
    }
  }, []);

  const loadHealth = useCallback(async () => {
    setIsLoading(true);

    try {
      setHealth(await requestHealth());
    } finally {
      setIsLoading(false);
    }
  }, [requestHealth]);

  useEffect(() => {
    let isMounted = true;

    async function checkHealth() {
      const data = await requestHealth();

      if (isMounted) {
        setHealth(data);
        setIsLoading(false);
      }
    }

    void checkHealth();

    return () => {
      isMounted = false;
    };
  }, [requestHealth]);

  const isOnline = health?.online ?? false;
  const checkedAt = health
    ? new Intl.DateTimeFormat(locale === "zh" ? "zh-CN" : "en", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      }).format(new Date(health.checkedAt))
    : locale === "zh"
      ? "未检查"
      : "Not checked";

  return (
    <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-slate-950">
            {locale === "zh" ? "后端状态" : "Backend Status"}
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {locale === "zh"
              ? "Spring Boot 健康检查接口"
              : "Spring Boot health endpoint"}
          </p>
        </div>
        <button
          aria-label={
            locale === "zh" ? "刷新后端状态" : "Refresh backend status"
          }
          className="inline-flex size-9 items-center justify-center rounded-md border border-slate-200 text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-950 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isLoading}
          onClick={loadHealth}
          type="button"
        >
          <RefreshCw
            aria-hidden="true"
            className={`size-4 ${isLoading ? "animate-spin" : ""}`}
          />
        </button>
      </div>

      <div className="mt-5 flex items-center gap-3">
        <span
          className={`flex size-10 items-center justify-center rounded-md ${
            isOnline
              ? "bg-emerald-50 text-emerald-700"
              : "bg-rose-50 text-rose-700"
          }`}
        >
          <Server aria-hidden="true" className="size-5" />
        </span>
        <div>
          <p className="text-sm font-medium text-slate-500">
            {locale === "zh" ? "状态" : "Status"}
          </p>
          <p className="mt-1 text-lg font-semibold text-slate-950">
            {isLoading && !health
              ? locale === "zh"
                ? "检查中"
                : "Checking"
              : isOnline
                ? locale === "zh"
                  ? "在线"
                  : "Online"
                : locale === "zh"
                  ? "离线"
                  : "Offline"}
          </p>
        </div>
      </div>

      <dl className="mt-5 space-y-3 text-sm">
        <div>
          <dt className="font-medium text-slate-500">
            {locale === "zh" ? "服务" : "Service"}
          </dt>
          <dd className="mt-1 text-slate-950">
            {health?.service ?? "backend"}
          </dd>
        </div>
        <div>
          <dt className="font-medium text-slate-500">
            {locale === "zh" ? "后端地址" : "Backend URL"}
          </dt>
          <dd className="mt-1 break-all text-slate-950">
            {health?.backendBaseUrl ?? (locale === "zh" ? "加载中" : "Loading")}
          </dd>
        </div>
        <div>
          <dt className="font-medium text-slate-500">
            {locale === "zh" ? "最近检查" : "Last checked"}
          </dt>
          <dd className="mt-1 text-slate-950">{checkedAt}</dd>
        </div>
      </dl>

      {health?.error ? (
        <p className="mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm leading-6 text-rose-700">
          {health.error}
        </p>
      ) : null}
    </section>
  );
}
