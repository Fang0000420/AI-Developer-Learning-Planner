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
              Dashboard
            </Link>
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-indigo-50 px-3 text-sm font-medium text-indigo-700">
              <Activity aria-hidden="true" className="size-4" />
              Agent observability
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              Agent Runs
            </h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
              Inspect every Agent call, including request correlation, latency,
              input, output, and failure details.
            </p>
          </div>
        </div>

        <form className="mb-5 grid gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_1fr_1fr_auto]">
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            Agent name
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
            Goal ID
            <input
              className="h-10 rounded-md border border-slate-300 px-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              defaultValue={filters.goalId ?? ""}
              inputMode="numeric"
              name="goalId"
              placeholder="1"
            />
          </label>
          <label className="grid gap-2 text-sm font-medium text-slate-700">
            Plan ID
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
            Apply
          </button>
        </form>

        {error ? (
          <section className="rounded-md border border-rose-200 bg-rose-50 p-5">
            <h2 className="text-base font-semibold text-rose-950">
              Agent runs unavailable
            </h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error.message || "The backend agent runs API did not respond."}
            </p>
          </section>
        ) : runs.length === 0 ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm">
            <span className="mx-auto flex size-12 items-center justify-center rounded-md bg-indigo-50 text-indigo-700">
              <Braces aria-hidden="true" className="size-6" />
            </span>
            <h2 className="mt-5 text-xl font-semibold text-slate-950">
              No agent runs found
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
              Run profile analysis, plan generation, or progress submission to
              create traceable Agent execution records.
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
                        {run.status === "SUCCESS" ? "Success" : "Failed"}
                      </span>
                      <span className="text-sm text-slate-500">
                        Run #{run.id}
                      </span>
                      {run.goalId ? (
                        <span className="text-sm text-slate-500">
                          Goal #{run.goalId}
                        </span>
                      ) : null}
                      {run.planId ? (
                        <span className="text-sm text-slate-500">
                          Plan #{run.planId}
                        </span>
                      ) : null}
                    </div>
                    <h2 className="mt-3 text-lg font-semibold text-slate-950">
                      {run.agentName}
                    </h2>
                    <p className="mt-2 truncate text-sm leading-6 text-slate-600">
                      requestId: {run.requestId ?? "not recorded"}
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
                        Latency
                      </div>
                      <p className="mt-2 text-sm font-semibold text-slate-950">
                        {run.latencyMs} ms
                      </p>
                    </div>
                    <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                      <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                        <Target aria-hidden="true" className="size-4" />
                        Scope
                      </div>
                      <p className="mt-2 text-sm font-semibold text-slate-950">
                        {run.planId
                          ? `Plan ${run.planId}`
                          : `Goal ${run.goalId ?? "-"}`}
                      </p>
                    </div>
                    <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                      <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                        <ArrowRight aria-hidden="true" className="size-4" />
                        Created
                      </div>
                      <p className="mt-2 text-sm font-semibold text-slate-950">
                        {formatGoalDate(run.createdAt)}
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
