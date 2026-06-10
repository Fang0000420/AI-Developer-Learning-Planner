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
  const { data: run, error } = await fetchBackendAgentRun(runId);

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <Link
          className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
          href="/agent-runs"
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Agent Runs
        </Link>

        {error || !run ? (
          <section className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-5">
            <h1 className="text-base font-semibold text-rose-950">
              Agent run unavailable
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message || "The backend agent run API did not respond."}
            </p>
          </section>
        ) : (
          <>
            <div className="mt-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
              <div>
                <p className="inline-flex h-8 items-center gap-2 rounded-md bg-indigo-50 px-3 text-sm font-medium text-indigo-700">
                  <Activity aria-hidden="true" className="size-4" />
                  Agent run detail
                </p>
                <h1 className="mt-4 text-3xl font-semibold text-slate-950">
                  {run.agentName}
                </h1>
                <p className="mt-3 max-w-3xl text-base leading-7 text-slate-600">
                  Run #{run.id} captured at {formatGoalDate(run.createdAt)}.
                </p>
              </div>
              <span
                className={`inline-flex h-8 w-fit items-center rounded-md px-3 text-sm font-semibold ring-1 ${getStatusClasses(run.status)}`}
              >
                {run.status === "SUCCESS" ? "Success" : "Failed"}
              </span>
            </div>

            <section className="mt-6 grid gap-4 lg:grid-cols-4">
              <MetricCard
                icon={Clock3}
                label="Latency"
                value={`${run.latencyMs} ms`}
              />
              <MetricCard
                icon={Target}
                label="Goal"
                value={run.goalId ? `#${run.goalId}` : "Not linked"}
              />
              <MetricCard
                icon={Target}
                label="Plan"
                value={run.planId ? `#${run.planId}` : "Not linked"}
              />
              <MetricCard
                icon={Braces}
                label="Request ID"
                value={run.requestId ?? "Not recorded"}
              />
            </section>

            {run.errorMessage ? (
              <section className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-5">
                <div className="flex items-center gap-2 text-sm font-semibold text-rose-950">
                  <FileWarning aria-hidden="true" className="size-4" />
                  Error
                </div>
                <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-rose-800">
                  {run.errorMessage}
                </p>
              </section>
            ) : null}

            <section className="mt-5 grid gap-5 lg:grid-cols-2">
              <JsonPanel title="Input JSON" value={run.inputJson} />
              <JsonPanel title="Output JSON" value={run.outputJson} />
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
