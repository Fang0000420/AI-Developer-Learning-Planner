import Link from "next/link";
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Clock3,
  FileText,
  ListChecks,
  Target,
} from "lucide-react";
import { fetchBackendPlan } from "@/lib/backend-plans";
import { formatGoalDate, formatMinutes, getPriorityClasses } from "@/lib/goals";

export const dynamic = "force-dynamic";

type PlanDetailPageProps = {
  params: Promise<{
    planId: string;
  }>;
};

export default async function PlanDetailPage({ params }: PlanDetailPageProps) {
  const { planId } = await params;
  const { data: plan, error } = await fetchBackendPlan(planId);

  if (error || !plan) {
    return (
      <main className="flex-1 bg-background text-foreground">
        <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <Link
            className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
            href="/goals"
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Goals
          </Link>

          <section className="mt-6 rounded-md border border-rose-200 bg-rose-50 p-5">
            <h1 className="text-xl font-semibold text-rose-950">
              Plan unavailable
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message ||
                "The requested learning plan could not be loaded."}
            </p>
          </section>
        </section>
      </main>
    );
  }

  const taskCount = plan.days.reduce(
    (total, day) => total + day.tasks.length,
    0,
  );
  const totalMinutes = plan.days.reduce(
    (total, day) => total + day.totalEstimatedMinutes,
    0,
  );
  const summaryItems = [
    {
      icon: CalendarDays,
      label: "Plan cycle",
      value: `${plan.durationDays} days`,
    },
    {
      icon: ListChecks,
      label: "Tasks",
      value: `${taskCount} tasks`,
    },
    {
      icon: Clock3,
      label: "Total time",
      value: formatMinutes(totalMinutes),
    },
  ];

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
        <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <Link
                className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
                href={`/goals/${plan.goalId}`}
              >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Goal #{plan.goalId}
              </Link>
              <div className="mt-5 flex flex-wrap items-center gap-2">
                <span className="inline-flex h-8 items-center gap-2 rounded-md bg-emerald-50 px-3 text-sm font-semibold text-emerald-700 ring-1 ring-emerald-200">
                  <CheckCircle2 aria-hidden="true" className="size-4" />
                  Ready
                </span>
                <span className="text-sm text-slate-500">Plan #{plan.id}</span>
              </div>
              <h1 className="mt-4 max-w-4xl text-3xl font-semibold text-slate-950">
                {plan.planTitle}
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Generated from the latest profile, goal decomposition, skill gap
                analysis, and project recommendation saved for this goal.
              </p>
            </div>
          </div>

          <div className="mt-8 grid gap-4 md:grid-cols-3">
            {summaryItems.map((item) => {
              const Icon = item.icon;

              return (
                <div
                  className="rounded-md border border-slate-200 bg-slate-50 p-4"
                  key={item.label}
                >
                  <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                    <Icon aria-hidden="true" className="size-4" />
                    {item.label}
                  </div>
                  <p className="mt-2 text-lg font-semibold text-slate-950">
                    {item.value}
                  </p>
                </div>
              );
            })}
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
          <section className="flex flex-col gap-4">
            {plan.days.map((day) => (
              <article
                className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
                key={day.dayIndex}
              >
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <div>
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-500">
                      <Target aria-hidden="true" className="size-4" />
                      Day {day.dayIndex}
                    </div>
                    <h2 className="mt-2 text-lg font-semibold text-slate-950">
                      {day.theme}
                    </h2>
                  </div>
                  <div className="flex flex-wrap gap-2 text-sm font-medium text-slate-600">
                    <span className="inline-flex h-8 items-center rounded-md bg-slate-100 px-3">
                      {day.tasks.length} tasks
                    </span>
                    <span className="inline-flex h-8 items-center rounded-md bg-slate-100 px-3">
                      {formatMinutes(day.totalEstimatedMinutes)}
                    </span>
                  </div>
                </div>

                <div className="mt-5 divide-y divide-slate-200">
                  {day.tasks.map((task) => (
                    <div className="py-4 first:pt-0 last:pb-0" key={task.id}>
                      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="text-base font-semibold text-slate-950">
                              {task.title}
                            </h3>
                            <span
                              className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${getPriorityClasses(task.priority)}`}
                            >
                              {task.priority}
                            </span>
                          </div>
                          <p className="mt-2 text-sm leading-6 text-slate-600">
                            {task.description}
                          </p>
                        </div>
                        <span className="inline-flex h-8 shrink-0 items-center rounded-md bg-sky-50 px-3 text-sm font-semibold text-sky-700 ring-1 ring-sky-200">
                          {formatMinutes(task.estimatedMinutes)}
                        </span>
                      </div>

                      <div className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
                        <div className="flex items-start gap-2 text-slate-600">
                          <FileText
                            aria-hidden="true"
                            className="mt-1 size-4 shrink-0 text-slate-400"
                          />
                          <span>{task.deliverable}</span>
                        </div>
                        <div className="flex items-start gap-2 text-slate-600">
                          <ListChecks
                            aria-hidden="true"
                            className="mt-1 size-4 shrink-0 text-slate-400"
                          />
                          <span>{task.type}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </section>

          <aside className="flex flex-col gap-6">
            <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-base font-semibold text-slate-950">
                Backend Record
              </h2>
              <dl className="mt-5 space-y-4">
                <div>
                  <dt className="text-sm font-medium text-slate-500">
                    Created
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-950">
                    {formatGoalDate(plan.createdAt)}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-slate-500">
                    Agent run
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-950">
                    {plan.sourceAgentRunId
                      ? `#${plan.sourceAgentRunId}`
                      : "Not recorded"}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-slate-500">User</dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-950">
                    #{plan.userId}
                  </dd>
                </div>
              </dl>
            </section>

            <section className="rounded-md border border-slate-200 bg-slate-950 p-5 text-white shadow-sm">
              <h2 className="text-base font-semibold">Next MVP Step</h2>
              <p className="mt-3 text-sm leading-6 text-slate-300">
                Day 11 can use these saved daily tasks to power the task list
                and current-day work view.
              </p>
            </section>
          </aside>
        </div>
      </section>
    </main>
  );
}
