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
  fetchBackendGoalProfile,
  fetchBackendProjectRecommendation,
  fetchBackendSkillGapAnalysis,
} from "@/lib/backend-goals";
import {
  formatDailyHours,
  formatGoalDate,
  getGoalStatusClasses,
  getGoalStatusLabel,
} from "@/lib/goals";
import { GoalDecompositionPanel } from "./goal-decomposition-panel";
import { GoalDemoRunner } from "./goal-demo-runner";
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

  if (error || !goal) {
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
              Goal unavailable
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message || "The requested goal could not be loaded."}
            </p>
          </section>
        </section>
      </main>
    );
  }

  const summaryItems = [
    {
      icon: CalendarDays,
      label: "Plan cycle",
      value: `${goal.durationDays} days`,
    },
    {
      icon: Clock3,
      label: "Daily time",
      value: formatDailyHours(goal.dailyAvailableHours),
    },
    {
      icon: Target,
      label: "Status",
      value: getGoalStatusLabel(goal.status),
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
                  Goals
                </Link>
                <div className="mt-5 flex flex-wrap items-center gap-2">
                  <span
                    className={`inline-flex h-8 items-center rounded-md px-3 text-sm font-semibold ring-1 ${getGoalStatusClasses(goal.status)}`}
                  >
                    {getGoalStatusLabel(goal.status)}
                  </span>
                  <span className="text-sm text-slate-500">
                    Goal #{goal.id}
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
                New Goal
              </Link>
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

          <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-3">
              <span className="flex size-9 items-center justify-center rounded-md bg-teal-50 text-teal-700">
                <FileText aria-hidden="true" className="size-4" />
              </span>
              <h2 className="text-lg font-semibold text-slate-950">
                Goal Description
              </h2>
            </div>
            <p className="mt-5 whitespace-pre-line text-sm leading-7 text-slate-600">
              {goal.description || "No description recorded."}
            </p>
          </section>

          <ProfileAnalysisPanel
            goalId={goal.id}
            initialError={profileError?.message ?? null}
            initialProfile={profile}
          />

          <GoalDecompositionPanel
            goalId={goal.id}
            initialDecomposition={decomposition}
            initialError={decompositionError?.message ?? null}
          />

          <SkillGapAnalysisPanel
            goalId={goal.id}
            initialError={skillGapError?.message ?? null}
            initialSkillGapAnalysis={skillGapAnalysis}
          />

          <ProjectRecommendationPanel
            goalId={goal.id}
            initialError={projectRecommendationError?.message ?? null}
            initialRecommendation={projectRecommendation}
          />
        </div>

        <aside className="flex flex-col gap-6">
          <GoalDemoRunner
            goalId={goal.id}
            hasDecomposition={decomposition !== null}
            hasProfile={profile !== null}
            hasProjectRecommendation={projectRecommendation !== null}
            hasSkillGapAnalysis={skillGapAnalysis !== null}
          />

          <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold text-slate-950">
              Backend Record
            </h2>
            <dl className="mt-5 space-y-4">
              <div>
                <dt className="text-sm font-medium text-slate-500">Created</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">
                  {formatGoalDate(goal.createdAt)}
                </dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-slate-500">Updated</dt>
                <dd className="mt-1 text-sm font-semibold text-slate-950">
                  {formatGoalDate(goal.updatedAt)}
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
            <h2 className="text-base font-semibold">Demo Path</h2>
            <p className="mt-3 text-sm leading-6 text-slate-300">
              Run the agent chain, open the generated plan, then submit Day 1
              progress to trigger review and the next day adjustment.
            </p>
          </section>
        </aside>
      </section>
    </main>
  );
}
