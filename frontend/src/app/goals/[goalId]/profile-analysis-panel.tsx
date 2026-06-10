"use client";

import { useState } from "react";
import {
  AlertCircle,
  Brain,
  CheckCircle2,
  LoaderCircle,
  Sparkles,
} from "lucide-react";
import { useRouter } from "next/navigation";
import type { ApiErrorResponse, SkillProfile } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type ProfileAnalysisPanelProps = {
  goalId: number;
  initialError?: string | null;
  initialProfile: SkillProfile | null;
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
    (locale === "zh" ? "能力画像生成失败。" : "Profile analysis failed.")
  );
}

function ProfileList({ items, locale }: { items: string[]; locale: Locale }) {
  if (items.length === 0) {
    return (
      <p className="text-sm leading-6 text-slate-500">
        {locale === "zh" ? "未返回。" : "Not returned."}
      </p>
    );
  }

  return (
    <ul className="space-y-2">
      {items.map((item) => (
        <li className="text-sm leading-6 text-slate-600" key={item}>
          {item}
        </li>
      ))}
    </ul>
  );
}

export function ProfileAnalysisPanel({
  goalId,
  initialError = null,
  initialProfile,
  locale,
}: ProfileAnalysisPanelProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(initialError);
  const [isGenerating, setIsGenerating] = useState(false);
  const [profile, setProfile] = useState<SkillProfile | null>(initialProfile);

  async function handleAnalyze() {
    setError(null);
    setIsGenerating(true);

    try {
      const response = await fetch(`/api/goals/${goalId}/profile/analyze`, {
        method: "POST",
      });
      const payload = (await response.json()) as
        | SkillProfile
        | ApiErrorResponse;

      if (!response.ok) {
        setError(getErrorMessage(payload as ApiErrorResponse, locale));
        return;
      }

      setProfile(payload as SkillProfile);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "能力画像生成失败。"
            : "Profile analysis failed.",
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
      : profile
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
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-violet-50 text-violet-700">
            <Brain aria-hidden="true" className="size-4" />
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold text-slate-950">
                {locale === "zh" ? "能力画像" : "Skill Profile"}
              </h2>
              <span className="inline-flex h-7 items-center gap-1 rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                {isGenerating ? (
                  <LoaderCircle
                    aria-hidden="true"
                    className="size-3.5 animate-spin"
                  />
                ) : profile && !error ? (
                  <CheckCircle2 aria-hidden="true" className="size-3.5" />
                ) : error ? (
                  <AlertCircle aria-hidden="true" className="size-3.5" />
                ) : null}
                {statusLabel}
              </span>
            </div>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "根据已保存的背景、目标和每日时间预算生成结构化画像。"
                : "Structured profile generated from the saved background, goal, and daily time budget."}
            </p>
          </div>
        </div>

        <button
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          disabled={isGenerating}
          onClick={handleAnalyze}
          type="button"
        >
          {isGenerating ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Sparkles aria-hidden="true" className="size-4" />
          )}
          {profile
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
            {locale === "zh"
              ? "无法生成能力画像。"
              : "Unable to generate profile."}
          </p>
          <p className="mt-2 text-sm leading-6 text-rose-700">{error}</p>
        </div>
      ) : null}

      {profile ? (
        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
            <h3 className="text-sm font-semibold text-slate-950">
              {locale === "zh" ? "当前技能" : "Current Skills"}
            </h3>
            <div className="mt-3">
              <ProfileList items={profile.currentSkills} locale={locale} />
            </div>
          </div>
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
            <h3 className="text-sm font-semibold text-slate-950">
              {locale === "zh" ? "优势" : "Strengths"}
            </h3>
            <div className="mt-3">
              <ProfileList items={profile.strengths} locale={locale} />
            </div>
          </div>
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
            <h3 className="text-sm font-semibold text-slate-950">
              {locale === "zh" ? "短板" : "Weaknesses"}
            </h3>
            <div className="mt-3">
              <ProfileList items={profile.weaknesses} locale={locale} />
            </div>
          </div>
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4 lg:col-span-3">
            <h3 className="text-sm font-semibold text-slate-950">
              {locale === "zh" ? "推荐方向" : "Recommended Direction"}
            </h3>
            <p className="mt-3 text-sm leading-7 text-slate-600">
              {profile.recommendedDirection}
            </p>
          </div>
        </div>
      ) : (
        <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          {locale === "zh"
            ? "生成能力画像后，会为该目标保存结构化画像和 Agent 运行记录。"
            : "Generate a profile to save structured profile and agent run records for this goal."}
        </p>
      )}
    </section>
  );
}
