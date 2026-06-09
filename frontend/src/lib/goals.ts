export type GoalStatus = "ACTIVE" | "COMPLETED" | "PAUSED" | "CANCELLED";
export type LearningPlanStatus = "ACTIVE" | "PAUSED";

export type Goal = {
  id: number;
  userId: number;
  title: string;
  description: string | null;
  durationDays: number;
  status: GoalStatus;
  dailyAvailableHours: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type SkillProfile = {
  id: number;
  userId: number;
  goalId: number;
  currentSkills: string[];
  strengths: string[];
  weaknesses: string[];
  recommendedDirection: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type Priority = "high" | "medium" | "low";

export type SubGoal = {
  title: string;
  description: string;
  priority: Priority;
};

export type GoalDecomposition = {
  runId: number;
  goalId: number;
  subGoals: SubGoal[];
  createdAt: string | null;
};

export type SkillGap = {
  skill: string;
  currentLevel: string;
  targetLevel: string;
  priority: Priority;
  reason: string;
};

export type SkillGapAnalysis = {
  runId: number;
  goalId: number;
  skillGaps: SkillGap[];
  createdAt: string | null;
};

export type ProjectRecommendation = {
  runId: number;
  goalId: number;
  recommendedProject: string;
  reason: string;
  difficulty: string;
  durationDays: number;
  dailyTimeHours: number;
  coreTechStack: string[];
  finalDeliverables: string[];
  createdAt: string | null;
};

export type DailyTaskStatus = "PENDING" | "IN_PROGRESS" | "DONE" | "SKIPPED";
export type ProgressImpact = "none" | "minor" | "medium" | "major";

export type PlanTask = {
  id: number;
  dayIndex: number;
  taskOrder: number;
  title: string;
  description: string;
  estimatedMinutes: number;
  type: string;
  deliverable: string;
  priority: Priority;
  status: DailyTaskStatus;
};

export type PlanDay = {
  dayIndex: number;
  theme: string;
  totalEstimatedMinutes: number;
  tasks: PlanTask[];
};

export type LearningPlan = {
  id: number;
  goalId: number;
  userId: number;
  sourceAgentRunId: number | null;
  planTitle: string;
  durationDays: number;
  status: LearningPlanStatus;
  days: PlanDay[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type LearningPlanSummary = {
  id: number;
  goalId: number;
  userId: number;
  planTitle: string;
  durationDays: number;
  status: LearningPlanStatus;
  dayCount: number;
  taskCount: number;
  totalEstimatedMinutes: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ProgressReviewResult = {
  completedTasks?: string[];
  unfinishedTasks?: string[];
  blockers?: string[];
  impact?: ProgressImpact;
  planAdjustment?: PlanAdjustmentResult;
  suggestion?: string;
};

export type PlanAdjustmentTask = {
  id?: number | null;
  dayIndex?: number | null;
  taskOrder?: number | null;
  title: string;
  description?: string;
  estimatedMinutes?: number;
  type?: string;
  deliverable?: string;
  priority?: Priority;
  status?: DailyTaskStatus;
};

export type PlanMovedTask = {
  taskId?: number | null;
  title: string;
  fromDayIndex: number;
  toDayIndex: number;
  reason: string;
};

export type PlanSplitTask = {
  sourceTaskId?: number | null;
  sourceTitle: string;
  parts: PlanAdjustmentTask[];
  reason: string;
};

export type PlanAdjustmentResult = {
  nextDayTasks?: PlanAdjustmentTask[];
  movedTasks?: PlanMovedTask[];
  splitTasks?: PlanSplitTask[];
  reason?: string;
};

export type ProgressLog = {
  id: number;
  planId: number;
  goalId: number;
  userId: number;
  dayIndex: number;
  userFeedback: string;
  completedTaskIds: number[];
  unfinishedTaskIds: number[];
  blockers: string[];
  reviewResultJson: ProgressReviewResult;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AsyncJobStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";
export type AsyncJobType = "PLAN_GENERATION" | "PROGRESS_SUBMISSION";

export type AsyncJob<T> = {
  jobId: string;
  jobType: AsyncJobType;
  status: AsyncJobStatus;
  result: T | null;
  errorMessage: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ApiErrorResponse = {
  status?: string;
  message?: string;
  errors?: Record<string, string>;
  timestamp?: string;
};

export type GoalCreatePayload = {
  technicalBackground: string;
  title: string;
  description: string;
  durationDays: number;
  dailyAvailableHours: number;
};

export function getGoalStatusLabel(status: GoalStatus) {
  const labels: Record<GoalStatus, string> = {
    ACTIVE: "Active",
    CANCELLED: "Cancelled",
    COMPLETED: "Completed",
    PAUSED: "Paused",
  };

  return labels[status];
}

export function getGoalStatusClasses(status: GoalStatus) {
  const classes: Record<GoalStatus, string> = {
    ACTIVE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    CANCELLED: "bg-rose-50 text-rose-700 ring-rose-200",
    COMPLETED: "bg-sky-50 text-sky-700 ring-sky-200",
    PAUSED: "bg-amber-50 text-amber-700 ring-amber-200",
  };

  return classes[status];
}

export function formatGoalDate(value: string | null) {
  if (!value) {
    return "Not recorded";
  }

  return new Intl.DateTimeFormat("en-US", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

export function formatDailyHours(value: number | null) {
  if (value === null) {
    return "Not set";
  }

  return `${value} hours`;
}

export function formatMinutes(value: number) {
  if (value < 60) {
    return `${value} min`;
  }

  const hours = Math.floor(value / 60);
  const minutes = value % 60;
  if (minutes === 0) {
    return `${hours} hr`;
  }

  return `${hours} hr ${minutes} min`;
}

export function getPriorityClasses(priority: Priority) {
  const classes: Record<Priority, string> = {
    high: "bg-rose-50 text-rose-700 ring-rose-200",
    low: "bg-slate-100 text-slate-600 ring-slate-200",
    medium: "bg-amber-50 text-amber-700 ring-amber-200",
  };

  return classes[priority];
}

export function getDailyTaskStatusLabel(status: DailyTaskStatus) {
  const labels: Record<DailyTaskStatus, string> = {
    DONE: "Done",
    IN_PROGRESS: "In progress",
    PENDING: "Pending",
    SKIPPED: "Skipped",
  };

  return labels[status];
}

export function getDailyTaskStatusClasses(status: DailyTaskStatus) {
  const classes: Record<DailyTaskStatus, string> = {
    DONE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    IN_PROGRESS: "bg-sky-50 text-sky-700 ring-sky-200",
    PENDING: "bg-slate-100 text-slate-600 ring-slate-200",
    SKIPPED: "bg-amber-50 text-amber-700 ring-amber-200",
  };

  return classes[status];
}

export function getProgressImpactLabel(impact: ProgressImpact) {
  const labels: Record<ProgressImpact, string> = {
    major: "Major impact",
    medium: "Medium impact",
    minor: "Minor impact",
    none: "No impact",
  };

  return labels[impact];
}

export function getProgressImpactClasses(impact: ProgressImpact) {
  const classes: Record<ProgressImpact, string> = {
    major: "bg-rose-50 text-rose-700 ring-rose-200",
    medium: "bg-amber-50 text-amber-700 ring-amber-200",
    minor: "bg-sky-50 text-sky-700 ring-sky-200",
    none: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  };

  return classes[impact];
}
