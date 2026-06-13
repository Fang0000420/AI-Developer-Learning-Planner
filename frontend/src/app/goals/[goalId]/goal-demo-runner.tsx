"use client";

import { useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  LoaderCircle,
  PlayCircle,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { pollJob, postJson } from "@/lib/client-jobs";
import type { AsyncJob, LearningPlan } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type GoalDemoRunnerProps = {
  goalId: number;
  hasPathRecommendation: boolean;
  locale: Locale;
};

type StepStatus = "pending" | "running" | "ready" | "failed";

function statusLabel(status: StepStatus, locale: Locale) {
  if (locale === "zh") {
    const labels: Record<StepStatus, string> = {
      failed: "失败",
      pending: "待开始",
      ready: "就绪",
      running: "运行中",
    };
    return labels[status];
  }
  return status;
}

export function GoalDemoRunner({
  goalId,
  hasPathRecommendation,
  locale,
}: GoalDemoRunnerProps) {
  const router = useRouter();
  const [pathStatus, setPathStatus] = useState<StepStatus>(
    hasPathRecommendation ? "ready" : "pending",
  );
  const [error, setError] = useState<string | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [planStatus, setPlanStatus] = useState<StepStatus>("pending");

  async function runDemoFlow() {
    setError(null);
    setPlanStatus("pending");
    setIsRunning(true);

    try {
      setPathStatus("running");
      await postJson<AsyncJob<unknown>>("/api/jobs/path-analysis", {
        goalId,
      }).then((job) => pollJob(job.jobId));
      setPathStatus("ready");

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
          : locale === "zh"
            ? "演示链路失败。"
            : "Demo flow failed.",
      );
      setPathStatus((current) => (current === "running" ? "failed" : current));
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
          <h2 className="text-base font-semibold text-slate-950">
            {locale === "zh" ? "演示链路" : "Demo Chain"}
          </h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            {locale === "zh"
              ? "先统一运行路径分析，再基于路径结果生成学习计划。"
              : "Run the unified path analysis first, then generate a learning plan from that path."}
          </p>
        </div>
      </div>

      <div className="mt-5 space-y-3">
        <div className="flex items-center justify-between gap-3 rounded-md border border-slate-200 px-3 py-2">
          <span className="text-sm font-medium text-slate-700">
            {locale === "zh" ? "路径分析" : "Path analysis"}
          </span>
          <span className="flex items-center gap-1 text-xs font-semibold capitalize text-slate-500">
            {pathStatus === "running" ? (
              <LoaderCircle
                aria-hidden="true"
                className="size-3.5 animate-spin"
              />
            ) : pathStatus === "ready" ? (
              <CheckCircle2
                aria-hidden="true"
                className="size-3.5 text-emerald-600"
              />
            ) : pathStatus === "failed" ? (
              <AlertCircle
                aria-hidden="true"
                className="size-3.5 text-rose-600"
              />
            ) : null}
            {statusLabel(pathStatus, locale)}
          </span>
        </div>
        <div className="flex items-center justify-between gap-3 rounded-md border border-slate-200 px-3 py-2">
          <span className="text-sm font-medium text-slate-700">
            {locale === "zh" ? "计划生成" : "Plan generation"}
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
            {statusLabel(planStatus, locale)}
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
        {isRunning
          ? locale === "zh"
            ? "运行中"
            : "Running"
          : locale === "zh"
            ? "运行到计划"
            : "Run to Plan"}
      </button>
    </section>
  );
}
