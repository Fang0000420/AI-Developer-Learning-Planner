import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  CalendarCheck,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  FileText,
  ListChecks,
} from "lucide-react";
import {
  fetchBackendPlan,
  fetchBackendPlanDayTasks,
} from "@/lib/backend-plans";
import { fetchBackendProgressLogs } from "@/lib/backend-progress";
import {
  formatMinutes,
  getDailyTaskStatusClasses,
  getDailyTaskStatusLabel,
  getPriorityClasses,
} from "@/lib/goals";
import { ProgressSubmitForm } from "./progress-submit-form";
import { TaskStatusActions } from "./task-status-actions";

export const dynamic = "force-dynamic";

type TodayTasksPageProps = {
  params: Promise<{
    planId: string;
  }>;
  searchParams: Promise<{
    dayIndex?: string;
  }>;
};

function parseDayIndex(value: string | undefined) {
  const parsed = Number(value ?? "1");
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1;
}

export default async function TodayTasksPage({
  params,
  searchParams,
}: TodayTasksPageProps) {
  const { planId } = await params;
  const { dayIndex: dayIndexParam } = await searchParams;
  const dayIndex = parseDayIndex(dayIndexParam);
  const [
    { data: plan, error: planError },
    { data: day, error: dayError },
    { data: progressLogs, error: progressError },
  ] = await Promise.all([
    fetchBackendPlan(planId),
    fetchBackendPlanDayTasks(planId, dayIndex),
    fetchBackendProgressLogs(planId, dayIndex),
  ]);

  const error = planError || dayError;

  if (error || !plan || !day) {
    return (
      <main className="flex-1 bg-background text-foreground">
        <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <Link
            className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
            href={`/plans/${planId}`}
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Plan
          </Link>

          <section className="mt-6 rounded-md border border-rose-200 bg-rose-50 p-5">
            <h1 className="text-xl font-semibold text-rose-950">
              Tasks unavailable
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message || "The requested daily tasks could not load."}
            </p>
          </section>
        </section>
      </main>
    );
  }

  const doneCount = day.tasks.filter((task) => task.status === "DONE").length;
  const inProgressCount = day.tasks.filter(
    (task) => task.status === "IN_PROGRESS",
  ).length;
  const previousDay = dayIndex > 1 ? dayIndex - 1 : null;
  const nextDay = dayIndex < plan.durationDays ? dayIndex + 1 : null;
  const dayNavigation = [
    {
      href: previousDay
        ? `/plans/${plan.id}/today?dayIndex=${previousDay}`
        : "",
      icon: ChevronLeft,
      label: "Previous",
      enabled: previousDay !== null,
    },
    {
      href: nextDay ? `/plans/${plan.id}/today?dayIndex=${nextDay}` : "",
      icon: ChevronRight,
      label: "Next",
      enabled: nextDay !== null,
    },
  ];
  const latestLog = progressLogs?.[0] ?? null;

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
        <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <Link
                className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
                href={`/plans/${plan.id}`}
              >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Plan #{plan.id}
              </Link>
              <div className="mt-5 flex flex-wrap items-center gap-2">
                <span className="inline-flex h-8 items-center gap-2 rounded-md bg-emerald-50 px-3 text-sm font-semibold text-emerald-700 ring-1 ring-emerald-200">
                  <CalendarCheck aria-hidden="true" className="size-4" />
                  Day {day.dayIndex}
                </span>
                <span className="text-sm text-slate-500">{plan.planTitle}</span>
              </div>
              <h1 className="mt-4 max-w-4xl text-3xl font-semibold text-slate-950">
                {day.theme}
              </h1>
            </div>

            <div className="flex flex-wrap gap-2">
              {dayNavigation.map((item) => {
                const Icon = item.icon;

                return item.enabled ? (
                  <Link
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                    href={item.href}
                    key={item.label}
                  >
                    <Icon aria-hidden="true" className="size-4" />
                    {item.label}
                  </Link>
                ) : (
                  <span
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-slate-50 px-3 text-sm font-semibold text-slate-400"
                    key={item.label}
                  >
                    <Icon aria-hidden="true" className="size-4" />
                    {item.label}
                  </span>
                );
              })}
            </div>
          </div>

          <div className="mt-8 grid gap-4 md:grid-cols-4">
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                <ListChecks aria-hidden="true" className="size-4" />
                Tasks
              </div>
              <p className="mt-2 text-lg font-semibold text-slate-950">
                {day.tasks.length}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                <Clock3 aria-hidden="true" className="size-4" />
                Time
              </div>
              <p className="mt-2 text-lg font-semibold text-slate-950">
                {formatMinutes(day.totalEstimatedMinutes)}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                <ArrowRight aria-hidden="true" className="size-4" />
                In progress
              </div>
              <p className="mt-2 text-lg font-semibold text-slate-950">
                {inProgressCount}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                <CheckCircle2 aria-hidden="true" className="size-4" />
                Done
              </div>
              <p className="mt-2 text-lg font-semibold text-slate-950">
                {doneCount}/{day.tasks.length}
              </p>
            </div>
          </div>
        </section>

        {progressError ? (
          <section className="rounded-md border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-800">
            {progressError.message || "Progress logs could not load."}
          </section>
        ) : null}

        <ProgressSubmitForm
          dayIndex={day.dayIndex}
          latestLog={latestLog}
          planId={plan.id}
          tasks={day.tasks}
        />

        <section className="flex flex-col gap-4">
          {day.tasks.map((task) => (
            <article
              className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
              key={task.id}
            >
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${getDailyTaskStatusClasses(task.status)}`}
                    >
                      {getDailyTaskStatusLabel(task.status)}
                    </span>
                    <span
                      className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${getPriorityClasses(task.priority)}`}
                    >
                      {task.priority}
                    </span>
                    <span className="text-sm text-slate-500">
                      Task {task.taskOrder}
                    </span>
                  </div>
                  <h2 className="mt-3 text-lg font-semibold text-slate-950">
                    {task.title}
                  </h2>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    {task.description}
                  </p>
                </div>

                <div className="shrink-0 lg:w-[420px]">
                  <TaskStatusActions planId={plan.id} task={task} />
                </div>
              </div>

              <div className="mt-5 grid gap-3 text-sm md:grid-cols-3">
                <div className="rounded-md bg-slate-50 p-3">
                  <div className="flex items-center gap-2 font-medium text-slate-500">
                    <Clock3 aria-hidden="true" className="size-4" />
                    Estimate
                  </div>
                  <p className="mt-2 font-semibold text-slate-950">
                    {formatMinutes(task.estimatedMinutes)}
                  </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                  <div className="flex items-center gap-2 font-medium text-slate-500">
                    <ListChecks aria-hidden="true" className="size-4" />
                    Type
                  </div>
                  <p className="mt-2 font-semibold text-slate-950">
                    {task.type}
                  </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                  <div className="flex items-start gap-2 font-medium text-slate-500">
                    <FileText
                      aria-hidden="true"
                      className="mt-0.5 size-4 shrink-0"
                    />
                    Deliverable
                  </div>
                  <p className="mt-2 font-semibold leading-6 text-slate-950">
                    {task.deliverable}
                  </p>
                </div>
              </div>
            </article>
          ))}
        </section>
      </section>
    </main>
  );
}
