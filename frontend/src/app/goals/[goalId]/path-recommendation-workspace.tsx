"use client";

import { useState } from "react";
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  Compass,
  FileText,
  Layers3,
  LoaderCircle,
  Milestone,
  Rocket,
  ShieldAlert,
  Sparkles,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { getApiErrorMessage, pollJob, postJson } from "@/lib/client-jobs";
import type {
  ApiErrorResponse,
  AsyncJob,
  LearningPlan,
  PathRecommendation,
} from "@/lib/goals";
import { formatDailyHours } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";
import {
  knowledgeScopeLabel,
  knowledgeSourceCategoryLabel,
} from "@/lib/knowledge";

type PathRecommendationWorkspaceProps = {
  goalId: number;
  initialError?: string | null;
  initialRecommendation: PathRecommendation | null;
  locale: Locale;
};

export function PathRecommendationWorkspace({
  goalId,
  initialError = null,
  initialRecommendation,
  locale,
}: PathRecommendationWorkspaceProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(initialError);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isGeneratingPlan, setIsGeneratingPlan] = useState(false);
  const [planJobStatus, setPlanJobStatus] = useState<string | null>(null);
  const [planError, setPlanError] = useState<string | null>(null);
  const [recommendation, setRecommendation] =
    useState<PathRecommendation | null>(initialRecommendation);

  async function handleAnalyze() {
    setError(null);
    setIsAnalyzing(true);

    try {
      const job = await postJson<AsyncJob<PathRecommendation>>(
        "/api/jobs/path-analysis",
        { goalId },
      );
      const result = await pollJob<PathRecommendation>(job.jobId);
      setRecommendation(result);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "路径分析失败。"
            : "Path analysis failed.",
      );
    } finally {
      setIsAnalyzing(false);
    }
  }

  async function handleAnalyzeNow() {
    setError(null);
    setIsAnalyzing(true);

    try {
      const response = await fetch(
        `/api/goals/${goalId}/path-recommendation/analyze`,
        {
          method: "POST",
        },
      );
      const payload = (await response.json()) as
        | PathRecommendation
        | ApiErrorResponse;

      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh" ? "路径分析失败。" : "Path analysis failed.",
          ),
        );
        return;
      }

      setRecommendation(payload as PathRecommendation);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "路径分析失败。"
            : "Path analysis failed.",
      );
    } finally {
      setIsAnalyzing(false);
    }
  }

  async function handleGeneratePlan() {
    setPlanError(null);
    setPlanJobStatus(locale === "zh" ? "启动中" : "Starting");
    setIsGeneratingPlan(true);

    try {
      const job = await postJson<AsyncJob<LearningPlan>>(
        "/api/jobs/plan-generation",
        { goalId },
      );
      setPlanJobStatus(job.status);
      const plan = await pollJob<LearningPlan>(job.jobId, (currentJob) => {
        setPlanJobStatus(currentJob.status);
      });
      router.push(`/plans/${plan.id}`);
    } catch (requestError) {
      setPlanError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "学习计划生成失败。"
            : "Learning plan generation failed.",
      );
    } finally {
      setPlanJobStatus(null);
      setIsGeneratingPlan(false);
    }
  }

  return (
    <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
            <Compass aria-hidden="true" className="size-4" />
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold text-slate-950">
                {locale === "zh" ? "成长路径工作台" : "Growth Path Workspace"}
              </h2>
              {recommendation ? (
                <span className="inline-flex h-7 items-center gap-1 rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                  <CheckCircle2 aria-hidden="true" className="size-3.5" />
                  {locale === "zh"
                    ? `版本 ${recommendation.version}`
                    : `Version ${recommendation.version}`}
                </span>
              ) : null}
            </div>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "先把当前定位、推荐主线、里程碑和下一步动作整合成一条清晰路径，再进入学习计划。"
                : "Turn the current position, recommended track, milestones, and next action into one clear path before generating a plan."}
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row">
          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
            disabled={isAnalyzing}
            onClick={recommendation ? handleAnalyze : handleAnalyzeNow}
            type="button"
          >
            {isAnalyzing ? (
              <LoaderCircle
                aria-hidden="true"
                className="size-4 animate-spin"
              />
            ) : (
              <Sparkles aria-hidden="true" className="size-4" />
            )}
            {recommendation
              ? locale === "zh"
                ? "刷新路径"
                : "Refresh Path"
              : locale === "zh"
                ? "生成路径"
                : "Generate Path"}
          </button>

          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isGeneratingPlan || !recommendation}
            onClick={handleGeneratePlan}
            type="button"
          >
            {isGeneratingPlan ? (
              <LoaderCircle
                aria-hidden="true"
                className="size-4 animate-spin"
              />
            ) : (
              <Rocket aria-hidden="true" className="size-4" />
            )}
            {isGeneratingPlan
              ? locale === "zh"
                ? `生成计划中${planJobStatus ? `：${planJobStatus}` : ""}`
                : `Generating Plan${planJobStatus ? `: ${planJobStatus}` : ""}`
              : locale === "zh"
                ? "基于路径生成计划"
                : "Generate Plan from Path"}
          </button>
        </div>
      </div>

      {error ? (
        <div className="mt-5 rounded-md border border-rose-200 bg-rose-50 p-4">
          <p className="text-sm font-semibold text-rose-950">
            {locale === "zh"
              ? "无法生成成长路径。"
              : "Unable to generate the growth path."}
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
                  {recommendation.recommendedPath}
                </h3>
                <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600">
                  {recommendation.summary}
                </p>
              </div>
              <span className="inline-flex h-8 items-center rounded-md bg-emerald-100 px-3 text-sm font-semibold text-emerald-700 ring-1 ring-emerald-200">
                {recommendation.difficulty}
              </span>
            </div>

            <div className="mt-5 grid gap-3 md:grid-cols-3">
              <div className="rounded-md border border-slate-200 bg-white p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "周期" : "Duration"}
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {recommendation.durationDays}{" "}
                  {locale === "zh" ? "天" : "days"}
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-white p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "每日时间" : "Daily time"}
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {formatDailyHours(recommendation.dailyTimeHours, locale)}
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-white p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "下一步" : "Next step"}
                </p>
                <p className="mt-1 text-sm font-semibold leading-6 text-slate-950">
                  {recommendation.nextStep}
                </p>
              </div>
            </div>
          </div>

          <div className="grid gap-5 lg:grid-cols-2">
            <section className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <ArrowRight
                  aria-hidden="true"
                  className="size-4 text-slate-500"
                />
                {locale === "zh" ? "当前定位" : "Current Position"}
              </div>
              <p className="mt-4 text-sm leading-7 text-slate-600">
                {recommendation.currentPosition}
              </p>
              <div className="mt-5 flex flex-wrap gap-2">
                {recommendation.focusAreas.map((focus) => (
                  <span
                    className="inline-flex h-8 items-center rounded-md bg-slate-100 px-3 text-sm font-medium text-slate-700"
                    key={focus}
                  >
                    {focus}
                  </span>
                ))}
              </div>
            </section>

            <section className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <Milestone
                  aria-hidden="true"
                  className="size-4 text-slate-500"
                />
                {locale === "zh" ? "近期里程碑" : "Near-term Milestones"}
              </div>
              <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-600">
                {recommendation.milestones.map((milestone) => (
                  <li className="flex gap-2" key={milestone}>
                    <CheckCircle2
                      aria-hidden="true"
                      className="mt-1 size-4 shrink-0 text-emerald-600"
                    />
                    <span>{milestone}</span>
                  </li>
                ))}
              </ul>
            </section>
          </div>

          <div className="grid gap-5 lg:grid-cols-2">
            <section className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <ShieldAlert
                  aria-hidden="true"
                  className="size-4 text-slate-500"
                />
                {locale === "zh" ? "风险提醒" : "Risk Signals"}
              </div>
              <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-600">
                {recommendation.riskSignals.map((signal) => (
                  <li className="flex gap-2" key={signal}>
                    <AlertCircle
                      aria-hidden="true"
                      className="mt-1 size-4 shrink-0 text-amber-600"
                    />
                    <span>{signal}</span>
                  </li>
                ))}
              </ul>
            </section>

            <section className="rounded-md border border-slate-200 p-5">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
                <Layers3 aria-hidden="true" className="size-4 text-slate-500" />
                {locale === "zh" ? "推荐依据" : "Evidence"}
              </div>
              <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-600">
                {recommendation.evidence.map((item) => (
                  <li className="flex gap-2" key={item}>
                    <CheckCircle2
                      aria-hidden="true"
                      className="mt-1 size-4 shrink-0 text-sky-600"
                    />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </section>
          </div>

          <section className="rounded-md border border-slate-200 p-5">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
              <FileText aria-hidden="true" className="size-4 text-slate-500" />
              {locale === "zh" ? "知识依据" : "Knowledge Basis"}
            </div>
            <p className="mt-4 text-sm leading-6 text-slate-600">
              {recommendation.knowledgeBasis.summary}
            </p>

            <div className="mt-4 grid gap-3 md:grid-cols-3">
              <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "优先文档数" : "Preferred docs"}
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {
                    recommendation.knowledgeBasis.preference
                      .preferredDocumentIds.length
                  }
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "固定作用域" : "Fixed scope"}
                </p>
                <p className="mt-1 text-sm font-semibold text-slate-950">
                  {recommendation.knowledgeBasis.preference.preferredScope
                    ? knowledgeScopeLabel(
                        recommendation.knowledgeBasis.preference.preferredScope,
                        locale,
                      )
                    : locale === "zh"
                      ? "未固定"
                      : "Not fixed"}
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "引用资料" : "Referenced docs"}
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {
                    recommendation.knowledgeBasis.referencedDocumentTitles
                      .length
                  }
                </p>
              </div>
            </div>

            {recommendation.knowledgeBasis.documents.length > 0 ? (
              <div className="mt-5 grid gap-3 lg:grid-cols-2">
                {recommendation.knowledgeBasis.documents.map((document) => (
                  <article
                    className="rounded-md border border-slate-200 bg-slate-50 p-4"
                    key={`${document.documentId ?? document.title}`}
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="inline-flex h-7 items-center rounded-md bg-white px-2 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">
                        {knowledgeScopeLabel(document.scope, locale)}
                      </span>
                      <span className="inline-flex h-7 items-center rounded-md bg-white px-2 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">
                        {knowledgeSourceCategoryLabel(
                          document.sourceCategory as
                            | "NOTE"
                            | "RESUME"
                            | "PROJECT"
                            | "COURSE"
                            | "REFERENCE"
                            | "OTHER",
                          locale,
                        )}
                      </span>
                    </div>
                    <h4 className="mt-3 text-sm font-semibold text-slate-950">
                      {document.title}
                    </h4>
                    <ul className="mt-3 space-y-1 text-sm text-slate-600">
                      {document.reasons.map((reason) => (
                        <li key={reason}>{reason}</li>
                      ))}
                    </ul>
                  </article>
                ))}
              </div>
            ) : null}

            {recommendation.knowledgeBasis.knowledgeEvidence.length > 0 ? (
              <div className="mt-5 rounded-md border border-slate-200 bg-slate-50 p-4">
                <div className="text-sm font-medium text-slate-500">
                  {locale === "zh" ? "知识证据片段" : "Knowledge evidence"}
                </div>
                <ul className="mt-3 space-y-2 text-sm leading-6 text-slate-600">
                  {recommendation.knowledgeBasis.knowledgeEvidence.map(
                    (item) => (
                      <li key={item}>{item}</li>
                    ),
                  )}
                </ul>
              </div>
            ) : null}
          </section>

          <section className="rounded-md border border-slate-200 p-5">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-950">
              <Rocket aria-hidden="true" className="size-4 text-slate-500" />
              {locale === "zh" ? "成果证明" : "Evidence of Progress"}
            </div>
            <div className="mt-4 flex flex-wrap gap-2">
              {recommendation.finalDeliverables.map((deliverable) => (
                <span
                  className="inline-flex h-8 items-center rounded-md bg-slate-100 px-3 text-sm font-medium text-slate-700"
                  key={deliverable}
                >
                  {deliverable}
                </span>
              ))}
            </div>
            <p className="mt-4 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "基于当前路径生成计划时，系统还会继续参考长期画像、最近反馈和已启用知识库文档。"
                : "When generating a plan from this path, the system also uses the long-term profile, recent feedback, and enabled knowledge-base documents."}
            </p>
          </section>

          {planError ? (
            <div className="rounded-md border border-rose-200 bg-rose-50 p-4">
              <p className="text-sm font-semibold text-rose-950">
                {locale === "zh"
                  ? "无法基于当前路径生成学习计划。"
                  : "Unable to generate a learning plan from the current path."}
              </p>
              <p className="mt-2 text-sm leading-6 text-rose-700">
                {planError}
              </p>
            </div>
          ) : null}
        </div>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          {locale === "zh"
            ? "先生成成长路径，让系统把当前定位、推荐主线、里程碑和下一步动作收敛成一个可执行方向。"
            : "Generate the growth path first so the system can consolidate the current position, recommended track, milestones, and next action into one executable direction."}
        </p>
      )}
    </section>
  );
}
