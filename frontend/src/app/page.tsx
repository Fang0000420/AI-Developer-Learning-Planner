import Link from "next/link";
import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Clock3,
  FolderKanban,
  Plus,
  Sparkles,
  Target,
} from "lucide-react";
import { BackendHealthCard } from "./backend-health-card";

const statusCards = [
  {
    label: "Active goal",
    value: "Draft ready",
    detail: "Goal intake page is ready for frontend validation.",
    icon: Target,
  },
  {
    label: "Plan window",
    value: "14 or 21 days",
    detail: "Daily task generation arrives after agent workflows.",
    icon: CalendarDays,
  },
  {
    label: "Today",
    value: "No tasks yet",
    detail: "Daily progress starts after the first plan exists.",
    icon: Clock3,
  },
];

const quickActions = [
  {
    title: "Create Goal",
    description:
      "Capture background, target role, time budget, and plan cycle.",
    href: "/goals/new",
    icon: Plus,
  },
  {
    title: "View Plan",
    description: "Review the generated learning roadmap and project direction.",
    href: "/plans",
    icon: FolderKanban,
  },
  {
    title: "Today Tasks",
    description: "Work through daily tasks and prepare progress notes.",
    href: "/tasks/today",
    icon: ClipboardList,
  },
];

const workflowSteps = [
  { label: "Profile analysis", state: "Ready" },
  { label: "Goal decomposition", state: "Planned" },
  { label: "Skill gap review", state: "Planned" },
  { label: "Daily plan", state: "Planned" },
];

export default function Home() {
  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-6 sm:px-6 lg:grid-cols-[1fr_360px] lg:px-8">
        <div className="flex flex-col gap-6">
          <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
              <div className="max-w-3xl">
                <p className="inline-flex h-8 items-center gap-2 rounded-md bg-emerald-50 px-3 text-sm font-medium text-emerald-700">
                  <Sparkles aria-hidden="true" className="size-4" />
                  Day 04 workspace
                </p>
                <h1 className="mt-4 text-3xl font-semibold text-slate-950">
                  Build an adaptive AI learning plan
                </h1>
                <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
                  Use this workspace to create a learning goal, inspect the
                  generated plan, and track daily execution once the MVP flow is
                  connected.
                </p>
              </div>

              <Link
                className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800"
                href="/goals/new"
              >
                <Plus aria-hidden="true" className="size-4" />
                New Goal
              </Link>
            </div>

            <div className="mt-8 grid gap-4 md:grid-cols-3">
              {statusCards.map((card) => {
                const Icon = card.icon;

                return (
                  <article
                    className="min-h-40 rounded-md border border-slate-200 bg-slate-50 p-5"
                    key={card.label}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-sm font-medium text-slate-500">
                        {card.label}
                      </span>
                      <Icon
                        aria-hidden="true"
                        className="size-5 text-teal-600"
                      />
                    </div>
                    <p className="mt-4 text-2xl font-semibold text-slate-950">
                      {card.value}
                    </p>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                      {card.detail}
                    </p>
                  </article>
                );
              })}
            </div>
          </section>

          <section aria-labelledby="quick-actions-title">
            <div className="mb-3 flex items-center justify-between gap-4">
              <h2
                className="text-lg font-semibold text-slate-950"
                id="quick-actions-title"
              >
                Workspace Entry Points
              </h2>
              <span className="text-sm text-slate-500">MVP flow</span>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              {quickActions.map((action) => {
                const Icon = action.icon;

                return (
                  <Link
                    className="group min-h-44 rounded-md border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-teal-300 hover:bg-teal-50"
                    href={action.href}
                    key={action.title}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span className="flex size-10 items-center justify-center rounded-md bg-slate-100 text-slate-700 group-hover:bg-white group-hover:text-teal-700">
                        <Icon aria-hidden="true" className="size-5" />
                      </span>
                      <ArrowRight
                        aria-hidden="true"
                        className="size-4 text-slate-400 transition-transform group-hover:translate-x-1 group-hover:text-teal-700"
                      />
                    </div>
                    <h3 className="mt-5 text-base font-semibold text-slate-950">
                      {action.title}
                    </h3>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                      {action.description}
                    </p>
                  </Link>
                );
              })}
            </div>
          </section>
        </div>

        <aside className="flex flex-col gap-6">
          <BackendHealthCard />

          <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-base font-semibold text-slate-950">
                MVP Progress
              </h2>
              <CheckCircle2
                aria-hidden="true"
                className="size-5 text-teal-600"
              />
            </div>
            <div className="mt-5 space-y-4">
              {workflowSteps.map((step, index) => (
                <div className="flex gap-3" key={step.label}>
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-slate-100 text-sm font-semibold text-slate-700">
                    {index + 1}
                  </span>
                  <div>
                    <p className="text-sm font-medium text-slate-950">
                      {step.label}
                    </p>
                    <p className="mt-1 text-sm text-slate-500">{step.state}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-md border border-slate-200 bg-slate-950 p-5 text-white shadow-sm">
            <h2 className="text-base font-semibold">Next Step</h2>
            <p className="mt-3 text-sm leading-6 text-slate-300">
              Backend health is now available from the workspace status panel.
              Day 05 will continue with goal management.
            </p>
            <Link
              className="mt-5 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-white px-3 text-sm font-semibold text-slate-950 transition-colors hover:bg-slate-200"
              href="/goals/new"
            >
              <Plus aria-hidden="true" className="size-4" />
              Open Goal Form
            </Link>
          </section>
        </aside>
      </section>
    </main>
  );
}
