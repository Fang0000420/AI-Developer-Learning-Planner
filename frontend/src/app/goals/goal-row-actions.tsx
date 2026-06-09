"use client";

import { useState } from "react";
import { LoaderCircle, Pause, Play, Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import type { ApiErrorResponse, Goal, GoalStatus } from "@/lib/goals";

type GoalRowActionsProps = {
  goal: Goal;
};

function getErrorMessage(error: ApiErrorResponse) {
  if (error.errors) {
    const firstError = Object.values(error.errors)[0];
    if (firstError) {
      return firstError;
    }
  }

  return error.message || "Goal operation failed.";
}

export function GoalRowActions({ goal }: GoalRowActionsProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<
    "status" | "delete" | null
  >(null);
  const nextStatus: GoalStatus = goal.status === "PAUSED" ? "ACTIVE" : "PAUSED";
  const isPaused = goal.status === "PAUSED";

  async function updateStatus() {
    setError(null);
    setPendingAction("status");

    try {
      const response = await fetch(`/api/goals/${goal.id}`, {
        body: JSON.stringify({
          title: goal.title,
          description: goal.description,
          durationDays: goal.durationDays,
          status: nextStatus,
          dailyAvailableHours: goal.dailyAvailableHours ?? 2,
        }),
        headers: {
          "Content-Type": "application/json",
        },
        method: "PUT",
      });
      const payload = (await response.json()) as Goal | ApiErrorResponse;

      if (!response.ok) {
        setError(getErrorMessage(payload as ApiErrorResponse));
        return;
      }

      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Goal operation failed.",
      );
    } finally {
      setPendingAction(null);
    }
  }

  async function deleteGoal() {
    if (!window.confirm(`Delete goal #${goal.id}?`)) {
      return;
    }

    setError(null);
    setPendingAction("delete");

    try {
      const response = await fetch(`/api/goals/${goal.id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        const payload = (await response.json()) as ApiErrorResponse;
        setError(getErrorMessage(payload));
        return;
      }

      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Goal operation failed.",
      );
    } finally {
      setPendingAction(null);
    }
  }

  return (
    <div className="flex shrink-0 flex-col gap-2 lg:w-40">
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
          {isPaused ? "Activate" : "Pause"}
        </button>
        <button
          className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-rose-200 bg-white text-rose-700 transition-colors hover:bg-rose-50 disabled:cursor-not-allowed disabled:text-rose-300"
          disabled={pendingAction !== null}
          onClick={deleteGoal}
          title="Delete goal"
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
