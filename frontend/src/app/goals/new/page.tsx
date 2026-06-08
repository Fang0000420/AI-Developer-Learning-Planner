import Link from "next/link";
import { ArrowLeft, Sparkles } from "lucide-react";
import { NewGoalForm } from "./new-goal-form";

export default function NewGoalPage() {
  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <Link
              className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
              href="/"
            >
              <ArrowLeft aria-hidden="true" className="size-4" />
              Dashboard
            </Link>
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-emerald-50 px-3 text-sm font-medium text-emerald-700">
              <Sparkles aria-hidden="true" className="size-4" />
              Goal intake
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              New Learning Goal
            </h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
              Capture the first planning inputs for the MVP profile analysis and
              plan generation flow.
            </p>
          </div>

          <div className="rounded-md border border-slate-200 bg-white px-4 py-3 shadow-sm">
            <p className="text-sm font-medium text-slate-500">Status</p>
            <p className="mt-1 text-sm font-semibold text-slate-950">
              Frontend draft only
            </p>
          </div>
        </div>

        <NewGoalForm />
      </section>
    </main>
  );
}
