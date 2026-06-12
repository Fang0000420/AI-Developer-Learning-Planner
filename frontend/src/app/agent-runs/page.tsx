import Link from "next/link";
import {
  Activity,
  ArrowLeft,
  ArrowRight,
  Braces,
  Clock3,
  Filter,
  Search,
  Target,
} from "lucide-react";
import { fetchBackendAgentRuns } from "@/lib/backend-agent-runs";
import { formatGoalDate } from "@/lib/goals";
import { getCurrentLocale } from "@/lib/i18n-server";
import type { Locale } from "@/lib/i18n";

export const dynamic = "force-dynamic";

type AgentRunsPageProps = {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function AgentRunsPage({
  searchParams,
}: AgentRunsPageProps) {
  const rawSearchParams = await searchParams;
  const filters = {
    agentName: firstValue(rawSearchParams.agentName),
    goalId: firstValue(rawSearchParams.goalId),
    planId: firstValue(rawSearchParams.planId),
  };
  const locale = await getCurrentLocale();
  const t = agentRunsLabels[locale];
  const { data: runs, error } = await fetchBackendAgentRuns(filters);

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <Link
              className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
              href="/"
            >
              <ArrowLeft aria-hidden="true" className="size-4" />
              {t.dashboard}
            </Link>
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-indigo-50 px-3 text-sm font-medium text-indigo-700">
              <Activity aria-hidden="true" className="size-4" />
              {t.badge}
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              {t.title}
            </h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
              {t.description}
            </p>
          </div>
        </div>

        <form className="mb-5 grid gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_1fr_1fr_auto]">
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            {t.agentName}
            <span className="relative">
              <Search
                aria-hidden="true"
                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400"
              />
              <input
                className="h-10 w-full rounded-md border border-slate-300 pl-9 pr-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
                defaultValue={filters.agentName ?? ""}
                name="agentName"
                placeholder="Plan Generator"
              />
            </span>
          </label>
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            {t.goalId}
            <input
              className="h-10 rounded-md border border-slate-300 px-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              defaultValue={filters.goalId ?? ""}
              inputMode="numeric"
              name="goalId"
              placeholder="1"
            />
          </label>
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            {t.planId}
            <input
              className="h-10 rounded-md border border-slate-300 px-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              defaultValue={filters.planId ?? ""}
              inputMode="numeric"
              name="planId"
              placeholder="1"
            />
          </label>
          <button className="inline-flex h-10 items-center justify-center gap-2 self-end rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800">
            <Filter aria-hidden="true" className="size-4" />
            {t.apply}
          </button>
        </form>

        {error ? (
          <section className="rounded-md border border-rose-200 bg-rose-50 p-5">
            <h2 className="text-base font-semibold text-rose-950">
              {t.unavailableTitle}
            </h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error.message || t.unavailableDescription}
            </p>
          </section>
        ) : runs.length === 0 ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm">
            <span className="mx-auto flex size-12 items-center justify-center rounded-md bg-indigo-50 text-indigo-700">
              <Braces aria-hidden="true" className="size-6" />
            </span>
            <h2 className="mt-5 text-xl font-semibold text-slate-950">
              {t.emptyTitle}
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
              {t.emptyDescription}
            </p>
          </section>
        ) : (
          <section className="grid gap-4">
            {runs.map((run) => (
              <Link
                className="group rounded-md border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-indigo-300 hover:bg-indigo-50"
                href={`/agent-runs/${run.id}`}
                key={run.id}
              >
                <article className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${getStatusClasses(run.status)}`}
                      >
                        {run.status === "SUCCESS" ? t.success : t.failed}
                      </span>
                      {run.responseSource ? (
                        <span
                          className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${getResponseSourceClasses(run.responseSource)}`}
                        >
                          {getResponseSourceLabel(run.responseSource, t)}
                        </span>
                      ) : null}
                      <span className="text-sm text-slate-500">
                        {t.run} #{run.id}
                      </span>
                      {run.goalId ? (
                        <span className="text-sm text-slate-500">
                          {t.goal} #{run.goalId}
                        </span>
                      ) : null}
                      {run.planId ? (
                        <span className="text-sm text-slate-500">
                          {t.plan} #{run.planId}
                        </span>
                      ) : null}
                    </div>
                    <h2 className="mt-3 text-lg font-semibold text-slate-950">
                      {run.agentName}
                    </h2>
                    <p className="mt-2 truncate text-sm leading-6 text-slate-600">
                      requestId: {run.requestId ?? t.notRecorded}
                    </p>
                    {run.errorMessage ? (
                      <p className="mt-2 line-clamp-2 text-sm leading-6 text-rose-700">
                        {run.errorMessage}
                      </p>
                    ) : null}
                  </div>

                  <div className="grid shrink-0 gap-3 sm:grid-cols-3 lg:w-[460px]">
                    <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                      <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                        <Clock3 aria-hidden="true" className="size-4" />
                        {t.latency}
                      </div>
                      <p className="mt-2 text-sm font-semibold text-slate-950">
                        {run.latencyMs} ms
                      </p>
                    </div>
                    <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                      <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                        <Target aria-hidden="true" className="size-4" />
                        {t.scope}
                      </div>
                      <p className="mt-2 text-sm font-semibold text-slate-950">
                        {run.planId
                          ? `${t.plan} ${run.planId}`
                          : `${t.goal} ${run.goalId ?? "-"}`}
                      </p>
                    </div>
                    <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                      <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                        <ArrowRight aria-hidden="true" className="size-4" />
                        {t.created}
                      </div>
                      <p className="mt-2 text-sm font-semibold text-slate-950">
                        {formatGoalDate(run.createdAt, locale)}
                      </p>
                    </div>
                  </div>
                </article>
              </Link>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}

function firstValue(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
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

const agentRunsLabels: Record<Locale, Record<string, string>> = {
  zh: {
    dashboard: "工作台",
    badge: "Agent 可观测性",
    title: "Agent 运行记录",
    description:
      "查看每次 Agent 调用，包括请求链路、耗时、输入、输出和失败详情。",
    agentName: "Agent 名称",
    goalId: "目标 ID",
    planId: "计划 ID",
    apply: "应用筛选",
    unavailableTitle: "Agent 运行记录不可用",
    unavailableDescription: "后端 Agent 运行记录接口没有响应。",
    emptyTitle: "暂无 Agent 运行记录",
    emptyDescription:
      "运行能力画像、计划生成或进度提交后，会创建可追踪的 Agent 执行记录。",
    success: "成功",
    failed: "失败",
    modelResponse: "大模型返回",
    fallbackResponse: "降级返回",
    run: "运行",
    goal: "目标",
    plan: "计划",
    notRecorded: "未记录",
    latency: "耗时",
    scope: "范围",
    created: "创建时间",
  },
  en: {
    dashboard: "Dashboard",
    badge: "Agent observability",
    title: "Agent Runs",
    description:
      "Inspect every Agent call, including request correlation, latency, input, output, and failure details.",
    agentName: "Agent name",
    goalId: "Goal ID",
    planId: "Plan ID",
    apply: "Apply",
    unavailableTitle: "Agent runs unavailable",
    unavailableDescription: "The backend agent runs API did not respond.",
    emptyTitle: "No agent runs found",
    emptyDescription:
      "Run profile analysis, plan generation, or progress submission to create traceable Agent execution records.",
    success: "Success",
    failed: "Failed",
    modelResponse: "Model response",
    fallbackResponse: "Fallback response",
    run: "Run",
    goal: "Goal",
    plan: "Plan",
    notRecorded: "not recorded",
    latency: "Latency",
    scope: "Scope",
    created: "Created",
  },
};
