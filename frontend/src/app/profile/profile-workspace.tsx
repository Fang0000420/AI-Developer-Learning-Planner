"use client";

import Link from "next/link";
import { useState } from "react";
import {
  Brain,
  CheckCircle2,
  Compass,
  LoaderCircle,
  PencilLine,
  Target,
} from "lucide-react";
import { useRouter } from "next/navigation";
import type { ApiErrorResponse, UserProfile } from "@/lib/goals";
import { formatGoalDate } from "@/lib/goals";
import type { Locale } from "@/lib/i18n";

type ProfileWorkspaceProps = {
  initialError?: string | null;
  initialProfile: UserProfile | null;
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
    (locale === "zh" ? "画像更新失败。" : "Profile update failed.")
  );
}

function ChipList({
  items,
  emptyLabel,
}: {
  items: string[];
  emptyLabel: string;
}) {
  if (items.length === 0) {
    return <p className="text-sm leading-6 text-slate-500">{emptyLabel}</p>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <span
          className="inline-flex h-8 items-center rounded-md bg-slate-100 px-3 text-sm font-medium text-slate-700"
          key={item}
        >
          {item}
        </span>
      ))}
    </div>
  );
}

export function ProfileWorkspace({
  initialError = null,
  initialProfile,
  locale,
}: ProfileWorkspaceProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(initialError);
  const [profile, setProfile] = useState<UserProfile | null>(initialProfile);
  const [preferredLearningStyle, setPreferredLearningStyle] = useState(
    initialProfile?.preferredLearningStyle ?? "",
  );
  const [pacePreference, setPacePreference] = useState(
    initialProfile?.pacePreference ?? "",
  );
  const [timeBudgetNote, setTimeBudgetNote] = useState(
    initialProfile?.timeBudgetNote ?? "",
  );
  const [manualCorrection, setManualCorrection] = useState(
    initialProfile?.manualCorrection ?? "",
  );
  const [isSaving, setIsSaving] = useState(false);

  async function handleSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSaving(true);

    try {
      const response = await fetch("/api/profile", {
        body: JSON.stringify({
          preferredLearningStyle,
          pacePreference,
          timeBudgetNote,
          manualCorrection,
        }),
        headers: { "Content-Type": "application/json" },
        method: "PATCH",
      });
      const payload = (await response.json()) as UserProfile | ApiErrorResponse;
      if (!response.ok) {
        setError(getErrorMessage(payload as ApiErrorResponse, locale));
        return;
      }
      const updated = payload as UserProfile;
      setProfile(updated);
      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "画像更新失败。"
            : "Profile update failed.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  if (!profile) {
    return (
      <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-violet-50 text-violet-700">
            <Brain aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              {locale === "zh" ? "长期画像" : "Long-term Profile"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "还没有长期画像。先在任意目标页生成一次能力画像，系统就会开始沉淀用户画像版本与目标快照。"
                : "There is no long-term profile yet. Generate a profile from any goal page to start storing user profile versions and goal snapshots."}
            </p>
            <Link
              className="mt-4 inline-flex h-10 items-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800"
              href="/goals"
            >
              <Target aria-hidden="true" className="size-4" />
              {locale === "zh" ? "打开目标" : "Open Goals"}
            </Link>
          </div>
        </div>
        {error ? (
          <p className="mt-4 text-sm leading-6 text-rose-700">{error}</p>
        ) : null}
      </section>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <div className="space-y-6">
        <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-start gap-3">
            <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-violet-50 text-violet-700">
              <Brain aria-hidden="true" className="size-4" />
            </span>
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-lg font-semibold text-slate-950">
                  {locale === "zh" ? "长期画像" : "Long-term Profile"}
                </h2>
                <span className="inline-flex h-7 items-center gap-1 rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                  <CheckCircle2 aria-hidden="true" className="size-3.5" />
                  {locale === "zh"
                    ? `版本 ${profile.currentVersion}`
                    : `Version ${profile.currentVersion}`}
                </span>
              </div>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {profile.profileSummary}
              </p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "推荐学习方式" : "Preferred Learning Style"}
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {profile.preferredLearningStyle ||
                  (locale === "zh" ? "未记录" : "Not recorded")}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "节奏偏好" : "Pace Preference"}
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {profile.pacePreference ||
                  (locale === "zh" ? "未记录" : "Not recorded")}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4 md:col-span-2">
              <p className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "时间预算说明" : "Time Budget Note"}
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {profile.timeBudgetNote ||
                  (locale === "zh" ? "未记录" : "Not recorded")}
              </p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 xl:grid-cols-2">
            <div className="rounded-md border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "当前基础" : "Current Skills"}
              </h3>
              <div className="mt-3">
                <ChipList
                  emptyLabel={locale === "zh" ? "未记录。" : "Not recorded."}
                  items={profile.currentSkills}
                />
              </div>
            </div>
            <div className="rounded-md border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "重点方向" : "Focus Areas"}
              </h3>
              <div className="mt-3">
                <ChipList
                  emptyLabel={locale === "zh" ? "未记录。" : "Not recorded."}
                  items={profile.focusAreas}
                />
              </div>
            </div>
            <div className="rounded-md border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "优势" : "Strengths"}
              </h3>
              <div className="mt-3">
                <ChipList
                  emptyLabel={locale === "zh" ? "未记录。" : "Not recorded."}
                  items={profile.strengths}
                />
              </div>
            </div>
            <div className="rounded-md border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-950">
                {locale === "zh" ? "风险信号" : "Risk Signals"}
              </h3>
              <div className="mt-3">
                <ChipList
                  emptyLabel={locale === "zh" ? "未记录。" : "Not recorded."}
                  items={profile.riskSignals}
                />
              </div>
            </div>
          </div>

          <div className="mt-6 rounded-md border border-slate-200 p-4">
            <h3 className="text-sm font-semibold text-slate-950">
              {locale === "zh" ? "画像证据" : "Profile Evidence"}
            </h3>
            <div className="mt-3">
              <ChipList
                emptyLabel={locale === "zh" ? "未记录。" : "Not recorded."}
                items={profile.evidence}
              />
            </div>
          </div>
        </section>

        <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-start gap-3">
            <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-sky-50 text-sky-700">
              <Compass aria-hidden="true" className="size-4" />
            </span>
            <div>
              <h2 className="text-lg font-semibold text-slate-950">
                {locale === "zh" ? "最近目标快照" : "Recent Goal Snapshots"}
              </h2>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {locale === "zh"
                  ? "每次在目标页生成画像后，系统会同步保存一份目标快照，用来追踪你在不同目标下的判断差异。"
                  : "Every time a goal profile is generated, the system stores a goal snapshot so you can compare profile judgments across goals."}
              </p>
            </div>
          </div>

          <div className="mt-6 space-y-4">
            {profile.recentSnapshots.map((snapshot) => (
              <article
                className="rounded-md border border-slate-200 p-4"
                key={snapshot.id}
              >
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-base font-semibold text-slate-950">
                        {snapshot.goalTitle}
                      </h3>
                      <span className="inline-flex h-7 items-center rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                        {locale === "zh"
                          ? `版本 ${snapshot.version}`
                          : `Version ${snapshot.version}`}
                      </span>
                    </div>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                      {snapshot.summary}
                    </p>
                  </div>
                  <Link
                    className="inline-flex h-10 items-center gap-2 rounded-md border border-slate-300 px-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                    href={`/goals/${snapshot.goalId}`}
                  >
                    <Target aria-hidden="true" className="size-4" />
                    {locale === "zh" ? "打开目标" : "Open Goal"}
                  </Link>
                </div>
                <p className="mt-3 text-xs text-slate-500">
                  {formatGoalDate(snapshot.updatedAt, locale)}
                </p>
              </article>
            ))}
          </div>
        </section>
      </div>

      <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-amber-50 text-amber-700">
            <PencilLine aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              {locale === "zh" ? "手动纠偏" : "Manual Corrections"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "如果系统对你的学习方式、节奏或时间判断有偏差，可以在这里修正。修正内容会进入下一版长期画像。"
                : "If the system misreads your learning style, pace, or time budget, correct it here. The correction becomes part of the next long-term profile version."}
            </p>
          </div>
        </div>

        <form className="mt-6 space-y-4" onSubmit={handleSave}>
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              {locale === "zh" ? "更适合的学习方式" : "Better Learning Style"}
            </label>
            <input
              className="h-11 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
              onChange={(event) => setPreferredLearningStyle(event.target.value)}
              placeholder={locale === "zh" ? "例如：项目驱动" : "For example: project-driven"}
              value={preferredLearningStyle}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              {locale === "zh" ? "更合适的节奏" : "Better Pace"}
            </label>
            <input
              className="h-11 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
              onChange={(event) => setPacePreference(event.target.value)}
              placeholder={locale === "zh" ? "例如：稳步推进" : "For example: steady pace"}
              value={pacePreference}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              {locale === "zh" ? "时间预算说明" : "Time Budget Note"}
            </label>
            <textarea
              className="min-h-24 w-full rounded-md border border-slate-300 px-3 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
              onChange={(event) => setTimeBudgetNote(event.target.value)}
              placeholder={
                locale === "zh"
                  ? "例如：工作日 1 小时，周末 3 小时。"
                  : "For example: 1 hour on weekdays and 3 hours on weekends."
              }
              value={timeBudgetNote}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              {locale === "zh" ? "你想纠正的判断" : "What should be corrected"}
            </label>
            <textarea
              className="min-h-32 w-full rounded-md border border-slate-300 px-3 py-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
              onChange={(event) => setManualCorrection(event.target.value)}
              placeholder={
                locale === "zh"
                  ? "例如：我更适合先从小项目切入，而不是先读大量资料。"
                  : "For example: I learn better from small projects than from long reading sessions."
              }
              value={manualCorrection}
            />
          </div>

          {error ? (
            <div className="rounded-md border border-rose-200 bg-rose-50 p-4 text-sm leading-6 text-rose-700">
              {error}
            </div>
          ) : null}

          <button
            className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isSaving}
            type="submit"
          >
            {isSaving ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : (
              <PencilLine aria-hidden="true" className="size-4" />
            )}
            {isSaving
              ? locale === "zh"
                ? "保存中"
                : "Saving"
              : locale === "zh"
                ? "保存到长期画像"
                : "Save to Long-term Profile"}
          </button>
        </form>
      </section>
    </div>
  );
}
