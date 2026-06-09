import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Clock3,
  Plus,
  Target,
} from "lucide-react";
import { fetchBackendGoals } from "@/lib/backend-goals";
import {
  formatDailyHours,
  formatGoalDate,
  getGoalStatusClasses,
  getGoalStatusLabel,
} from "@/lib/goals";
import { GoalRowActions } from "./goal-row-actions";

export const dynamic = "force-dynamic";

export default async function GoalsPage() {
  const { data: goals, error } = await fetchBackendGoals();

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
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-sky-50 px-3 text-sm font-medium text-sky-700">
              <Target aria-hidden="true" className="size-4" />
              Goal management
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              Learning Goals
            </h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
              Review saved learning goals from the backend and open a goal to
              continue the MVP planning flow.
            </p>
          </div>

          <Link
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800"
            href="/goals/new"
          >
            <Plus aria-hidden="true" className="size-4" />
            New Goal
          </Link>
        </div>

        {error ? (
          <section className="rounded-md border border-rose-200 bg-rose-50 p-5">
            <h2 className="text-base font-semibold text-rose-950">
              Goals unavailable
            </h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error.message || "The backend goals API did not respond."}
            </p>
          </section>
        ) : goals.length === 0 ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm">
            <span className="mx-auto flex size-12 items-center justify-center rounded-md bg-teal-50 text-teal-700">
              <Target aria-hidden="true" className="size-6" />
            </span>
            <h2 className="mt-5 text-xl font-semibold text-slate-950">
              No goals yet
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
              Create the first learning goal to persist it in the backend and
              use it for upcoming profile analysis and planning.
            </p>
            <Link
              className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800"
              href="/goals/new"
            >
              <Plus aria-hidden="true" className="size-4" />
              Create Goal
            </Link>
          </section>
        ) : (
          <section className="grid gap-4">
            {goals.map((goal) => (
              <article
                className="group rounded-md border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-teal-300 hover:bg-teal-50"
                key={goal.id}
              >
                <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                  <Link
                    className="min-w-0 flex-1 rounded-md outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
                    href={`/goals/${goal.id}`}
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span
                            className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${getGoalStatusClasses(goal.status)}`}
                          >
                            {getGoalStatusLabel(goal.status)}
                          </span>
                          <span className="text-sm text-slate-500">
                            Goal #{goal.id}
                          </span>
                        </div>
                        <h2 className="mt-3 text-lg font-semibold text-slate-950">
                          {goal.title}
                        </h2>
                        <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">
                          {goal.description || "No description recorded."}
                        </p>
                      </div>

                      <div className="grid shrink-0 gap-3 sm:grid-cols-3 lg:w-[420px]">
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <CalendarDays
                              aria-hidden="true"
                              className="size-4"
                            />
                            Cycle
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {goal.durationDays} days
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <Clock3 aria-hidden="true" className="size-4" />
                            Daily
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {formatDailyHours(goal.dailyAvailableHours)}
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <ArrowRight aria-hidden="true" className="size-4" />
                            Updated
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {formatGoalDate(goal.updatedAt)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </Link>

                  <GoalRowActions goal={goal} />
                </div>
              </article>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}
