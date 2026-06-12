"use client";

import { useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Layers3,
  LoaderCircle,
  PackageCheck,
  Rocket,
  Sparkles,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { getApiErrorMessage, pollJob, postJson } from "@/lib/client-jobs";
import type {
  ApiErrorResponse,
  AsyncJob,
  LearningPlan,
  ProjectRecommendation,
} from "@/lib/goals";
import { formatDailyHours } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type ProjectRecommendationPanelProps = {
  goalId: number;
  initialError?: string | null;
  initialRecommendation: ProjectRecommendation | null;
  locale: Locale;
};

export function ProjectRecommendationPanel({
  goalId,
  initialError = null,
  initialRecommendation,
  locale,
}: ProjectRecommendationPanelProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(initialError);
  const [planJobStatus, setPlanJobStatus] = useState<string | null>(null);
  const [planError, setPlanError] = useState<string | null>(null);
  const [isGeneratingPlan, setIsGeneratingPlan] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [recommendation, setRecommendation] =
    useState<ProjectRecommendation | null>(initialRecommendation);

  async function handleRecommend() {
    setError(null);
    setIsGenerating(true);

    try {
      const response = await fetch(
        `/api/goals/${goalId}/project-recommendation/recommend`,
        {
          method: "POST",
        },
      );
      const payload = (await response.json()) as
        | ProjectRecommendation
        | ApiErrorResponse;

      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh"
              ? "学习主线建议生成失败。"
              : "Learning track recommendation failed.",
          ),
        );
        return;
      }

      setRecommendation(payload as ProjectRecommendation);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "学习主线建议生成失败。"
            : "Learning track recommendation failed.",
      );
    } finally {
      setIsGenerating(false);
    }
  }

  async function handleGeneratePlan() {
    setPlanError(null);
    setPlanJobStatus(locale === "zh" ? "启动中" : "Starting");
    setIsGeneratingPlan(true);

    try {
      const job = await postJson<AsyncJob<LearningPlan>>(
        "/api/jobs/plan-generation",
        { goalId },
      );
      setPlanJobStatus(job.status);
      const plan = await pollJob<LearningPlan>(job.jobId, (currentJob) => {
        setPlanJobStatus(currentJob.status);
      });
      router.push(`/plans/${plan.id}`);
    } catch (requestError) {
      setPlanError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "学习计划生成失败。"
            : "Learning plan generation failed.",
      );
    } finally {
      setPlanJobStatus(null);
      setIsGeneratingPlan(false);
    }
  }

  const statusLabel = isGenerating
    ? locale === "zh"
      ? "生成中"
      : "Generating"
    : error
      ? locale === "zh"
        ? "失败"
        : "Failed"
      : recommendation
        ? locale === "zh"
          ? "就绪"
          : "Ready"
        : locale === "zh"
          ? "未生成"
          : "Not generated";

  return (
    <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-sky-50 text-sky-700">
            <Rocket aria-hidden="true" className="size-4" />
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold text-slate-950">
                {locale === "zh" ? "学习主线建议" : "Learning Track Recommendation"}
              </h2>
              <span className="inline-flex h-7 items-center gap-1 rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                {isGenerating ? (
                  <LoaderCircle
                    aria-hidden="true"
                    className="size-3.5 animate-spin"
                  />
                ) : recommendation && !error ? (
                  <CheckCircle2 aria-hidden="true" className="size-3.5" />
                ) : error ? (
                  <AlertCircle aria-hidden="true" className="size-3.5" />
                ) : null}
                {statusLabel}
              </span>
            </div>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "把画像、子目标和技能差距转化为一个聚焦的学习主线或实践方向。"
                : "Turn the profile, sub-goals, and skill gaps into one focused learning track or practice direction."}
            </p>
          </div>
        </div>

        <button
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          disabled={isGenerating}
          onClick={handleRecommend}
          type="button"
        >
          {isGenerating ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Sparkles aria-hidden="true" className="size-4" />
          )}
          {recommendation
            ? locale === "zh"
              ? "重新生成"
              : "Regenerate"
            : locale === "zh"
              ? "生成"
              : "Generate"}
        </button>
      </div>

      {error ? (
        <div className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-4">
          <p className="text-sm font-semibold text-rose-950">
            {locale === "zh"
              ? "无法生成学习主线建议。"
              : "Unable to recommend a learning track."}
          </p>
          <p className="mt-2 text-sm leading-6 text-rose-700">{error}</p>
        </div>
      ) : null}

      {recommendation ? (
        <div className="mt-6 space-y-5">
          <div className="rounded-md border border-slate-200 bg-slate-50 p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h3 className="text-xl font-semibold text-slate-950">
                  {recommendation.recommendedProject}
                </h3>
                <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600">
                  {recommendation.reason}
                </p>
              </div>
              <span className="inline-flex h-8 items-center rounded-md bg-sky-100 px-3 text-sm font-semibold text-sky-700 ring-1 ring-sky-200">
                {recommendation.difficulty}
              </span>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-md border border-slate-200 bg-white p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "周期" : "Duration"}
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {recommendation.durationDays}{" "}
                  {locale === "zh" ? "天" : "days"}
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-white p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "每日时间" : "Daily time"}
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {formatDailyHours(recommendation.dailyTimeHours, locale)}
                </p>
              </div>
            </div>
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <div className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <Layers3 aria-hidden="true" className="size-4 text-slate-500" />
                {locale === "zh" ? "核心聚焦领域" : "Core Focus Areas"}
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {recommendation.coreTechStack.map((tech) => (
                  <span
                    className="inline-flex h-8 items-center rounded-md bg-slate-100 px-3 text-sm font-medium text-slate-700"
                    key={tech}
                  >
                    {tech}
                  </span>
                ))}
              </div>
            </div>

            <div className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <PackageCheck
                  aria-hidden="true"
                  className="size-4 text-slate-500"
                />
                {locale === "zh" ? "成果证明" : "Evidence of Progress"}
              </div>
              <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-600">
                {recommendation.finalDeliverables.map((deliverable) => (
                  <li className="flex gap-2" key={deliverable}>
                    <CheckCircle2
                      aria-hidden="true"
                      className="mt-1 size-4 shrink-0 text-emerald-600"
                    />
                    <span>{deliverable}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isGeneratingPlan}
            onClick={handleGeneratePlan}
            type="button"
          >
            {isGeneratingPlan ? (
              <LoaderCircle
                aria-hidden="true"
                className="size-4 animate-spin"
              />
            ) : (
              <Rocket aria-hidden="true" className="size-4" />
            )}
            {isGeneratingPlan
              ? locale === "zh"
                ? `生成计划中${planJobStatus ? `：${planJobStatus}` : ""}`
                : `Generating Plan${planJobStatus ? `: ${planJobStatus}` : ""}`
              : locale === "zh"
                ? "生成学习计划"
                : "Generate Learning Plan"}
          </button>

          {planError ? (
            <div className="rounded-md border border-rose-200 bg-rose-50 p-4">
              <p className="text-sm font-semibold text-rose-950">
                {locale === "zh"
                  ? "无法生成学习计划。"
                  : "Unable to generate a learning plan."}
              </p>
              <p className="mt-2 text-sm leading-6 text-rose-700">
                {planError}
              </p>
            </div>
          ) : null}
        </div>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          {locale === "zh"
            ? "技能差距可用后生成学习主线建议，为下一步计划获得更聚焦的方向。"
            : "Generate a learning track recommendation after skill gaps are available to get a clearer direction for the next planning step."}
        </p>
      )}
    </section>
  );
}
