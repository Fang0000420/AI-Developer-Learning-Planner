"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { CalendarDays, Clock3, RotateCcw, Save, Target } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import type {
  ApiErrorResponse,
  Goal,
  GoalCreatePayload,
  ResponseLanguage,
} from "@/lib/goals";
import { dictionaries, responseLanguageLabel, type Locale } from "@/lib/i18n";

function createGoalFormSchema(locale: Locale) {
  const t = dictionaries[locale].newGoal.validation;
  return z.object({
    technicalBackground: z
      .string()
      .trim()
      .min(10, t.technicalBackground)
      .max(3000, t.technicalBackgroundMax),
    learningGoal: z
      .string()
      .trim()
      .min(10, t.learningGoal)
      .max(255, t.learningGoalMax),
    jobTarget: z.string().trim().min(2, t.jobTarget).max(120, t.jobTargetMax),
    dailyAvailableHours: z.number().min(0.5, t.dailyMin).max(12, t.dailyMax),
    planCycleDays: z
      .number()
      .int(t.planCycleInt)
      .min(7, t.planCycleMin)
      .max(60, t.planCycleMax),
    responseLanguage: z.enum(["zh", "en"]),
  });
}

type GoalFormValues = z.infer<ReturnType<typeof createGoalFormSchema>>;

function defaultValues(locale: Locale): GoalFormValues {
  return {
    dailyAvailableHours: 2,
    jobTarget: "",
    learningGoal: "",
    planCycleDays: 14,
    responseLanguage: locale,
    technicalBackground: "",
  };
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }

  return <p className="mt-2 text-sm font-medium text-rose-600">{message}</p>;
}

function composeGoalPayload(
  values: GoalFormValues,
  locale: Locale,
): GoalCreatePayload {
  return {
    dailyAvailableHours: values.dailyAvailableHours,
    description: [
      `${locale === "zh" ? "目标方向" : "Job target"}: ${values.jobTarget.trim()}`,
    ].join("\n\n"),
    durationDays: values.planCycleDays,
    responseLanguage: values.responseLanguage,
    technicalBackground: values.technicalBackground.trim(),
    title: values.learningGoal.trim(),
  };
}

function getErrorMessage(error: ApiErrorResponse, fallback: string) {
  if (error.errors) {
    const firstFieldError = Object.values(error.errors)[0];
    if (firstFieldError) {
      return firstFieldError;
    }
  }

  return error.message || fallback;
}

type NewGoalFormProps = {
  locale: Locale;
};

export function NewGoalForm({ locale }: NewGoalFormProps) {
  const t = dictionaries[locale];
  const router = useRouter();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const initialValues = defaultValues(locale);

  const {
    control,
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
    reset,
  } = useForm<GoalFormValues>({
    defaultValues: initialValues,
    resolver: zodResolver(createGoalFormSchema(locale)),
  });

  const watchedDailyHours = useWatch({ control, name: "dailyAvailableHours" });
  const watchedPlanCycleDays = useWatch({ control, name: "planCycleDays" });
  const watchedJobTarget = useWatch({ control, name: "jobTarget" });
  const watchedResponseLanguage = useWatch({ control, name: "responseLanguage" });

  const summaryItems = [
    {
      icon: Clock3,
      label: locale === "zh" ? "每日时间" : "Daily time",
      value: `${watchedDailyHours || 0} ${t.common.hours}`,
    },
    {
      icon: CalendarDays,
      label: locale === "zh" ? "计划周期" : "Plan cycle",
      value: `${watchedPlanCycleDays || initialValues.planCycleDays} ${t.common.days}`,
    },
    {
      icon: Target,
      label: locale === "zh" ? "目标方向" : "Target",
      value: watchedJobTarget || t.common.notSet,
    },
    {
      icon: Target,
      label: t.common.responseLanguage,
      value: responseLanguageLabel(
        (watchedResponseLanguage || locale) as ResponseLanguage,
        locale,
      ),
    },
  ];

  function handleReset() {
    setSubmitError(null);
    reset(initialValues);
  }

  async function onSubmit(values: GoalFormValues) {
    setSubmitError(null);

    const response = await fetch("/api/goals", {
      body: JSON.stringify(composeGoalPayload(values, locale)),
      headers: {
        "content-type": "application/json",
      },
      method: "POST",
    });

    const payload = (await response.json()) as Goal | ApiErrorResponse;

    if (!response.ok) {
      setSubmitError(
        getErrorMessage(payload as ApiErrorResponse, t.goals.createFailed),
      );
      return;
    }

    const goal = payload as Goal;
    router.push(`/goals/${goal.id}`);
    router.refresh();
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
      <form
        className="rounded-md border border-slate-200 bg-white p-6 shadow-sm"
        noValidate
        onSubmit={handleSubmit(onSubmit)}
      >
        <div className="grid gap-5">
          <div>
            <label
              className="text-sm font-semibold text-slate-950"
              htmlFor="technicalBackground"
            >
              {t.newGoal.technicalBackground}
            </label>
            <textarea
              className="mt-2 min-h-32 w-full resize-y rounded-md border border-slate-300 bg-white px-3 py-2 text-sm leading-6 text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
              id="technicalBackground"
              placeholder={t.newGoal.technicalPlaceholder}
              {...register("technicalBackground")}
            />
            <FieldError message={errors.technicalBackground?.message} />
          </div>

          <div>
            <label
              className="text-sm font-semibold text-slate-950"
              htmlFor="learningGoal"
            >
              {t.newGoal.learningGoal}
            </label>
            <textarea
              className="mt-2 min-h-32 w-full resize-y rounded-md border border-slate-300 bg-white px-3 py-2 text-sm leading-6 text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
              id="learningGoal"
              placeholder={t.newGoal.learningPlaceholder}
              {...register("learningGoal")}
            />
            <FieldError message={errors.learningGoal?.message} />
          </div>

          <div className="grid gap-5 md:grid-cols-3">
            <div>
              <label
                className="text-sm font-semibold text-slate-950"
                htmlFor="jobTarget"
              >
                {t.newGoal.jobTarget}
              </label>
              <input
                className="mt-2 h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                id="jobTarget"
                placeholder={t.newGoal.jobPlaceholder}
                type="text"
                {...register("jobTarget")}
              />
              <FieldError message={errors.jobTarget?.message} />
            </div>

            <div>
              <label
                className="text-sm font-semibold text-slate-950"
                htmlFor="dailyAvailableHours"
              >
                {t.newGoal.dailyHours}
              </label>
              <input
                className="mt-2 h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                id="dailyAvailableHours"
                max="12"
                min="0.5"
                step="0.5"
                type="number"
                {...register("dailyAvailableHours", { valueAsNumber: true })}
              />
              <FieldError message={errors.dailyAvailableHours?.message} />
            </div>

            <div>
              <label
                className="text-sm font-semibold text-slate-950"
                htmlFor="planCycleDays"
              >
                {t.newGoal.planCycle}
              </label>
              <input
                className="mt-2 h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none transition-colors focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                id="planCycleDays"
                max="60"
                min="7"
                step="1"
                type="number"
                {...register("planCycleDays", { valueAsNumber: true })}
              />
              <FieldError message={errors.planCycleDays?.message} />
            </div>
          </div>

          <div>
            <label
              className="text-sm font-semibold text-slate-950"
              htmlFor="responseLanguage"
            >
              {t.common.responseLanguage}
            </label>
            <select
              className="mt-2 h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none transition-colors focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
              id="responseLanguage"
              {...register("responseLanguage")}
            >
              <option value="zh">{t.common.chinese}</option>
              <option value="en">{t.common.english}</option>
            </select>
            <FieldError message={errors.responseLanguage?.message} />
          </div>
        </div>

        <div className="mt-6 flex flex-col gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end">
          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
            onClick={handleReset}
            type="button"
          >
            <RotateCcw aria-hidden="true" className="size-4" />
            {t.common.reset}
          </button>
          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isSubmitting}
            type="submit"
          >
            <Save aria-hidden="true" className="size-4" />
            {isSubmitting ? t.newGoal.creating : t.newGoal.submit}
          </button>
        </div>
      </form>

      <aside className="flex flex-col gap-6">
        <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-base font-semibold text-slate-950">
            {t.newGoal.summary}
          </h2>
          <div className="mt-5 space-y-4">
            {summaryItems.map((item) => {
              const Icon = item.icon;

              return (
                <div className="flex items-start gap-3" key={item.label}>
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-teal-50 text-teal-700">
                    <Icon aria-hidden="true" className="size-4" />
                  </span>
                  <div>
                    <p className="text-sm font-medium text-slate-950">
                      {item.label}
                    </p>
                    <p className="mt-1 text-sm leading-6 text-slate-600">
                      {item.value}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section className="rounded-md border border-slate-200 bg-slate-950 p-5 text-white shadow-sm">
          <h2 className="text-base font-semibold">{t.newGoal.createStatus}</h2>
          {submitError ? (
            <div className="mt-4 space-y-3 text-sm leading-6 text-slate-300">
              <p className="font-medium text-rose-300">{t.newGoal.unable}</p>
              <p>{submitError}</p>
            </div>
          ) : (
            <p className="mt-4 text-sm leading-6 text-slate-300">
              {t.newGoal.validHint}
            </p>
          )}
        </section>
      </aside>
    </div>
  );
}
