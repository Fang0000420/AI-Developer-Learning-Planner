export type GoalStatus = "ACTIVE" | "COMPLETED" | "PAUSED" | "CANCELLED";
export type LearningPlanStatus = "ACTIVE" | "PAUSED";
export type ResponseLanguage = "zh" | "en";

export type Goal = {
  id: number;
  userId: number;
  title: string;
  description: string | null;
  durationDays: number;
  responseLanguage: ResponseLanguage;
  status: GoalStatus;
  dailyAvailableHours: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type GoalKnowledgePreference = {
  goalId: number;
  preferredDocumentIds: number[];
  preferredScope: "PERSONAL" | "PLATFORM" | null;
  preferredCategories: string[];
};

export type KnowledgeBasisDocument = {
  documentId: number | null;
  title: string;
  scope: "PERSONAL" | "PLATFORM";
  sourceCategory: string;
  selectedForContext: boolean;
  reasons: string[];
};

export type KnowledgeBasis = {
  summary: string;
  preference: GoalKnowledgePreference;
  referencedDocumentTitles: string[];
  knowledgeEvidence: string[];
  documents: KnowledgeBasisDocument[];
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

export type GoalProfileSnapshot = {
  id: number;
  goalId: number;
  goalTitle: string;
  userProfileVersionId: number;
  version: number;
  summary: string;
  preferredLearningStyle: string | null;
  pacePreference: string | null;
  timeBudgetNote: string | null;
  currentSkills: string[];
  strengths: string[];
  weaknesses: string[];
  focusAreas: string[];
  riskSignals: string[];
  evidence: string[];
  recommendedDirection: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type UserProfile = {
  id: number;
  userId: number;
  currentVersion: number;
  profileSummary: string;
  preferredLearningStyle: string | null;
  pacePreference: string | null;
  timeBudgetNote: string | null;
  manualCorrection: string | null;
  currentSkills: string[];
  strengths: string[];
  weaknesses: string[];
  focusAreas: string[];
  riskSignals: string[];
  evidence: string[];
  recommendedDirection: string;
  recentSnapshots: GoalProfileSnapshot[];
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

export type PathRecommendation = {
  id: number;
  goalId: number;
  userId: number;
  sourceAgentRunId: number | null;
  version: number;
  recommendedPath: string;
  summary: string;
  currentPosition: string;
  nextStep: string;
  difficulty: string;
  durationDays: number;
  dailyTimeHours: number;
  focusAreas: string[];
  milestones: string[];
  riskSignals: string[];
  evidence: string[];
  knowledgeBasis: KnowledgeBasis;
  finalDeliverables: string[];
  createdAt: string | null;
  updatedAt: string | null;
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

export type LearningPlanVersionSummary = {
  version: number;
  trigger: string;
  reason: string;
  dayCount: number;
  taskCount: number;
  totalEstimatedMinutes: number;
  minuteDelta: number;
  taskDelta: number;
  affectedDayIndexes: number[];
  diff: LearningPlanVersionDiff;
  current: boolean;
  createdAt: string | null;
};

export type LearningPlanVersionTaskChange = {
  dayIndex: number;
  title: string;
  changeType: "added" | "removed" | "updated";
  previousEstimatedMinutes?: number | null;
  currentEstimatedMinutes?: number | null;
};

export type LearningPlanVersionDiff = {
  addedTaskCount: number;
  removedTaskCount: number;
  updatedTaskCount: number;
  changedDayIndexes: number[];
  taskChanges: LearningPlanVersionTaskChange[];
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
  versions: LearningPlanVersionSummary[];
  knowledgeBasis: KnowledgeBasis;
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
  wins?: string[];
  nextFocus?: string[];
  paceAdjustment?: "keep" | "slower" | "faster";
  confidence?: "low" | "medium" | "high";
  adaptiveSchedule?: AdaptiveScheduleResult;
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

export type AdaptiveScheduleResult = {
  pacing?: "lighter" | "steady" | "stronger";
  reason?: string;
  recentCompletionRate?: number;
  recentBlockerAverage?: number;
  minuteAdjustmentPercent?: number;
  affectedDayIndexes?: number[];
};

export type AdaptiveScheduleOverrideSummary = {
  pacing?: "lighter" | "steady" | "stronger";
  reason?: string;
  affectedDayIndexes?: number[];
  anchorDayIndex?: number;
  appliedAt?: string | null;
};

export type AdaptiveScheduleControl = {
  latestAutomatic?: AdaptiveScheduleResult | null;
  activeOverride?: AdaptiveScheduleOverrideSummary | null;
  evidence?: string[];
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
export type AsyncJobType =
  | "KNOWLEDGE_INGESTION"
  | "PATH_ANALYSIS"
  | "PLAN_GENERATION"
  | "PROGRESS_SUBMISSION";

export type AsyncJob<T> = {
  jobId: string;
  jobType: AsyncJobType;
  status: AsyncJobStatus;
  result: T | null;
  errorMessage: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AgentRunStatus = "SUCCESS" | "FAILED";
export type AgentResponseSource = "MODEL" | "FALLBACK";

export type AgentRunSummary = {
  id: number;
  userId: number | null;
  goalId: number | null;
  planId: number | null;
  agentName: string;
  status: AgentRunStatus;
  responseSource: AgentResponseSource | null;
  latencyMs: number;
  errorMessage: string | null;
  requestId: string | null;
  createdAt: string | null;
};

export type AgentRunDetail = AgentRunSummary & {
  inputJson: unknown;
  outputJson: unknown;
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
  responseLanguage: ResponseLanguage;
};

export function getGoalStatusLabel(
  status: GoalStatus,
  locale: ResponseLanguage = "en",
) {
  if (locale === "zh") {
    const labels: Record<GoalStatus, string> = {
      ACTIVE: "进行中",
      CANCELLED: "已取消",
      COMPLETED: "已完成",
      PAUSED: "已暂停",
    };

    return labels[status];
  }

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

export function formatGoalDate(
  value: string | null,
  locale: ResponseLanguage = "en",
) {
  if (!value) {
    return locale === "zh" ? "未记录" : "Not recorded";
  }

  return new Intl.DateTimeFormat(locale === "zh" ? "zh-CN" : "en-US", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

export function formatDailyHours(
  value: number | null,
  locale: ResponseLanguage = "en",
) {
  if (value === null) {
    return locale === "zh" ? "未设置" : "Not set";
  }

  return locale === "zh" ? `${value} 小时` : `${value} hours`;
}

export function formatMinutes(value: number, locale: ResponseLanguage = "en") {
  if (value < 60) {
    return locale === "zh" ? `${value} 分钟` : `${value} min`;
  }

  const hours = Math.floor(value / 60);
  const minutes = value % 60;
  if (minutes === 0) {
    return locale === "zh" ? `${hours} 小时` : `${hours} hr`;
  }

  return locale === "zh"
    ? `${hours} 小时 ${minutes} 分钟`
    : `${hours} hr ${minutes} min`;
}

export function getPriorityClasses(priority: Priority) {
  const classes: Record<Priority, string> = {
    high: "bg-rose-50 text-rose-700 ring-rose-200 dark:bg-rose-500/10 dark:text-rose-300 dark:ring-rose-500/30",
    low: "bg-slate-100 text-slate-600 ring-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:ring-slate-700",
    medium:
      "bg-amber-50 text-amber-700 ring-amber-200 dark:bg-amber-500/10 dark:text-amber-300 dark:ring-amber-500/30",
  };

  return classes[priority];
}

export function getDailyTaskStatusLabel(
  status: DailyTaskStatus,
  locale: ResponseLanguage = "en",
) {
  if (locale === "zh") {
    const labels: Record<DailyTaskStatus, string> = {
      DONE: "已完成",
      IN_PROGRESS: "进行中",
      PENDING: "待开始",
      SKIPPED: "已跳过",
    };

    return labels[status];
  }

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

export function getProgressImpactLabel(
  impact: ProgressImpact,
  locale: ResponseLanguage = "en",
) {
  if (locale === "zh") {
    const labels: Record<ProgressImpact, string> = {
      major: "严重影响",
      medium: "中等影响",
      minor: "轻微影响",
      none: "无影响",
    };

    return labels[impact];
  }

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
