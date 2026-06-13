"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Clock3, History, RotateCcw } from "lucide-react";
import {
  formatGoalDate,
  formatMinutes,
  type LearningPlanVersionSummary,
} from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type PlanVersionPanelProps = {
  locale: Locale;
  planId: number;
  versions: LearningPlanVersionSummary[];
};

function triggerLabel(trigger: string, locale: Locale) {
  if (locale === "zh") {
    return (
      {
        generated: "初始生成",
        manual_override: "手动覆盖",
        progress_adjustment: "进度调整",
        restore: "版本回滚",
      }[trigger as keyof Record<string, string>] ?? trigger
    );
  }

  return (
    {
      generated: "Generated",
      manual_override: "Manual override",
      progress_adjustment: "Progress adjustment",
      restore: "Rollback",
    }[trigger as keyof Record<string, string>] ?? trigger
  );
}

function changeTypeLabel(
  changeType: "added" | "removed" | "updated",
  locale: Locale,
) {
  if (locale === "zh") {
    return {
      added: "新增",
      removed: "移除",
      updated: "更新",
    }[changeType];
  }

  return {
    added: "Added",
    removed: "Removed",
    updated: "Updated",
  }[changeType];
}

export function PlanVersionPanel({
  locale,
  planId,
  versions,
}: PlanVersionPanelProps) {
  const router = useRouter();
  const [isRestoring, setIsRestoring] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function restoreVersion(version: number) {
    setError(null);
    setIsRestoring(version);
    try {
      const response = await fetch(
        `/api/plans/${planId}/versions/${version}/restore`,
        {
          method: "POST",
        },
      );
      if (!response.ok) {
        const payload = (await response.json()) as { message?: string };
        setError(
          payload.message ||
            (locale === "zh" ? "回滚计划版本失败。" : "Plan rollback failed."),
        );
        return;
      }
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "回滚计划版本失败。"
            : "Plan rollback failed.",
      );
    } finally {
      setIsRestoring(null);
    }
  }

  return (
    <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center gap-2 text-base font-semibold text-slate-950">
        <History aria-hidden="true" className="size-4" />
        {locale === "zh" ? "计划版本" : "Plan Versions"}
      </div>
      <p className="mt-3 text-sm leading-6 text-slate-600">
        {locale === "zh"
          ? "记录计划生成、自动调整、手动覆盖和回滚带来的版本变化。"
          : "Tracks versions created by generation, automatic adjustments, manual overrides, and rollback."}
      </p>

      <div className="mt-5 space-y-3">
        {versions.map((version) => (
          <article
            className="rounded-md border border-slate-200 bg-slate-50 p-4"
            key={version.version}
          >
            <div className="flex flex-col gap-3">
              <div className="flex flex-wrap items-center gap-2">
                <span className="inline-flex h-7 items-center rounded-md bg-slate-900 px-2 text-xs font-semibold text-white">
                  {locale === "zh"
                    ? `版本 ${version.version}`
                    : `Version ${version.version}`}
                </span>
                <span className="inline-flex h-7 items-center rounded-md bg-white px-2 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">
                  {triggerLabel(version.trigger, locale)}
                </span>
                {version.current ? (
                  <span className="inline-flex h-7 items-center rounded-md bg-emerald-50 px-2 text-xs font-semibold text-emerald-700 ring-1 ring-emerald-200">
                    {locale === "zh" ? "当前版本" : "Current"}
                  </span>
                ) : null}
              </div>

              <p className="text-sm leading-6 text-slate-700">
                {version.reason}
              </p>

              <div className="grid gap-3 text-sm md:grid-cols-3">
                <div className="rounded-md border border-slate-200 bg-white p-3">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "总时间" : "Total time"}
                  </div>
                  <p className="mt-1 text-slate-800">
                    {formatMinutes(version.totalEstimatedMinutes, locale)}
                  </p>
                </div>
                <div className="rounded-md border border-slate-200 bg-white p-3">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "任务变化" : "Task delta"}
                  </div>
                  <p className="mt-1 text-slate-800">{version.taskDelta}</p>
                </div>
                <div className="rounded-md border border-slate-200 bg-white p-3">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "分钟变化" : "Minute delta"}
                  </div>
                  <p className="mt-1 text-slate-800">{version.minuteDelta}</p>
                </div>
              </div>

              <div className="grid gap-3 text-sm md:grid-cols-3">
                <div className="rounded-md border border-slate-200 bg-white p-3">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "新增任务" : "Added tasks"}
                  </div>
                  <p className="mt-1 text-slate-800">
                    {version.diff.addedTaskCount}
                  </p>
                </div>
                <div className="rounded-md border border-slate-200 bg-white p-3">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "移除任务" : "Removed tasks"}
                  </div>
                  <p className="mt-1 text-slate-800">
                    {version.diff.removedTaskCount}
                  </p>
                </div>
                <div className="rounded-md border border-slate-200 bg-white p-3">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "更新任务" : "Updated tasks"}
                  </div>
                  <p className="mt-1 text-slate-800">
                    {version.diff.updatedTaskCount}
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
                <span className="inline-flex items-center gap-2">
                  <Clock3 aria-hidden="true" className="size-4" />
                  {formatGoalDate(version.createdAt, locale)}
                </span>
                {version.affectedDayIndexes.length > 0 ? (
                  <span>
                    {locale === "zh" ? "影响天数：" : "Affected days: "}
                    {version.affectedDayIndexes.join(", ")}
                  </span>
                ) : null}
              </div>

              {version.diff.taskChanges.length > 0 ? (
                <div className="rounded-md border border-slate-200 bg-white p-4 text-sm">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "变更明细" : "Change details"}
                  </div>
                  <ul className="mt-3 space-y-2 text-slate-700">
                    {version.diff.taskChanges.map((change, index) => (
                      <li
                        className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2"
                        key={`${change.dayIndex}-${change.title}-${index}`}
                      >
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="inline-flex h-6 items-center rounded-md bg-white px-2 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">
                            {changeTypeLabel(change.changeType, locale)}
                          </span>
                          <span className="text-xs text-slate-500">
                            {locale === "zh"
                              ? `第 ${change.dayIndex} 天`
                              : `Day ${change.dayIndex}`}
                          </span>
                        </div>
                        <div className="mt-2 font-medium text-slate-800">
                          {change.title}
                        </div>
                        {(change.previousEstimatedMinutes !== null &&
                          change.previousEstimatedMinutes !== undefined) ||
                        (change.currentEstimatedMinutes !== null &&
                          change.currentEstimatedMinutes !== undefined) ? (
                          <div className="mt-1 text-slate-600">
                            {locale === "zh" ? "分钟：" : "Minutes: "}
                            {change.previousEstimatedMinutes ?? "-"}
                            {" → "}
                            {change.currentEstimatedMinutes ?? "-"}
                          </div>
                        ) : null}
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {!version.current ? (
                <div>
                  <button
                    className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
                    disabled={isRestoring !== null}
                    onClick={() => restoreVersion(version.version)}
                    type="button"
                  >
                    <RotateCcw aria-hidden="true" className="size-4" />
                    {isRestoring === version.version
                      ? locale === "zh"
                        ? "回滚中"
                        : "Restoring"
                      : locale === "zh"
                        ? "回滚到此版本"
                        : "Restore this version"}
                  </button>
                </div>
              ) : null}
            </div>
          </article>
        ))}
      </div>

      {error ? <p className="mt-4 text-sm text-rose-700">{error}</p> : null}
    </section>
  );
}
