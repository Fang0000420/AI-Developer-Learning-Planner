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
    unfinishedTasks: Array.isArray(review.unfinishedTasks)
      ? review.unfinishedTasks
      : [],
  };
}

export function ProgressSubmitForm({
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
