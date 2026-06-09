export type GoalStatus = "ACTIVE" | "COMPLETED" | "PAUSED" | "CANCELLED";

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

export function getPriorityClasses(priority: Priority) {
  const classes: Record<Priority, string> = {
    high: "bg-rose-50 text-rose-700 ring-rose-200",
    low: "bg-slate-100 text-slate-600 ring-slate-200",
    medium: "bg-amber-50 text-amber-700 ring-amber-200",
  };

  return classes[priority];
}
