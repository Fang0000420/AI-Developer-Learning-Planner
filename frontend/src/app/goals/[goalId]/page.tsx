import Link from "next/link";
import {
  ArrowLeft,
  CalendarDays,
  Clock3,
  FileText,
  Plus,
  Target,
} from "lucide-react";
import {
  fetchBackendGoal,
  fetchBackendGoalDecomposition,
  fetchBackendGoalKnowledgePreference,
  fetchBackendGoalProfile,
  fetchBackendPathRecommendation,
  fetchBackendProjectRecommendation,
  fetchBackendSkillGapAnalysis,
} from "@/lib/backend-goals";
import {
  formatDailyHours,
  formatGoalDate,
  getGoalStatusClasses,
  getGoalStatusLabel,
} from "@/lib/goals";
import { dictionaries, responseLanguageLabel } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";
import { GoalDecompositionPanel } from "./goal-decomposition-panel";
import { GoalDemoRunner } from "./goal-demo-runner";
import { PathRecommendationWorkspace } from "./path-recommendation-workspace";
import { ProfileAnalysisPanel } from "./profile-analysis-panel";
import { ProjectRecommendationPanel } from "./project-recommendation-panel";
import { SkillGapAnalysisPanel } from "./skill-gap-analysis-panel";

export const dynamic = "force-dynamic";

type GoalDetailPageProps = {
  params: Promise<{
    goalId: string;
  }>;
};

export default async function GoalDetailPage({ params }: GoalDetailPageProps) {
  const { goalId } = await params;
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
  const { data: goal, error } = await fetchBackendGoal(goalId);
  const { data: profile, error: profileError } = goal
    ? await fetchBackendGoalProfile(goalId)
    : { data: null, error: null };
  const { data: decomposition, error: decompositionError } = goal
    ? await fetchBackendGoalDecomposition(goalId)
    : { data: null, error: null };
  const { data: skillGapAnalysis, error: skillGapError } = goal
    ? await fetchBackendSkillGapAnalysis(goalId)
    : { data: null, error: null };
  const { data: projectRecommendation, error: projectRecommendationError } =
    goal
      ? await fetchBackendProjectRecommendation(goalId)
      : { data: null, error: null };
  const { data: pathRecommendation, error: pathRecommendationError } = goal
    ? await fetchBackendPathRecommendation(goalId)
    : { data: null, error: null };
  const knowledgePreferenceResult = goal
    ? await fetchBackendGoalKnowledgePreference(goalId)
    : { data: null };
  const knowledgePreference = knowledgePreferenceResult.data;

  if (error || !goal) {
    return (
      <main className="flex-1 bg-background text-foreground">
        <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <Link
            className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
            href="/goals"
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            {t.nav.goals}
          </Link>

          <section className="mt-6 rounded-md border border-rose-200 bg-rose-50 p-5">
            <h1 className="text-xl font-semibold text-rose-950">
              {locale === "zh" ? "目标不可用" : "Goal unavailable"}
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message ||
                (locale === "zh"
                  ? "无法加载请求的目标。"
                  : "The requested goal could not be loaded.")}
            </p>
          </section>
        </section>
      </main>
    );
  }

  const summaryItems = [
    {
      icon: CalendarDays,
      label: locale === "zh" ? "计划周期" : "Plan cycle",
      value: `${goal.durationDays} ${t.common.days}`,
    },
    {
      icon: Clock3,
      label: locale === "zh" ? "每日时间" : "Daily time",
      value: formatDailyHours(goal.dailyAvailableHours, locale),
    },
    {
      icon: Target,
      label: t.common.status,
      value: getGoalStatusLabel(goal.status, locale),
    },
    {
      icon: Target,
      label: t.common.responseLanguage,
      value: responseLanguageLabel(goal.responseLanguage ?? "zh", locale),
    },
  ];

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-6 sm:px-6 lg:grid-cols-[1fr_360px] lg:px-8">
        <div className="flex flex-col gap-6">
          <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
              <div>
                <Link
                  className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
                  href="/goals"
                >
                  <ArrowLeft aria-hidden="true" className="size-4" />
                  {t.nav.goals}
                </Link>
                <div className="mt-5 flex flex-wrap items-center gap-2">
                  <span
                    className={`inline-flex h-8 items-center rounded-md px-3 text-sm font-semibold ring-1 ${getGoalStatusClasses(goal.status)}`}
                  >
                    {getGoalStatusLabel(goal.status, locale)}
                  </span>
                  <span className="text-sm text-slate-500">
                    {t.common.goalId}
                    {goal.id}
                  </span>
                </div>
                <h1 className="mt-4 max-w-3xl text-3xl font-semibold text-slate-950">
                  {goal.title}
                </h1>
              </div>

              <Link
                className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800"
                href="/goals/new"
              >
                <Plus aria-hidden="true" className="size-4" />
                {t.common.newGoal}
              </Link>
            </div>

            <div className="mt-8 grid gap-4 md:grid-cols-4">
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

          <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-3">
              <span className="flex size-9 items-center justify-center rounded-md bg-teal-50 text-teal-700">
                <FileText aria-hidden="true" className="size-4" />
              </span>
              <h2 className="text-lg font-semibold text-slate-950">
                {locale === "zh" ? "目标描述" : "Goal Description"}
              </h2>
            </div>
            <p className="mt-5 whitespace-pre-line text-sm leading-7 text-slate-600">
              {goal.description || t.common.noDescription}
            </p>
          </section>

          <PathRecommendationWorkspace
            goalId={goal.id}
            initialError={pathRecommendationError?.message ?? null}
            initialRecommendation={pathRecommendation}
            locale={locale}
          />

          <ProfileAnalysisPanel
            goalId={goal.id}
            initialError={profileError?.message ?? null}
            initialProfile={profile}
            locale={locale}
          />

          <GoalDecompositionPanel
            goalId={goal.id}
            initialDecomposition={decomposition}
            initialError={decompositionError?.message ?? null}
            locale={locale}
          />

          <SkillGapAnalysisPanel
            goalId={goal.id}
            initialError={skillGapError?.message ?? null}
            initialSkillGapAnalysis={skillGapAnalysis}
            locale={locale}
          />

          <ProjectRecommendationPanel
            goalId={goal.id}
            initialError={projectRecommendationError?.message ?? null}
            initialRecommendation={projectRecommendation}
            locale={locale}
          />
        </div>

        <aside className="flex flex-col gap-6">
          <GoalDemoRunner
            goalId={goal.id}
            hasPathRecommendation={pathRecommendation !== null}
            locale={locale}
          />

          <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold text-slate-950">
              {locale === "zh" ? "知识联动" : "Knowledge Link"}
            </h2>
            <div className="mt-5 space-y-3 text-sm text-slate-600">
              <p>
                {locale === "zh" ? "优先文档数：" : "Preferred docs: "}
                {knowledgePreference?.preferredDocumentIds.length ?? 0}
              </p>
              <p>
                {locale === "zh" ? "固定作用域：" : "Fixed scope: "}
                {knowledgePreference?.preferredScope ?? (locale === "zh" ? "未固定" : "Not fixed")}
              </p>
              <p>
                {locale === "zh" ? "固定分类：" : "Fixed categories: "}
                {knowledgePreference && knowledgePreference.preferredCategories.length > 0
                  ? knowledgePreference.preferredCategories.join(", ")
                  : locale === "zh"
                    ? "未固定"
                    : "Not fixed"}
              </p>
              <Link
                className="inline-flex h-10 items-center justify-center rounded-md bg-slate-950 px-4 text-sm font-semibold text-white"
                href={`/knowledge?goalId=${goal.id}`}
              >
                {locale === "zh" ? "打开知识库并配置当前目标" : "Open Knowledge for This Goal"}
              </Link>
            </div>
          </section>

          <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold text-slate-950">
              {locale === "zh" ? "后端记录" : "Backend Record"}
            </h2>
            <dl className="mt-5 space-y-4">
              <div>
                <dt className="text-sm font-medium text-slate-500">
                  {t.common.created}
                </dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">
                  {formatGoalDate(goal.createdAt, locale)}
                </dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-slate-500">
                  {t.common.updated}
                </dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">
                  {formatGoalDate(goal.updatedAt, locale)}
                </dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-slate-500">User</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">
                  #{goal.userId}
                </dd>
              </div>
            </dl>
          </section>

          <section className="rounded-md border border-slate-200 bg-slate-950 p-5 text-white shadow-sm">
            <h2 className="text-base font-semibold">
              {locale === "zh" ? "演示路径" : "Demo Path"}
            </h2>
            <p className="mt-3 text-sm leading-6 text-slate-300">
              {locale === "zh"
                ? "运行 Agent 链路，打开生成的计划，然后提交第 1 天进度以触发复盘和下一天调整。"
                : "Run the agent chain, open the generated plan, then submit Day 1 progress to trigger review and the next day adjustment."}
            </p>
          </section>
        </aside>
      </section>
    </main>
  );
}
