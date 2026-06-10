import Link from "next/link";
import { redirect } from "next/navigation";
import { ArrowLeft, ListChecks } from "lucide-react";
import { fetchBackendPlans } from "@/lib/backend-plans";
import { getCurrentLocale } from "@/lib/i18n-server";

export const dynamic = "force-dynamic";

export default async function LatestTodayTasksPage() {
  const locale = await getCurrentLocale();
  const { data: plans, error } = await fetchBackendPlans();
  const activePlan =
    plans?.find((plan) => plan.status === "ACTIVE") ?? plans?.[0];

  if (activePlan) {
    redirect(`/plans/${activePlan.id}/today`);
  }

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <Link
          className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
          href="/plans"
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          {locale === "zh" ? "计划" : "Plans"}
        </Link>

        <section className="mt-6 rounded-md border border-slate-200 bg-white p-8 text-center shadow-sm">
          <span className="mx-auto flex size-12 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
            <ListChecks aria-hidden="true" className="size-6" />
          </span>
          <h1 className="mt-5 text-xl font-semibold text-slate-950">
            {locale === "zh" ? "还没有每日任务" : "No daily tasks yet"}
          </h1>
          <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
            {error?.message ||
              (locale === "zh"
                ? "请先生成学习计划，之后这里会打开最新计划。"
                : "Generate a learning plan first, then the latest plan will open here.")}
          </p>
        </section>
      </section>
    </main>
  );
}
