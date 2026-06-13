import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, test, vi } from "vitest";
import type { LearningPlan, PlanDay } from "@/lib/goals";
import TodayTasksPage from "./page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    refresh: vi.fn(),
  }),
}));

vi.mock("@/lib/backend-plans", () => ({
  fetchBackendPlan: vi.fn(),
  fetchBackendPlanDayTasks: vi.fn(),
}));

vi.mock("@/lib/backend-progress", () => ({
  fetchBackendAdaptiveScheduleControl: vi.fn(),
  fetchBackendProgressLogs: vi.fn(),
}));

vi.mock("@/lib/i18n-server", () => {
  return {
    getCurrentLocale: vi.fn(async () => "en"),
  };
});

import {
  fetchBackendPlan,
  fetchBackendPlanDayTasks,
} from "@/lib/backend-plans";
import {
  fetchBackendAdaptiveScheduleControl,
  fetchBackendProgressLogs,
} from "@/lib/backend-progress";

const mockedFetchBackendPlan = vi.mocked(fetchBackendPlan);
const mockedFetchBackendPlanDayTasks = vi.mocked(fetchBackendPlanDayTasks);
const mockedFetchBackendAdaptiveScheduleControl = vi.mocked(
  fetchBackendAdaptiveScheduleControl,
);
const mockedFetchBackendProgressLogs = vi.mocked(fetchBackendProgressLogs);

const plan: LearningPlan = {
  createdAt: "2026-06-10T10:00:00",
  days: [],
  durationDays: 14,
  goalId: 10,
  id: 30,
  planTitle: "14-Day AI Planner MVP Plan",
  knowledgeBasis: {
    summary: "This plan references work notes and enabled knowledge snippets.",
    preference: {
      goalId: 10,
      preferredDocumentIds: [],
      preferredScope: null,
      preferredCategories: [],
    },
    referencedDocumentTitles: [],
    knowledgeEvidence: [],
    documents: [],
  },
  sourceAgentRunId: 90,
  status: "ACTIVE",
  updatedAt: "2026-06-10T10:00:00",
  userId: 1,
  versions: [],
};

const day: PlanDay = {
  dayIndex: 1,
  tasks: [
    {
      dayIndex: 1,
      deliverable: "Architecture notes",
      description: "Understand the service boundaries.",
      estimatedMinutes: 30,
      id: 1,
      priority: "high",
      status: "DONE",
      taskOrder: 1,
      title: "Review architecture",
      type: "learning",
    },
    {
      dayIndex: 1,
      deliverable: "Working endpoint",
      description: "Implement the backend endpoint.",
      estimatedMinutes: 60,
      id: 2,
      priority: "medium",
      status: "PENDING",
      taskOrder: 2,
      title: "Create plan endpoint",
      type: "build",
    },
  ],
  theme: "Foundation setup",
  totalEstimatedMinutes: 90,
};

async function renderPage(dayIndex = "1") {
  render(
    await TodayTasksPage({
      params: Promise.resolve({ planId: "30" }),
      searchParams: Promise.resolve({ dayIndex }),
    }),
  );
}

describe("TodayTasksPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedFetchBackendPlan.mockResolvedValue({ data: plan, error: null });
    mockedFetchBackendPlanDayTasks.mockResolvedValue({
      data: day,
      error: null,
    });
    mockedFetchBackendAdaptiveScheduleControl.mockResolvedValue({
      data: {},
      error: null,
    });
    mockedFetchBackendProgressLogs.mockResolvedValue({ data: [], error: null });
  });

  test("renders daily task summary and task cards", async () => {
    await renderPage();

    expect(
      screen.getByRole("heading", { name: "Foundation setup" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Day 1")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("1 hr 30 min")).toBeInTheDocument();
    expect(screen.getByText("1/2")).toBeInTheDocument();
    expect(screen.getAllByText("Review architecture").length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText("Create plan endpoint").length).toBeGreaterThan(
      0,
    );
    expect(mockedFetchBackendPlanDayTasks).toHaveBeenCalledWith("30", 1);
  });

  test("renders an API error state when tasks cannot load", async () => {
    mockedFetchBackendPlanDayTasks.mockResolvedValue({
      data: null,
      error: {
        message: "Backend daily tasks request failed.",
        status: "BAD_GATEWAY",
      },
    });

    await renderPage("2");

    expect(
      screen.getByRole("heading", { name: "Tasks unavailable" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Backend daily tasks request failed."),
    ).toBeInTheDocument();
  });
});
