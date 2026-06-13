"use client";

import type { FormEvent } from "react";
import { useMemo, useState } from "react";
import {
  Activity,
  ArrowRight,
  CheckCircle2,
  ClipboardCheck,
  Lightbulb,
  LoaderCircle,
  MessageSquareText,
  XCircle,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { getApiErrorMessage, pollJob, postJson } from "@/lib/client-jobs";
import type {
  AdaptiveScheduleControl,
  ApiErrorResponse,
  AsyncJob,
  PlanTask,
  ProgressImpact,
  ProgressLog,
  ProgressReviewResult,
} from "@/lib/goals";
import type { Locale } from "@/lib/i18n";
import {
  formatGoalDate,
  getProgressImpactClasses,
  getProgressImpactLabel,
} from "@/lib/goals";

type ProgressSubmitFormProps = {
  adaptiveScheduleControl: AdaptiveScheduleControl | null;
  dayIndex: number;
  latestLog: ProgressLog | null;
  locale: Locale;
  planId: number;
  tasks: PlanTask[];
};

function taskTitleMap(tasks: PlanTask[]) {
  return new Map(tasks.map((task) => [task.id, task.title]));
}

function normalizeReview(
  review: ProgressReviewResult | null | undefined,
): ProgressReviewResult | null {
  if (!review || !review.impact || !review.suggestion) {
    return null;
  }

  return {
    blockers: Array.isArray(review.blockers) ? review.blockers : [],
    completedTasks: Array.isArray(review.completedTasks)
      ? review.completedTasks
      : [],
    impact: review.impact,
    planAdjustment: review.planAdjustment,
    suggestion: review.suggestion,
    wins: Array.isArray(review.wins) ? review.wins : [],
    nextFocus: Array.isArray(review.nextFocus) ? review.nextFocus : [],
    paceAdjustment: review.paceAdjustment ?? "keep",
    confidence: review.confidence ?? "medium",
    adaptiveSchedule: review.adaptiveSchedule,
    unfinishedTasks: Array.isArray(review.unfinishedTasks)
      ? review.unfinishedTasks
      : [],
  };
}

function paceAdjustmentLabel(
  paceAdjustment: "keep" | "slower" | "faster",
  locale: Locale,
) {
  if (locale === "zh") {
    return {
      faster: "可以加快",
      keep: "保持节奏",
      slower: "需要放慢",
    }[paceAdjustment];
  }

  return {
    faster: "Can go faster",
    keep: "Keep the pace",
    slower: "Slow down",
  }[paceAdjustment];
}

function confidenceLabel(
  confidence: "low" | "medium" | "high",
  locale: Locale,
) {
  if (locale === "zh") {
    return {
      high: "高",
      low: "低",
      medium: "中",
    }[confidence];
  }

  return {
    high: "High",
    low: "Low",
    medium: "Medium",
  }[confidence];
}

function adaptivePacingLabel(
  pacing: "lighter" | "steady" | "stronger",
  locale: Locale,
) {
  if (locale === "zh") {
    return {
      lighter: "后续减负",
      steady: "保持当前密度",
      stronger: "后续加压",
    }[pacing];
  }

  return {
    lighter: "Lighten ahead",
    steady: "Keep current density",
    stronger: "Increase ahead",
  }[pacing];
}

export function ProgressSubmitForm({
  adaptiveScheduleControl,
  dayIndex,
  latestLog,
  locale,
  planId,
  tasks,
}: ProgressSubmitFormProps) {
  const router = useRouter();
  const titlesById = useMemo(() => taskTitleMap(tasks), [tasks]);
  const latestReview = normalizeReview(latestLog?.reviewResultJson);
  const [completedTaskIds, setCompletedTaskIds] = useState<number[]>(
    tasks.filter((task) => task.status === "DONE").map((task) => task.id),
  );
  const [unfinishedTaskIds, setUnfinishedTaskIds] = useState<number[]>(
    tasks.filter((task) => task.status !== "DONE").map((task) => task.id),
  );
  const [blockers, setBlockers] = useState(
    latestLog?.blockers.join("\n") ?? "",
  );
  const [userFeedback, setUserFeedback] = useState(
    latestLog?.userFeedback ?? "",
  );
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [jobStatus, setJobStatus] = useState<string | null>(null);
  const [isOverriding, setIsOverriding] = useState(false);

  function toggleCompleted(taskId: number) {
    setCompletedTaskIds((current) =>
      current.includes(taskId)
        ? current.filter((id) => id !== taskId)
        : [...current, taskId],
    );
    setUnfinishedTaskIds((current) => current.filter((id) => id !== taskId));
  }

  function toggleUnfinished(taskId: number) {
    setUnfinishedTaskIds((current) =>
      current.includes(taskId)
        ? current.filter((id) => id !== taskId)
        : [...current, taskId],
    );
    setCompletedTaskIds((current) => current.filter((id) => id !== taskId));
  }

  async function submitProgress(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setJobStatus(locale === "zh" ? "启动中" : "Starting");
    setIsSubmitting(true);

    try {
      const job = await postJson<AsyncJob<ProgressLog>>(
        "/api/jobs/progress-submission",
        {
          blockers: blockers
            .split("\n")
            .map((blocker) => blocker.trim())
            .filter(Boolean),
          completedTaskIds,
          dayIndex,
          planId,
          unfinishedTaskIds,
          userFeedback,
        },
      );
      setJobStatus(job.status);
      await pollJob<ProgressLog>(job.jobId, (currentJob) => {
        setJobStatus(currentJob.status);
      });
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "进度提交失败。"
            : "Progress submission failed.",
      );
    } finally {
      setJobStatus(null);
      setIsSubmitting(false);
    }
  }

  async function overrideAdaptiveSchedule(
    pacing: "lighter" | "steady" | "stronger",
  ) {
    setError(null);
    setIsOverriding(true);

    try {
      const response = await fetch(`/api/progress/${planId}`, {
        body: JSON.stringify({ pacing }),
        headers: { "Content-Type": "application/json" },
        method: "PATCH",
      });
      if (!response.ok) {
        const payload = (await response.json()) as ApiErrorResponse;
        setError(
          getApiErrorMessage(
            payload,
            locale === "zh"
              ? "手动覆盖自适应调度失败。"
              : "Adaptive schedule override failed.",
          ),
        );
        return;
      }
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "手动覆盖自适应调度失败。"
            : "Adaptive schedule override failed.",
      );
    } finally {
      setIsOverriding(false);
    }
  }

  return (
    <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-500">
            <ClipboardCheck aria-hidden="true" className="size-4" />
            {locale === "zh" ? "进度" : "Progress"}
          </div>
          <h2 className="mt-2 text-xl font-semibold text-slate-950">
            {locale === "zh"
              ? `提交第 ${dayIndex} 天`
              : `Submit Day ${dayIndex}`}
          </h2>
        </div>
        {latestLog ? (
          <div className="rounded-md bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-700 ring-1 ring-emerald-200">
            {locale === "zh" ? "上次提交 " : "Last submitted "}
            {formatGoalDate(latestLog.createdAt, locale)}
          </div>
        ) : null}
      </div>

      <form className="mt-6 flex flex-col gap-5" onSubmit={submitProgress}>
        {adaptiveScheduleControl ? (
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
              <Activity aria-hidden="true" className="size-4" />
              {locale === "zh"
                ? "调度解释与覆盖"
                : "Scheduling explanation and override"}
            </div>
            {adaptiveScheduleControl.latestAutomatic?.pacing ? (
              <div className="mt-3 rounded-md border border-slate-200 bg-white p-4 text-sm">
                <p className="font-semibold text-slate-800">
                  {locale === "zh" ? "当前自动判断：" : "Current automatic mode: "}
                  {adaptivePacingLabel(
                    adaptiveScheduleControl.latestAutomatic.pacing,
                    locale,
                  )}
                </p>
                {adaptiveScheduleControl.latestAutomatic.reason ? (
                  <p className="mt-2 leading-6 text-slate-700">
                    {adaptiveScheduleControl.latestAutomatic.reason}
                  </p>
                ) : null}
                {adaptiveScheduleControl.evidence &&
                adaptiveScheduleControl.evidence.length > 0 ? (
                  <div className="mt-3">
                    <div className="font-medium text-slate-500">
                      {locale === "zh" ? "判断依据" : "Evidence"}
                    </div>
                    <ul className="mt-2 space-y-1 text-slate-700">
                      {adaptiveScheduleControl.evidence.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </div>
            ) : null}
            {adaptiveScheduleControl.activeOverride?.pacing ? (
              <div className="mt-3 rounded-md border border-amber-200 bg-amber-50 p-4 text-sm">
                <p className="font-semibold text-amber-900">
                  {locale === "zh" ? "当前手动覆盖：" : "Current manual override: "}
                  {adaptivePacingLabel(
                    adaptiveScheduleControl.activeOverride.pacing,
                    locale,
                  )}
                </p>
                {adaptiveScheduleControl.activeOverride.reason ? (
                  <p className="mt-2 leading-6 text-amber-800">
                    {adaptiveScheduleControl.activeOverride.reason}
                  </p>
                ) : null}
              </div>
            ) : null}
            <div className="mt-4 flex flex-wrap gap-2">
              {(
                [
                  ["lighter", locale === "zh" ? "手动减负" : "Lighten"],
                  ["steady", locale === "zh" ? "保持当前" : "Keep steady"],
                  ["stronger", locale === "zh" ? "手动加压" : "Intensify"],
                ] as const
              ).map(([pacing, label]) => (
                <button
                  className="inline-flex h-10 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-400"
                  disabled={isOverriding}
                  key={pacing}
                  onClick={() => overrideAdaptiveSchedule(pacing)}
                  type="button"
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        <div className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-md border border-slate-200 p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
              <CheckCircle2 aria-hidden="true" className="size-4" />
              {locale === "zh" ? "已完成" : "Completed"}
            </div>
            <div className="mt-3 flex flex-col gap-2">
              {tasks.map((task) => (
                <label
                  className="flex min-h-11 items-start gap-3 rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700"
                  key={`completed-${task.id}`}
                >
                  <input
                    checked={completedTaskIds.includes(task.id)}
                    className="mt-1 size-4 rounded border-slate-300 text-slate-950"
                    onChange={() => toggleCompleted(task.id)}
                    type="checkbox"
                  />
                  <span>{task.title}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="rounded-md border border-slate-200 p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
              <XCircle aria-hidden="true" className="size-4" />
              {locale === "zh" ? "未完成" : "Unfinished"}
            </div>
            <div className="mt-3 flex flex-col gap-2">
              {tasks.map((task) => (
                <label
                  className="flex min-h-11 items-start gap-3 rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700"
                  key={`unfinished-${task.id}`}
                >
                  <input
                    checked={unfinishedTaskIds.includes(task.id)}
                    className="mt-1 size-4 rounded border-slate-300 text-slate-950"
                    onChange={() => toggleUnfinished(task.id)}
                    type="checkbox"
                  />
                  <span>{task.title}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
          <span className="flex items-center gap-2">
            <MessageSquareText aria-hidden="true" className="size-4" />
            {locale === "zh" ? "反馈" : "Feedback"}
          </span>
          <textarea
            className="min-h-28 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-normal leading-6 text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-slate-950"
            onChange={(event) => setUserFeedback(event.target.value)}
            placeholder={
              locale === "zh" ? "今天发生了什么变化？" : "What changed today?"
            }
            required
            value={userFeedback}
          />
        </label>

        <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
          {locale === "zh" ? "阻塞项" : "Blockers"}
          <textarea
            className="min-h-24 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-normal leading-6 text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-slate-950"
            onChange={(event) => setBlockers(event.target.value)}
            placeholder={
              locale === "zh" ? "每行一个阻塞项" : "One blocker per line"
            }
            value={blockers}
          />
        </label>

        {latestLog ? (
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
            <div className="text-sm font-semibold text-slate-700">
              {locale === "zh" ? "最近记录" : "Latest record"}
            </div>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {latestLog.userFeedback}
            </p>
            <div className="mt-3 grid gap-3 text-sm lg:grid-cols-2">
              <div>
                <div className="font-medium text-slate-500">
                  {locale === "zh" ? "已完成" : "Completed"}
                </div>
                <p className="mt-1 leading-6 text-slate-700">
                  {latestLog.completedTaskIds
                    .map(
                      (taskId) =>
                        titlesById.get(taskId) ??
                        `${locale === "zh" ? "任务" : "Task"} ${taskId}`,
                    )
                    .join(", ") || (locale === "zh" ? "无" : "None")}
                </p>
              </div>
              <div>
                <div className="font-medium text-slate-500">
                  {locale === "zh" ? "未完成" : "Unfinished"}
                </div>
                <p className="mt-1 leading-6 text-slate-700">
                  {latestLog.unfinishedTaskIds
                    .map(
                      (taskId) =>
                        titlesById.get(taskId) ??
                        `${locale === "zh" ? "任务" : "Task"} ${taskId}`,
                    )
                    .join(", ") || (locale === "zh" ? "无" : "None")}
                </p>
              </div>
            </div>
            {latestReview ? (
              <div className="mt-4 border-t border-slate-200 pt-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
                    <Activity aria-hidden="true" className="size-4" />
                    {locale === "zh" ? "复盘" : "Review"}
                  </div>
                  <span
                    className={`inline-flex w-fit items-center rounded-md px-2.5 py-1 text-xs font-semibold ring-1 ${getProgressImpactClasses(
                      latestReview.impact as ProgressImpact,
                    )}`}
                  >
                    {getProgressImpactLabel(
                      latestReview.impact as ProgressImpact,
                      locale,
                    )}
                  </span>
                </div>
                <p className="mt-3 flex gap-2 text-sm leading-6 text-slate-700">
                  <Lightbulb
                    aria-hidden="true"
                    className="mt-1 size-4 shrink-0 text-amber-600"
                  />
                  <span>{latestReview.suggestion}</span>
                </p>
                <div className="mt-3 grid gap-3 md:grid-cols-2">
                  <div className="rounded-md border border-slate-200 bg-white p-3 text-sm">
                    <div className="font-medium text-slate-500">
                      {locale === "zh" ? "节奏判断" : "Pace judgment"}
                    </div>
                    <p className="mt-1 text-slate-700">
                      {paceAdjustmentLabel(
                        latestReview.paceAdjustment ?? "keep",
                        locale,
                      )}
                    </p>
                  </div>
                  <div className="rounded-md border border-slate-200 bg-white p-3 text-sm">
                    <div className="font-medium text-slate-500">
                      {locale === "zh" ? "复盘信心" : "Review confidence"}
                    </div>
                    <p className="mt-1 text-slate-700">
                      {confidenceLabel(
                        latestReview.confidence ?? "medium",
                        locale,
                      )}
                    </p>
                  </div>
                </div>
                {latestReview.wins && latestReview.wins.length > 0 ? (
                  <div className="mt-3 text-sm">
                    <div className="font-medium text-slate-500">
                      {locale === "zh" ? "今天的收获" : "Today's wins"}
                    </div>
                    <ul className="mt-2 space-y-1 text-slate-700">
                      {latestReview.wins.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </div>
                ) : null}
                {latestReview.nextFocus && latestReview.nextFocus.length > 0 ? (
                  <div className="mt-3 text-sm">
                    <div className="font-medium text-slate-500">
                      {locale === "zh" ? "下一步聚焦" : "Next focus"}
                    </div>
                    <ul className="mt-2 space-y-1 text-slate-700">
                      {latestReview.nextFocus.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </div>
                ) : null}
                {latestReview.adaptiveSchedule?.pacing ? (
                  <div className="mt-4 rounded-md border border-slate-200 bg-white p-4 text-sm">
                    <div className="flex items-center gap-2 font-medium text-slate-500">
                      <Activity aria-hidden="true" className="size-4" />
                      {locale === "zh"
                        ? "自适应调度"
                        : "Adaptive scheduling"}
                    </div>
                    <p className="mt-2 font-semibold text-slate-800">
                      {adaptivePacingLabel(
                        latestReview.adaptiveSchedule.pacing,
                        locale,
                      )}
                    </p>
                    {latestReview.adaptiveSchedule.reason ? (
                      <p className="mt-2 leading-6 text-slate-700">
                        {latestReview.adaptiveSchedule.reason}
                      </p>
                    ) : null}
                    <div className="mt-3 grid gap-3 md:grid-cols-3">
                      <div className="rounded-md border border-slate-200 bg-slate-50 p-3">
                        <div className="font-medium text-slate-500">
                          {locale === "zh" ? "近期完成率" : "Recent completion"}
                        </div>
                        <p className="mt-1 text-slate-700">
                          {Math.round(
                            (latestReview.adaptiveSchedule
                              .recentCompletionRate ?? 0) * 100,
                          )}
                          %
                        </p>
                      </div>
                      <div className="rounded-md border border-slate-200 bg-slate-50 p-3">
                        <div className="font-medium text-slate-500">
                          {locale === "zh" ? "平均阻塞" : "Average blockers"}
                        </div>
                        <p className="mt-1 text-slate-700">
                          {latestReview.adaptiveSchedule.recentBlockerAverage ?? 0}
                        </p>
                      </div>
                      <div className="rounded-md border border-slate-200 bg-slate-50 p-3">
                        <div className="font-medium text-slate-500">
                          {locale === "zh" ? "分钟调整" : "Minute change"}
                        </div>
                        <p className="mt-1 text-slate-700">
                          {latestReview.adaptiveSchedule.minuteAdjustmentPercent ??
                            0}
                          %
                        </p>
                      </div>
                    </div>
                    {latestReview.adaptiveSchedule.affectedDayIndexes &&
                    latestReview.adaptiveSchedule.affectedDayIndexes.length > 0 ? (
                      <p className="mt-3 text-slate-600">
                        {locale === "zh" ? "影响天数：" : "Affected days: "}
                        {latestReview.adaptiveSchedule.affectedDayIndexes.join(
                          ", ",
                        )}
                      </p>
                    ) : null}
                  </div>
                ) : null}
                {latestReview.blockers && latestReview.blockers.length > 0 ? (
                  <div className="mt-3 text-sm">
                    <div className="font-medium text-slate-500">
                      {locale === "zh" ? "复盘阻塞项" : "Reviewer blockers"}
                    </div>
                    <p className="mt-1 leading-6 text-slate-700">
                      {latestReview.blockers.join(", ")}
                    </p>
                  </div>
                ) : null}
                {latestReview.planAdjustment ? (
                  <div className="mt-4 rounded-md border border-sky-200 bg-sky-50 p-4">
                    <div className="flex items-center gap-2 text-sm font-semibold text-sky-900">
                      <ArrowRight aria-hidden="true" className="size-4" />
                      {locale === "zh" ? "明日调整" : "Tomorrow adjustment"}
                    </div>
                    {latestReview.planAdjustment.reason ? (
                      <p className="mt-2 text-sm leading-6 text-sky-800">
                        {latestReview.planAdjustment.reason}
                      </p>
                    ) : null}
                    {latestReview.planAdjustment.movedTasks &&
                    latestReview.planAdjustment.movedTasks.length > 0 ? (
                      <div className="mt-3 text-sm">
                        <div className="font-medium text-sky-900">
                          {locale === "zh" ? "移动任务" : "Moved tasks"}
                        </div>
                        <ul className="mt-2 space-y-1 text-sky-800">
                          {latestReview.planAdjustment.movedTasks.map(
                            (task) => (
                              <li key={`${task.taskId}-${task.title}`}>
                                {locale === "zh"
                                  ? `${task.title}: 第 ${task.fromDayIndex} 天 → 第 ${task.toDayIndex} 天`
                                  : `${task.title}: Day ${task.fromDayIndex} to Day ${task.toDayIndex}`}
                              </li>
                            ),
                          )}
                        </ul>
                      </div>
                    ) : null}
                    {latestReview.planAdjustment.splitTasks &&
                    latestReview.planAdjustment.splitTasks.length > 0 ? (
                      <div className="mt-3 text-sm">
                        <div className="font-medium text-sky-900">
                          {locale === "zh" ? "拆分任务" : "Split tasks"}
                        </div>
                        <ul className="mt-2 space-y-2 text-sky-800">
                          {latestReview.planAdjustment.splitTasks.map(
                            (task) => (
                              <li
                                key={`${task.sourceTaskId}-${task.sourceTitle}`}
                              >
                                <div>{task.sourceTitle}</div>
                                {task.parts.length > 0 ? (
                                  <div className="mt-1 text-sky-700">
                                    {task.parts
                                      .map((part) => part.title)
                                      .join(", ")}
                                  </div>
                                ) : null}
                              </li>
                            ),
                          )}
                        </ul>
                      </div>
                    ) : null}
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>
        ) : null}

        {isSubmitting && jobStatus ? (
          <div className="rounded-md border border-sky-200 bg-sky-50 p-3 text-sm font-medium text-sky-800">
            {locale === "zh" ? "异步任务状态：" : "Async job status: "}
            {jobStatus}
          </div>
        ) : null}

        {error ? (
          <p className="text-sm text-rose-700">
            {getApiErrorMessage(
              { message: error } as ApiErrorResponse,
              locale === "zh"
                ? "进度提交失败。"
                : "Progress submission failed.",
            )}
          </p>
        ) : null}

        <div className="flex justify-end">
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? (
              <LoaderCircle
                aria-hidden="true"
                className="size-4 animate-spin"
              />
            ) : (
              <ClipboardCheck aria-hidden="true" className="size-4" />
            )}
            {isSubmitting
              ? locale === "zh"
                ? "提交中"
                : "Submitting"
              : locale === "zh"
                ? "提交"
                : "Submit"}
          </button>
        </div>
      </form>
    </section>
  );
}
