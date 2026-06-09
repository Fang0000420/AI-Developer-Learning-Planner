"use client";

import { useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  LoaderCircle,
  PlayCircle,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { pollJob, postJson } from "@/lib/client-jobs";
import type { AsyncJob, LearningPlan } from "@/lib/goals";

type GoalDemoRunnerProps = {
  goalId: number;
  hasDecomposition: boolean;
  hasProfile: boolean;
  hasProjectRecommendation: boolean;
  hasSkillGapAnalysis: boolean;
};

type StepStatus = "pending" | "running" | "ready" | "failed";

type DemoStep = {
  endpoint: string;
  key: string;
  label: string;
  ready: boolean;
};

export function GoalDemoRunner({
  goalId,
  hasDecomposition,
  hasProfile,
  hasProjectRecommendation,
  hasSkillGapAnalysis,
}: GoalDemoRunnerProps) {
  const router = useRouter();
  const steps = useMemo<DemoStep[]>(
    () => [
      {
        endpoint: `/api/goals/${goalId}/profile/analyze`,
        key: "profile",
        label: "Profile",
        ready: hasProfile,
      },
      {
        endpoint: `/api/goals/${goalId}/decomposition/decompose`,
        key: "decomposition",
        label: "Decomposition",
        ready: hasDecomposition,
      },
      {
        endpoint: `/api/goals/${goalId}/skill-gap/analyze`,
        key: "skill-gap",
        label: "Skill gaps",
        ready: hasSkillGapAnalysis,
      },
      {
        endpoint: `/api/goals/${goalId}/project-recommendation/recommend`,
        key: "project",
        label: "Project",
        ready: hasProjectRecommendation,
      },
    ],
    [
      goalId,
      hasDecomposition,
      hasProfile,
      hasProjectRecommendation,
      hasSkillGapAnalysis,
    ],
  );
  const [statuses, setStatuses] = useState<Record<string, StepStatus>>(() =>
    Object.fromEntries(
      steps.map((step) => [step.key, step.ready ? "ready" : "pending"]),
    ),
  );
  const [error, setError] = useState<string | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [planStatus, setPlanStatus] = useState<StepStatus>("pending");

  async function runDemoFlow() {
    setError(null);
    setPlanStatus("pending");
    setIsRunning(true);

    try {
      for (const step of steps) {
        if (step.ready) {
          setStatuses((current) => ({ ...current, [step.key]: "ready" }));
          continue;
        }

        setStatuses((current) => ({ ...current, [step.key]: "running" }));
        await postJson(step.endpoint);
        setStatuses((current) => ({ ...current, [step.key]: "ready" }));
      }

      setPlanStatus("running");
      const job = await postJson<AsyncJob<LearningPlan>>(
        "/api/jobs/plan-generation",
        { goalId },
      );
      const plan = await pollJob<LearningPlan>(job.jobId, (currentJob) => {
        setPlanStatus(currentJob.status === "SUCCEEDED" ? "ready" : "running");
      });
      setPlanStatus("ready");
      router.push(`/plans/${plan.id}`);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Demo flow failed.",
      );
      setStatuses((current) => {
        const runningStep = Object.entries(current).find(
          ([, status]) => status === "running",
        );
        return runningStep
          ? { ...current, [runningStep[0]]: "failed" }
          : current;
      });
      setPlanStatus((current) => (current === "running" ? "failed" : current));
    } finally {
      setIsRunning(false);
    }
  }

  return (
    <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
          <PlayCircle aria-hidden="true" className="size-4" />
        </span>
        <div>
          <h2 className="text-base font-semibold text-slate-950">Demo Chain</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Run missing agent steps and create a learning plan for this goal.
          </p>
        </div>
      </div>

      <div className="mt-5 space-y-3">
        {steps.map((step) => {
          const status = statuses[step.key] ?? "pending";

          return (
            <div
              className="flex items-center justify-between gap-3 rounded-md border border-slate-200 px-3 py-2"
              key={step.key}
            >
              <span className="text-sm font-medium text-slate-700">
                {step.label}
              </span>
              <span className="flex items-center gap-1 text-xs font-semibold capitalize text-slate-500">
                {status === "running" ? (
                  <LoaderCircle
                    aria-hidden="true"
                    className="size-3.5 animate-spin"
                  />
                ) : status === "ready" ? (
                  <CheckCircle2
                    aria-hidden="true"
                    className="size-3.5 text-emerald-600"
                  />
                ) : status === "failed" ? (
                  <AlertCircle
                    aria-hidden="true"
                    className="size-3.5 text-rose-600"
                  />
                ) : null}
                {status}
              </span>
            </div>
          );
        })}
        <div className="flex items-center justify-between gap-3 rounded-md border border-slate-200 px-3 py-2">
          <span className="text-sm font-medium text-slate-700">
            Plan generation
          </span>
          <span className="flex items-center gap-1 text-xs font-semibold capitalize text-slate-500">
            {planStatus === "running" ? (
              <LoaderCircle
                aria-hidden="true"
                className="size-3.5 animate-spin"
              />
            ) : planStatus === "ready" ? (
              <CheckCircle2
                aria-hidden="true"
                className="size-3.5 text-emerald-600"
              />
            ) : planStatus === "failed" ? (
              <AlertCircle
                aria-hidden="true"
                className="size-3.5 text-rose-600"
              />
            ) : null}
            {planStatus}
          </span>
        </div>
      </div>

      {error ? (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 p-3 text-sm leading-6 text-rose-700">
          {error}
        </div>
      ) : null}

      <button
        className="mt-5 inline-flex h-10 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        disabled={isRunning}
        onClick={runDemoFlow}
        type="button"
      >
        {isRunning ? (
          <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
        ) : (
          <PlayCircle aria-hidden="true" className="size-4" />
        )}
        {isRunning ? "Running" : "Run to Plan"}
      </button>
    </section>
  );
}
