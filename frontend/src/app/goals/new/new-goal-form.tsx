"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { CalendarDays, Clock3, RotateCcw, Save, Target } from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

const goalFormSchema = z.object({
  technicalBackground: z
    .string()
    .trim()
    .min(10, "Describe your background in at least 10 characters."),
  learningGoal: z
    .string()
    .trim()
    .min(10, "Describe your learning goal in at least 10 characters."),
  jobTarget: z
    .string()
    .trim()
    .min(2, "Enter a target role or career direction."),
  dailyAvailableHours: z
    .number()
    .min(0.5, "Daily time must be at least 0.5 hours.")
    .max(12, "Daily time cannot exceed 12 hours."),
  planCycleDays: z.enum(["14", "21"]),
});

type GoalFormValues = z.infer<typeof goalFormSchema>;

const defaultValues: GoalFormValues = {
  technicalBackground: "",
  learningGoal: "",
  jobTarget: "",
  dailyAvailableHours: 2,
  planCycleDays: "14",
};

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }

  return <p className="mt-2 text-sm font-medium text-rose-600">{message}</p>;
}

export function NewGoalForm() {
  const [submittedDraft, setSubmittedDraft] = useState<GoalFormValues | null>(
    null,
  );

  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
    reset,
    control,
  } = useForm<GoalFormValues>({
    defaultValues,
    resolver: zodResolver(goalFormSchema),
  });

  const watchedValues = useWatch({ control });

  const summaryItems = [
    {
      label: "Daily time",
      value: `${watchedValues.dailyAvailableHours || 0} hours`,
      icon: Clock3,
    },
    {
      label: "Plan cycle",
      value: `${watchedValues.planCycleDays || defaultValues.planCycleDays} days`,
      icon: CalendarDays,
    },
    {
      label: "Target",
      value: watchedValues.jobTarget || "Not set",
      icon: Target,
    },
  ];

  function handleReset() {
    setSubmittedDraft(null);
    reset(defaultValues);
  }

  function onSubmit(values: GoalFormValues) {
    setSubmittedDraft(values);
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
              Technical Background
            </label>
            <textarea
              className="mt-2 min-h-32 w-full resize-y rounded-md border border-slate-300 bg-white px-3 py-2 text-sm leading-6 text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
              id="technicalBackground"
              placeholder="Backend developer with Java and PostgreSQL experience..."
              {...register("technicalBackground")}
            />
            <FieldError message={errors.technicalBackground?.message} />
          </div>

          <div>
            <label
              className="text-sm font-semibold text-slate-950"
              htmlFor="learningGoal"
            >
              Learning Goal
            </label>
            <textarea
              className="mt-2 min-h-32 w-full resize-y rounded-md border border-slate-300 bg-white px-3 py-2 text-sm leading-6 text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
              id="learningGoal"
              placeholder="Build AI agent applications and understand production workflows..."
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
                Job Target
              </label>
              <input
                className="mt-2 h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none transition-colors placeholder:text-slate-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                id="jobTarget"
                placeholder="AI Engineer"
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
                Daily Hours
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
                Plan Cycle
              </label>
              <select
                className="mt-2 h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 outline-none transition-colors focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                id="planCycleDays"
                {...register("planCycleDays")}
              >
                <option value="14">14 days</option>
                <option value="21">21 days</option>
              </select>
              <FieldError message={errors.planCycleDays?.message} />
            </div>
          </div>
        </div>

        <div className="mt-6 flex flex-col gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end">
          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
            onClick={handleReset}
            type="button"
          >
            <RotateCcw aria-hidden="true" className="size-4" />
            Reset
          </button>
          <button
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isSubmitting}
            type="submit"
          >
            <Save aria-hidden="true" className="size-4" />
            Save Draft
          </button>
        </div>
      </form>

      <aside className="flex flex-col gap-6">
        <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-base font-semibold text-slate-950">
            Goal Summary
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
          <h2 className="text-base font-semibold">Draft Status</h2>
          {submittedDraft ? (
            <div className="mt-4 space-y-3 text-sm leading-6 text-slate-300">
              <p className="font-medium text-emerald-300">
                Draft passed frontend validation.
              </p>
              <p>
                {submittedDraft.jobTarget} · {submittedDraft.planCycleDays} days
                · {submittedDraft.dailyAvailableHours} hours/day
              </p>
            </div>
          ) : (
            <p className="mt-4 text-sm leading-6 text-slate-300">
              No validated draft yet.
            </p>
          )}
        </section>
      </aside>
    </div>
  );
}
