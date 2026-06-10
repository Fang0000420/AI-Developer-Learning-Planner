"use client";

import { useState } from "react";
import { LoaderCircle, Pause, Play, Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import type {
  ApiErrorResponse,
  LearningPlan,
  LearningPlanStatus,
  LearningPlanSummary,
} from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type PlanRowActionsProps = {
  locale: Locale;
  plan: LearningPlanSummary;
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
    (locale === "zh" ? "计划操作失败。" : "Plan operation failed.")
  );
}

export function PlanRowActions({ locale, plan }: PlanRowActionsProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<
    "status" | "delete" | null
  >(null);
  const nextStatus: LearningPlanStatus =
    plan.status === "PAUSED" ? "ACTIVE" : "PAUSED";
  const isPaused = plan.status === "PAUSED";

  async function updateStatus() {
    setError(null);
    setPendingAction("status");

    try {
      const response = await fetch(`/api/plans/${plan.id}`, {
        body: JSON.stringify({ status: nextStatus }),
        headers: {
          "Content-Type": "application/json",
        },
        method: "PUT",
      });
      const payload = (await response.json()) as
        | LearningPlan
        | ApiErrorResponse;

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
            ? "计划操作失败。"
            : "Plan operation failed.",
      );
    } finally {
      setPendingAction(null);
    }
  }

  async function deletePlan() {
    if (
      !window.confirm(
        locale === "zh" ? `删除计划 #${plan.id}？` : `Delete plan #${plan.id}?`,
      )
    ) {
      return;
    }

    setError(null);
    setPendingAction("delete");

    try {
      const response = await fetch(`/api/plans/${plan.id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        const payload = (await response.json()) as ApiErrorResponse;
        setError(getErrorMessage(payload, locale));
        return;
      }

      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "计划操作失败。"
            : "Plan operation failed.",
      );
    } finally {
      setPendingAction(null);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex gap-2">
        <button
          className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
          disabled={pendingAction !== null}
          onClick={updateStatus}
          type="button"
        >
          {pendingAction === "status" ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : isPaused ? (
            <Play aria-hidden="true" className="size-4" />
          ) : (
            <Pause aria-hidden="true" className="size-4" />
          )}
          {isPaused
            ? locale === "zh"
              ? "激活"
              : "Activate"
            : locale === "zh"
              ? "暂停"
              : "Pause"}
        </button>
        <button
          className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-rose-200 bg-white text-rose-700 transition-colors hover:bg-rose-50 disabled:cursor-not-allowed disabled:text-rose-300"
          disabled={pendingAction !== null}
          onClick={deletePlan}
          title={locale === "zh" ? "删除计划" : "Delete plan"}
          type="button"
        >
          {pendingAction === "delete" ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Trash2 aria-hidden="true" className="size-4" />
          )}
        </button>
      </div>

      {error ? (
        <p className="text-xs leading-5 text-rose-700">{error}</p>
      ) : null}
    </div>
  );
}
