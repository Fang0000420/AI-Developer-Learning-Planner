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

type SkillGapAnalysisPanelProps = {
  goalId: number;
  initialError?: string | null;
  initialSkillGapAnalysis: SkillGapAnalysis | null;
};

function getErrorMessage(error: ApiErrorResponse) {
  if (error.errors) {
    const firstError = Object.values(error.errors)[0];
    if (firstError) {
      return firstError;
    }
  }

  return error.message || "Skill gap analysis failed.";
}

export function SkillGapAnalysisPanel({
  goalId,
  initialError = null,
  initialSkillGapAnalysis,
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
        setError(getErrorMessage(payload as ApiErrorResponse));
        return;
      }

      setSkillGapAnalysis(payload as SkillGapAnalysis);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Skill gap analysis failed.",
      );
    } finally {
      setIsGenerating(false);
    }
  }

  const statusLabel = isGenerating
    ? "Generating"
    : error
      ? "Failed"
      : skillGapAnalysis
        ? "Ready"
        : "Not generated";

  return (
    <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
            <Gauge aria-hidden="true" className="size-4" />
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold text-slate-950">
                Skill Gap Analysis
              </h2>
              <span className="inline-flex h-7 items-center gap-1 rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
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
            <p className="mt-2 text-sm leading-6 text-slate-600">
              Compare the saved profile and decomposed goal to identify the
              skills that need the most attention.
            </p>
          </div>
        </div>

        <button
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          disabled={isGenerating}
          onClick={handleAnalyze}
          type="button"
        >
          {isGenerating ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Sparkles aria-hidden="true" className="size-4" />
          )}
          {skillGapAnalysis ? "Regenerate" : "Generate"}
        </button>
      </div>

      {error ? (
        <div className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-4">
          <p className="text-sm font-semibold text-rose-950">
            Unable to analyze skill gaps.
          </p>
          <p className="mt-2 text-sm leading-6 text-rose-700">{error}</p>
        </div>
      ) : null}

      {skillGapAnalysis ? (
        <div className="mt-6 overflow-hidden rounded-md border border-slate-200">
          <div className="overflow-x-auto">
            <table className="min-w-[760px] divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="w-[190px] px-4 py-3">Skill</th>
                  <th className="w-[130px] px-4 py-3">Current</th>
                  <th className="w-[150px] px-4 py-3">Target</th>
                  <th className="w-[110px] px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {skillGapAnalysis.skillGaps.map((skillGap, index) => (
                  <tr
                    className={
                      skillGap.priority === "high" ? "bg-rose-50/40" : ""
                    }
                    key={`${skillGap.skill}-${index}`}
                  >
                    <td className="px-4 py-4 align-top font-semibold text-slate-950">
                      {skillGap.skill}
                    </td>
                    <td className="px-4 py-4 align-top text-slate-600">
                      {skillGap.currentLevel}
                    </td>
                    <td className="px-4 py-4 align-top text-slate-600">
                      {skillGap.targetLevel}
                    </td>
                    <td className="px-4 py-4 align-top">
                      <span
                        className={`inline-flex h-7 items-center rounded-md px-2 text-xs font-semibold capitalize ring-1 ${getPriorityClasses(skillGap.priority)}`}
                      >
                        {skillGap.priority}
                      </span>
                    </td>
                    <td className="px-4 py-4 align-top leading-6 text-slate-600">
                      {skillGap.reason}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          Generate a skill gap analysis after the profile and decomposition are
          available to get the clearest priorities.
        </p>
      )}
    </section>
  );
}
