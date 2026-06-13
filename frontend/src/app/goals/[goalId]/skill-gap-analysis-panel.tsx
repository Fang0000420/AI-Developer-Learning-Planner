"use client";

import { useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Gauge,
  LoaderCircle,
  Sparkles,
} from "lucide-react";
import { useRouter } from "next/navigation";
import type { ApiErrorResponse, SkillGapAnalysis } from "@/lib/goals";
import { getPriorityClasses } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type SkillGapAnalysisPanelProps = {
  goalId: number;
  initialError?: string | null;
  initialSkillGapAnalysis: SkillGapAnalysis | null;
  locale: Locale;
};

function getErrorMessage(error: ApiErrorResponse, locale: Locale) {
  if (error.errors) {
    const firstError = Object.values(error.errors)[0];
    if (firstError) {
      return firstError;
    }
  }

  return (
    error.message ||
    (locale === "zh" ? "技能差距分析失败。" : "Skill gap analysis failed.")
  );
}

export function SkillGapAnalysisPanel({
  goalId,
  initialError = null,
  initialSkillGapAnalysis,
  locale,
}: SkillGapAnalysisPanelProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(initialError);
  const [isGenerating, setIsGenerating] = useState(false);
  const [skillGapAnalysis, setSkillGapAnalysis] =
    useState<SkillGapAnalysis | null>(initialSkillGapAnalysis);

  async function handleAnalyze() {
    setError(null);
    setIsGenerating(true);

    try {
      const response = await fetch(`/api/goals/${goalId}/skill-gap/analyze`, {
        method: "POST",
      });
      const payload = (await response.json()) as
        | SkillGapAnalysis
        | ApiErrorResponse;

      if (!response.ok) {
        setError(getErrorMessage(payload as ApiErrorResponse, locale));
        return;
      }

      setSkillGapAnalysis(payload as SkillGapAnalysis);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "技能差距分析失败。"
            : "Skill gap analysis failed.",
      );
    } finally {
      setIsGenerating(false);
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
      : skillGapAnalysis
        ? locale === "zh"
          ? "就绪"
          : "Ready"
        : locale === "zh"
          ? "未生成"
          : "Not generated";

  return (
    <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-950">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-sky-50 text-sky-700 dark:bg-sky-500/10 dark:text-sky-300">
            <Gauge aria-hidden="true" className="size-4" />
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold text-slate-950 dark:text-slate-50">
                {locale === "zh" ? "技能差距分析" : "Skill Gap Analysis"}
              </h2>
              <span className="inline-flex h-7 items-center gap-1 rounded-md border border-slate-200 bg-slate-50 px-2 text-xs font-semibold text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                {isGenerating ? (
                  <LoaderCircle
                    aria-hidden="true"
                    className="size-3.5 animate-spin"
                  />
                ) : skillGapAnalysis && !error ? (
                  <CheckCircle2 aria-hidden="true" className="size-3.5" />
                ) : error ? (
                  <AlertCircle aria-hidden="true" className="size-3.5" />
                ) : null}
                {statusLabel}
              </span>
            </div>
            <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">
              {locale === "zh"
                ? "比较已保存画像和拆解目标，识别最需要关注的技能。"
                : "Compare the saved profile and decomposed goal to identify the skills that need the most attention."}
            </p>
          </div>
        </div>

        <button
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400 dark:bg-slate-100 dark:text-slate-950 dark:hover:bg-slate-200 dark:disabled:bg-slate-700 dark:disabled:text-slate-300"
          disabled={isGenerating}
          onClick={handleAnalyze}
          type="button"
        >
          {isGenerating ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Sparkles aria-hidden="true" className="size-4" />
          )}
          {skillGapAnalysis
            ? locale === "zh"
              ? "重新生成"
              : "Regenerate"
            : locale === "zh"
              ? "生成"
              : "Generate"}
        </button>
      </div>

      {error ? (
        <div className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-4 dark:border-rose-900/60 dark:bg-rose-950/30">
          <p className="text-sm font-semibold text-rose-950 dark:text-rose-200">
            {locale === "zh"
              ? "无法分析技能差距。"
              : "Unable to analyze skill gaps."}
          </p>
          <p className="mt-2 text-sm leading-6 text-rose-700 dark:text-rose-300">{error}</p>
        </div>
      ) : null}

      {skillGapAnalysis ? (
        <div className="mt-6 overflow-hidden rounded-xl border border-slate-200 bg-slate-50/70 dark:border-slate-800 dark:bg-slate-900/30">
          <div className="overflow-x-auto">
            <table className="min-w-[760px] divide-y divide-slate-200 text-left text-sm dark:divide-slate-800">
              <thead className="bg-slate-100/80 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:bg-slate-900 dark:text-slate-400">
                <tr>
                  <th className="w-[190px] px-4 py-3">
                    {locale === "zh" ? "技能" : "Skill"}
                  </th>
                  <th className="w-[130px] px-4 py-3">
                    {locale === "zh" ? "当前" : "Current"}
                  </th>
                  <th className="w-[150px] px-4 py-3">
                    {locale === "zh" ? "目标" : "Target"}
                  </th>
                  <th className="w-[110px] px-4 py-3">
                    {locale === "zh" ? "优先级" : "Priority"}
                  </th>
                  <th className="px-4 py-3">
                    {locale === "zh" ? "原因" : "Reason"}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-950">
                {skillGapAnalysis.skillGaps.map((skillGap, index) => (
                  <tr
                    className={
                      skillGap.priority === "high"
                        ? "bg-rose-50/60 dark:bg-rose-500/5"
                        : "bg-white dark:bg-slate-950"
                    }
                    key={`${skillGap.skill}-${index}`}
                  >
                    <td
                      className={`px-4 py-4 align-top font-semibold text-slate-950 dark:text-slate-50 ${
                        skillGap.priority === "high"
                          ? "border-l-2 border-rose-300 dark:border-rose-500/40"
                          : "border-l-2 border-transparent"
                      }`}
                    >
                      {skillGap.skill}
                    </td>
                    <td className="px-4 py-4 align-top text-slate-600 dark:text-slate-300">
                      {skillGap.currentLevel}
                    </td>
                    <td className="px-4 py-4 align-top text-slate-600 dark:text-slate-300">
                      {skillGap.targetLevel}
                    </td>
                    <td className="px-4 py-4 align-top">
                      <span
                        className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold capitalize ring-1 ${getPriorityClasses(skillGap.priority)}`}
                      >
                        {skillGap.priority}
                      </span>
                    </td>
                    <td className="px-4 py-4 align-top leading-6 text-slate-600 dark:text-slate-300">
                      {skillGap.reason}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
          {locale === "zh"
            ? "画像和目标拆解可用后生成技能差距分析，以获得更清晰的优先级。"
            : "Generate a skill gap analysis after the profile and decomposition are available to get the clearest priorities."}
        </p>
      )}
    </section>
  );
}
