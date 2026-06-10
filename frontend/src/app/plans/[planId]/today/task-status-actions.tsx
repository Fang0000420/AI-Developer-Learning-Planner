"use client";

import { useState } from "react";
import {
  CheckCircle2,
  Circle,
  LoaderCircle,
  PlayCircle,
  SkipForward,
} from "lucide-react";
import { useRouter } from "next/navigation";
import type { ApiErrorResponse, DailyTaskStatus, PlanTask } from "@/lib/goals";
import { getDailyTaskStatusLabel } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type TaskStatusActionsProps = {
  locale: Locale;
  planId: number;
  task: PlanTask;
};

const statusOptions: {
  icon: typeof Circle;
  status: DailyTaskStatus;
}[] = [
  { icon: Circle, status: "PENDING" },
  { icon: PlayCircle, status: "IN_PROGRESS" },
  { icon: CheckCircle2, status: "DONE" },
  { icon: SkipForward, status: "SKIPPED" },
];

function getErrorMessage(error: ApiErrorResponse, locale: Locale) {
  if (error.errors) {
    const firstError = Object.values(error.errors)[0];
    if (firstError) {
      return firstError;
    }
  }

  return (
    error.message ||
    (locale === "zh" ? "任务状态更新失败。" : "Task status update failed.")
  );
}

export function TaskStatusActions({
  locale,
  planId,
  task,
}: TaskStatusActionsProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pendingStatus, setPendingStatus] = useState<DailyTaskStatus | null>(
    null,
  );

  async function updateStatus(status: DailyTaskStatus) {
    if (status === task.status) {
      return;
    }

    setError(null);
    setPendingStatus(status);

    try {
      const response = await fetch(
        `/api/plans/${planId}/tasks/${task.id}/status`,
        {
          body: JSON.stringify({ status }),
          headers: {
            "Content-Type": "application/json",
          },
          method: "PUT",
        },
      );
      const payload = (await response.json()) as PlanTask | ApiErrorResponse;

      if (!response.ok) {
        setError(getErrorMessage(payload as ApiErrorResponse, locale));
        return;
      }

      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "任务状态更新失败。"
            : "Task status update failed.",
      );
    } finally {
      setPendingStatus(null);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
        {statusOptions.map((option) => {
          const Icon = option.icon;
          const isActive = option.status === task.status;
          const isPending = option.status === pendingStatus;

          return (
            <button
              className={`inline-flex h-9 items-center justify-center gap-2 rounded-md border px-3 text-xs font-semibold transition-colors disabled:cursor-not-allowed ${
                isActive
                  ? "border-slate-950 bg-slate-950 text-white"
                  : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
              }`}
              disabled={pendingStatus !== null}
              key={option.status}
              onClick={() => updateStatus(option.status)}
              title={getDailyTaskStatusLabel(option.status, locale)}
              type="button"
            >
              {isPending ? (
                <LoaderCircle
                  aria-hidden="true"
                  className="size-4 animate-spin"
                />
              ) : (
                <Icon aria-hidden="true" className="size-4" />
              )}
              {getDailyTaskStatusLabel(option.status, locale)}
            </button>
          );
        })}
      </div>

      {error ? (
        <p className="text-xs leading-5 text-rose-700">{error}</p>
      ) : null}
    </div>
  );
}
