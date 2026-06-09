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
import type { ApiErrorResponse, ProjectRecommendation } from "@/lib/goals";
import { formatDailyHours } from "@/lib/goals";

type ProjectRecommendationPanelProps = {
  goalId: number;
  initialError?: string | null;
  initialRecommendation: ProjectRecommendation | null;
};

function getErrorMessage(error: ApiErrorResponse) {
  if (error.errors) {
    const firstError = Object.values(error.errors)[0];
    if (firstError) {
      return firstError;
    }
  }

  return error.message || "Project recommendation failed.";
}

export function ProjectRecommendationPanel({
  goalId,
  initialError = null,
  initialRecommendation,
}: ProjectRecommendationPanelProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(initialError);
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
        setError(getErrorMessage(payload as ApiErrorResponse));
        return;
      }

      setRecommendation(payload as ProjectRecommendation);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Project recommendation failed.",
      );
    } finally {
      setIsGenerating(false);
    }
  }

  const statusLabel = isGenerating
    ? "Generating"
    : error
      ? "Failed"
      : recommendation
        ? "Ready"
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
                Project Recommendation
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
              Turn the profile, sub-goals, and skill gaps into one concrete MVP
              project direction.
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
          {recommendation ? "Regenerate" : "Generate"}
        </button>
      </div>

      {error ? (
        <div className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-4">
          <p className="text-sm font-semibold text-rose-950">
            Unable to recommend a project.
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
                <p className="text-sm font-medium text-slate-500">Duration</p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {recommendation.durationDays} days
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-white p-4">
                <p className="text-sm font-medium text-slate-500">Daily time</p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {formatDailyHours(recommendation.dailyTimeHours)}
                </p>
              </div>
            </div>
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <div className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <Layers3 aria-hidden="true" className="size-4 text-slate-500" />
                Core Tech Stack
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
                Final Deliverables
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
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-200 px-4 text-sm font-semibold text-slate-500"
            disabled
            type="button"
          >
            <Rocket aria-hidden="true" className="size-4" />
            Generate Learning Plan
          </button>
        </div>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          Generate a project recommendation after skill gaps are available to
          get a focused build target for the next planning step.
        </p>
      )}
    </section>
  );
}
