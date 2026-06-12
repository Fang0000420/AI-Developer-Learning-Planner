import Link from "next/link";
import {
  Activity,
  ArrowLeft,
  Braces,
  Clock3,
  FileWarning,
  Target,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { fetchBackendAgentRun } from "@/lib/backend-agent-runs";
import { formatGoalDate } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";

export const dynamic = "force-dynamic";

type AgentRunDetailPageProps = {
  params: Promise<{
    runId: string;
  }>;
};

export default async function AgentRunDetailPage({
  params,
}: AgentRunDetailPageProps) {
  const { runId } = await params;
  const locale = await getCurrentLocale();
  const t = agentRunDetailLabels[locale];
  const { data: run, error } = await fetchBackendAgentRun(runId);

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <Link
          className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
          href="/agent-runs"
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          {t.back}
        </Link>

        {error || !run ? (
          <section className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-5">
            <h1 className="text-base font-semibold text-rose-950">
              {t.unavailableTitle}
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message || t.unavailableDescription}
            </p>
          </section>
        ) : (
          <>
            <div className="mt-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
              <div>
                <p className="inline-flex h-8 items-center gap-2 rounded-md bg-indigo-50 px-3 text-sm font-medium text-indigo-700">
                  <Activity aria-hidden="true" className="size-4" />
                  {t.badge}
                </p>
                <h1 className="mt-4 text-3xl font-semibold text-slate-950">
                  {run.agentName}
                </h1>
                <p className="mt-3 max-w-3xl text-base leading-7 text-slate-600">
                  {t.run} #{run.id} {t.capturedAt}{" "}
                  {formatGoalDate(run.createdAt, locale)}.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className={`inline-flex h-8 w-fit items-center rounded-md px-3 text-sm font-semibold ring-1 ${getStatusClasses(run.status)}`}
                >
                  {run.status === "SUCCESS" ? t.success : t.failed}
                </span>
                {run.responseSource ? (
                  <span
                    className={`inline-flex h-8 w-fit items-center rounded-md px-3 text-sm font-semibold ring-1 ${getResponseSourceClasses(run.responseSource)}`}
                  >
                    {getResponseSourceLabel(run.responseSource, t)}
                  </span>
                ) : null}
              </div>
            </div>

            <section className="mt-6 grid gap-4 lg:grid-cols-4">
              <MetricCard
                icon={Clock3}
                label={t.latency}
                value={`${run.latencyMs} ms`}
              />
              <MetricCard
                icon={Target}
                label={t.goal}
                value={run.goalId ? `#${run.goalId}` : t.notLinked}
              />
              <MetricCard
                icon={Target}
                label={t.plan}
                value={run.planId ? `#${run.planId}` : t.notLinked}
              />
              <MetricCard
                icon={Braces}
                label={t.requestId}
                value={run.requestId ?? t.notRecorded}
              />
            </section>

            {run.errorMessage ? (
              <section className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-5">
                <div className="flex items-center gap-2 text-sm font-semibold text-rose-950">
                  <FileWarning aria-hidden="true" className="size-4" />
                  {t.error}
                </div>
                <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-rose-800">
                  {run.errorMessage}
                </p>
              </section>
            ) : null}

            <section className="mt-5 grid gap-5 lg:grid-cols-2">
              <JsonPanel title={t.inputJson} value={run.inputJson} />
              <JsonPanel title={t.outputJson} value={run.outputJson} />
            </section>
          </>
        )}
      </section>
    </main>
  );
}

function MetricCard({
  icon: Icon,
  label,
  value,
}: {
  icon: LucideIcon;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
        <Icon aria-hidden="true" className="size-4" />
        {label}
      </div>
      <p className="mt-2 break-all text-sm font-semibold text-slate-950">
        {value}
      </p>
    </div>
  );
}

function JsonPanel({ title, value }: { title: string; value: unknown }) {
  return (
    <section className="min-w-0 rounded-md border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 px-4 py-3">
        <h2 className="text-sm font-semibold text-slate-950">{title}</h2>
      </div>
      <pre className="max-h-[560px] overflow-auto p-4 text-xs leading-6 text-slate-700">
        {JSON.stringify(value ?? null, null, 2)}
      </pre>
    </section>
  );
}

function getStatusClasses(status: "SUCCESS" | "FAILED") {
  return status === "SUCCESS"
    ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
    : "bg-rose-50 text-rose-700 ring-rose-200";
}

function getResponseSourceClasses(source: "MODEL" | "FALLBACK") {
  return source === "MODEL"
    ? "bg-sky-50 text-sky-700 ring-sky-200"
    : "bg-amber-50 text-amber-700 ring-amber-200";
}

function getResponseSourceLabel(
  source: "MODEL" | "FALLBACK",
  t: Record<string, string>,
) {
  return source === "MODEL" ? t.modelResponse : t.fallbackResponse;
}

const agentRunDetailLabels: Record<Locale, Record<string, string>> = {
  zh: {
    back: "Agent 运行记录",
    unavailableTitle: "Agent 运行记录不可用",
    unavailableDescription: "后端 Agent 运行详情接口没有响应。",
    badge: "Agent 运行详情",
    run: "运行",
    capturedAt: "记录于",
    success: "成功",
    failed: "失败",
    modelResponse: "大模型返回",
    fallbackResponse: "降级返回",
    latency: "耗时",
    goal: "目标",
    plan: "计划",
    requestId: "请求 ID",
    notLinked: "未关联",
    notRecorded: "未记录",
    error: "错误",
    inputJson: "输入 JSON",
    outputJson: "输出 JSON",
  },
  en: {
    back: "Agent Runs",
    unavailableTitle: "Agent run unavailable",
    unavailableDescription: "The backend agent run API did not respond.",
    badge: "Agent run detail",
    run: "Run",
    capturedAt: "captured at",
    success: "Success",
    failed: "Failed",
    modelResponse: "Model response",
    fallbackResponse: "Fallback response",
    latency: "Latency",
    goal: "Goal",
    plan: "Plan",
    requestId: "Request ID",
    notLinked: "Not linked",
    notRecorded: "Not recorded",
    error: "Error",
    inputJson: "Input JSON",
    outputJson: "Output JSON",
  },
};
