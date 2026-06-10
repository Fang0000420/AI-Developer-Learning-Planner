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
import { dictionaries, responseLanguageLabel } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";
import { GoalRowActions } from "./goal-row-actions";

export const dynamic = "force-dynamic";

export default async function GoalsPage() {
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
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
              {t.common.dashboard}
            </Link>
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-sky-50 px-3 text-sm font-medium text-sky-700">
              <Target aria-hidden="true" className="size-4" />
              {t.goals.badge}
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              {t.goals.title}
            </h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
              {t.goals.description}
            </p>
          </div>

          <Link
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800"
            href="/goals/new"
          >
            <Plus aria-hidden="true" className="size-4" />
            {t.common.newGoal}
          </Link>
        </div>

        {error ? (
          <section className="rounded-md border border-rose-200 bg-rose-50 p-5">
            <h2 className="text-base font-semibold text-rose-950">
              {t.goals.unavailableTitle}
            </h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error.message || t.common.backendError}
            </p>
          </section>
        ) : goals.length === 0 ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm">
            <span className="mx-auto flex size-12 items-center justify-center rounded-md bg-teal-50 text-teal-700">
              <Target aria-hidden="true" className="size-6" />
            </span>
            <h2 className="mt-5 text-xl font-semibold text-slate-950">
              {t.goals.emptyTitle}
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
              {t.goals.emptyDescription}
            </p>
            <Link
              className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800"
              href="/goals/new"
            >
              <Plus aria-hidden="true" className="size-4" />
              {t.common.createGoal}
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
                            {getGoalStatusLabel(goal.status, locale)}
                          </span>
                          <span className="text-sm text-slate-500">
                            {t.common.goalId}
                            {goal.id}
                          </span>
                          <span className="inline-flex h-7 items-center rounded-md bg-indigo-50 px-2 text-xs font-semibold text-indigo-700 ring-1 ring-indigo-200">
                            {responseLanguageLabel(
                              goal.responseLanguage ?? "zh",
                              locale,
                            )}
                          </span>
                        </div>
                        <h2 className="mt-3 text-lg font-semibold text-slate-950">
                          {goal.title}
                        </h2>
                        <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">
                          {goal.description || t.common.noDescription}
                        </p>
                      </div>

                      <div className="grid shrink-0 gap-3 sm:grid-cols-3 lg:w-[420px]">
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <CalendarDays
                              aria-hidden="true"
                              className="size-4"
                            />
                            {t.common.cycle}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {goal.durationDays} {t.common.days}
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <Clock3 aria-hidden="true" className="size-4" />
                            {t.common.daily}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {formatDailyHours(goal.dailyAvailableHours, locale)}
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <ArrowRight aria-hidden="true" className="size-4" />
                            {t.common.updated}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {formatGoalDate(goal.updatedAt, locale)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </Link>

                  <GoalRowActions goal={goal} locale={locale} />
                </div>
              </article>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}
