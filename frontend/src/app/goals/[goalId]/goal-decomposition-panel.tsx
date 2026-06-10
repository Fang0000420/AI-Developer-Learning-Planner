"use client";

import { useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  GitBranch,
  LoaderCircle,
  Sparkles,
} from "lucide-react";
import { useRouter } from "next/navigation";
import type { ApiErrorResponse, GoalDecomposition } from "@/lib/goals";
import { getPriorityClasses } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type GoalDecompositionPanelProps = {
  goalId: number;
  initialDecomposition: GoalDecomposition | null;
  initialError?: string | null;
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
    (locale === "zh" ? "目标拆解失败。" : "Goal decomposition failed.")
  );
}

export function GoalDecompositionPanel({
  goalId,
  initialDecomposition,
  initialError = null,
  locale,
}: GoalDecompositionPanelProps) {
  const router = useRouter();
  const [decomposition, setDecomposition] = useState<GoalDecomposition | null>(
    initialDecomposition,
  );
  const [error, setError] = useState<string | null>(initialError);
  const [isGenerating, setIsGenerating] = useState(false);

  async function handleDecompose() {
    setError(null);
    setIsGenerating(true);

    try {
      const response = await fetch(
        `/api/goals/${goalId}/decomposition/decompose`,
        {
          method: "POST",
        },
      );
      const payload = (await response.json()) as
        | GoalDecomposition
        | ApiErrorResponse;

      if (!response.ok) {
        setError(getErrorMessage(payload as ApiErrorResponse, locale));
        return;
      }

      setDecomposition(payload as GoalDecomposition);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "目标拆解失败。"
            : "Goal decomposition failed.",
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
      : decomposition
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
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-cyan-50 text-cyan-700">
            <GitBranch aria-hidden="true" className="size-4" />
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold text-slate-950">
                {locale === "zh" ? "目标拆解" : "Goal Decomposition"}
              </h2>
              <span className="inline-flex h-7 items-center gap-1 rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                {isGenerating ? (
                  <LoaderCircle
                    aria-hidden="true"
                    className="size-3.5 animate-spin"
                  />
                ) : decomposition && !error ? (
                  <CheckCircle2 aria-hidden="true" className="size-3.5" />
                ) : error ? (
                  <AlertCircle aria-hidden="true" className="size-3.5" />
                ) : null}
                {statusLabel}
              </span>
            </div>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "把已保存主目标拆成带执行优先级的具体子目标。"
                : "Break the saved main goal into concrete sub-goals with execution priority."}
            </p>
          </div>
        </div>

        <button
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          disabled={isGenerating}
          onClick={handleDecompose}
          type="button"
        >
          {isGenerating ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Sparkles aria-hidden="true" className="size-4" />
          )}
          {decomposition
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
            {locale === "zh" ? "无法拆解目标。" : "Unable to decompose goal."}
          </p>
          <p className="mt-2 text-sm leading-6 text-rose-700">{error}</p>
        </div>
      ) : null}

      {decomposition ? (
        <ol className="mt-6 space-y-3">
          {decomposition.subGoals.map((subGoal, index) => (
            <li
              className="rounded-md border border-slate-200 bg-slate-50 p-4"
              key={`${subGoal.title}-${index}`}
            >
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex gap-3">
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-white text-sm font-semibold text-slate-700 ring-1 ring-slate-200">
                    {index + 1}
                  </span>
                  <div>
                    <h3 className="text-sm font-semibold leading-6 text-slate-950">
                      {subGoal.title}
                    </h3>
                    <p className="mt-1 text-sm leading-6 text-slate-600">
                      {subGoal.description}
                    </p>
                  </div>
                </div>
                <span
                  className={`inline-flex h-7 shrink-0 items-center self-start rounded-md px-2 text-xs font-semibold capitalize ring-1 ${getPriorityClasses(subGoal.priority)}`}
                >
                  {subGoal.priority}
                </span>
              </div>
            </li>
          ))}
        </ol>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          {locale === "zh"
            ? "生成目标拆解后，会把最新 Goal Decomposer 结果保存到 Agent 运行历史。"
            : "Generate a decomposition to save the latest Goal Decomposer result in the agent run history."}
        </p>
      )}
    </section>
  );
}
