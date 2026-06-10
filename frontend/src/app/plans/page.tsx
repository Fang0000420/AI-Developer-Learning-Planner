import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  CalendarCheck,
  CalendarDays,
  Clock3,
  ListChecks,
  Plus,
  Target,
} from "lucide-react";
import { fetchBackendPlans } from "@/lib/backend-plans";
import { formatGoalDate, formatMinutes } from "@/lib/goals";
import { dictionaries } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";
import { PlanRowActions } from "./plan-row-actions";

export const dynamic = "force-dynamic";

export default async function PlansPage() {
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
  const { data: plans, error } = await fetchBackendPlans();

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
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-emerald-50 px-3 text-sm font-medium text-emerald-700">
              <ListChecks aria-hidden="true" className="size-4" />
              {locale === "zh" ? "学习计划" : "Learning plans"}
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              {locale === "zh" ? "已生成计划" : "Generated Plans"}
            </h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
              {locale === "zh"
                ? "查看已保存的学习计划，并打开计划检查每日任务、预计时间和交付物。"
                : "Review saved learning plans and open a plan to inspect its daily tasks, estimated time, and deliverables."}
            </p>
          </div>

          <Link
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800"
            href="/goals"
          >
            <Target aria-hidden="true" className="size-4" />
            {locale === "zh" ? "打开目标" : "Open Goals"}
          </Link>
        </div>

        {error ? (
          <section className="rounded-md border border-rose-200 bg-rose-50 p-5">
            <h2 className="text-base font-semibold text-rose-950">
              {locale === "zh" ? "计划不可用" : "Plans unavailable"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error.message || t.common.backendError}
            </p>
          </section>
        ) : plans.length === 0 ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm">
            <span className="mx-auto flex size-12 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
              <ListChecks aria-hidden="true" className="size-6" />
            </span>
            <h2 className="mt-5 text-xl font-semibold text-slate-950">
              {locale === "zh" ? "还没有计划" : "No plans yet"}
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "当能力画像、目标拆解、技能差距分析和项目推荐准备好后，从目标生成学习计划。"
                : "Generate a learning plan from a goal after profile analysis, goal decomposition, skill gap analysis, and project recommendation are ready."}
            </p>
            <Link
              className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800"
              href="/goals"
            >
              <Plus aria-hidden="true" className="size-4" />
              {locale === "zh" ? "选择目标" : "Choose Goal"}
            </Link>
          </section>
        ) : (
          <section className="grid gap-4">
            {plans.map((plan) => (
              <article
                className="group rounded-md border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-emerald-300 hover:bg-emerald-50"
                key={plan.id}
              >
                <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                  <Link
                    className="min-w-0 flex-1 rounded-md outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                    href={`/plans/${plan.id}`}
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span
                            className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold ring-1 ${
                              plan.status === "PAUSED"
                                ? "bg-amber-50 text-amber-700 ring-amber-200"
                                : "bg-emerald-50 text-emerald-700 ring-emerald-200"
                            } group-hover:bg-white`}
                          >
                            {plan.status === "PAUSED"
                              ? locale === "zh"
                                ? "已暂停"
                                : "Paused"
                              : locale === "zh"
                                ? "进行中"
                                : "Active"}
                          </span>
                          <span className="text-sm text-slate-500">
                            {locale === "zh" ? "计划 #" : "Plan #"}
                            {plan.id}
                          </span>
                          <span className="text-sm text-slate-500">
                            {t.common.goalId}
                            {plan.goalId}
                          </span>
                        </div>
                        <h2 className="mt-3 text-lg font-semibold text-slate-950">
                          {plan.planTitle}
                        </h2>
                        <p className="mt-2 text-sm leading-6 text-slate-600">
                          {locale === "zh"
                            ? `${plan.dayCount} 天，${plan.taskCount} 个任务。`
                            : `${plan.dayCount} saved days with ${plan.taskCount} tasks.`}
                        </p>
                      </div>

                      <div className="grid shrink-0 gap-3 sm:grid-cols-4 lg:w-[560px]">
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <CalendarDays
                              aria-hidden="true"
                              className="size-4"
                            />
                            {t.common.cycle}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {plan.durationDays} {t.common.days}
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <ListChecks aria-hidden="true" className="size-4" />
                            {locale === "zh" ? "任务" : "Tasks"}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {plan.taskCount}
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <Clock3 aria-hidden="true" className="size-4" />
                            {locale === "zh" ? "时间" : "Time"}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {formatMinutes(plan.totalEstimatedMinutes, locale)}
                          </p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3 group-hover:bg-white">
                          <div className="flex items-center gap-2 text-sm font-medium text-slate-500">
                            <ArrowRight aria-hidden="true" className="size-4" />
                            {t.common.created}
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-950">
                            {formatGoalDate(plan.createdAt, locale)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </Link>

                  <div className="flex shrink-0 flex-col gap-2 lg:w-40">
                    <Link
                      className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800"
                      href={`/plans/${plan.id}/today`}
                    >
                      <CalendarCheck aria-hidden="true" className="size-4" />
                      {t.nav.today}
                    </Link>
                    <PlanRowActions locale={locale} plan={plan} />
                  </div>
                </div>
              </article>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}
