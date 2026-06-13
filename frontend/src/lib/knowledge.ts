export type KnowledgeDocumentStatus =
  | "PENDING"
  | "PROCESSING"
  | "READY"
  | "FAILED";

export type KnowledgeDocumentScope = "PERSONAL" | "PLATFORM";
export type KnowledgeSourceCategory =
  | "NOTE"
  | "RESUME"
  | "PROJECT"
  | "COURSE"
  | "REFERENCE"
  | "OTHER";

export type KnowledgeDocumentSummary = {
  id: number;
  userId: number;
  scope: KnowledgeDocumentScope;
  sourceCategory: KnowledgeSourceCategory;
  groupName: string | null;
  tags: string[];
  retrievalPriority: number;
  title: string;
  sourceLabel: string | null;
  originalFileName: string;
  mimeType: string | null;
  fileSizeBytes: number;
  status: KnowledgeDocumentStatus;
  enabled: boolean;
  summary: string | null;
  chunkCount: number;
  importedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type KnowledgeDocumentDetail = KnowledgeDocumentSummary & {
  previewText: string | null;
};

export type KnowledgeUploadResponse = {
  document: KnowledgeDocumentSummary;
  jobId: string;
};

export type KnowledgeRetrievalPreviewMatch = {
  documentId: number;
  title: string;
  scope: KnowledgeDocumentScope;
  sourceCategory: KnowledgeSourceCategory;
  retrievalPriority: number;
  groupName: string | null;
  tags: string[];
  score: number;
  selectedForContext: boolean;
  reasons: string[];
  excerpts: string[];
};

export type KnowledgeRetrievalPreview = {
  goalId: number;
  goalTitle: string;
  documentsReviewed: number;
  matchedDocumentCount: number;
  contextDocumentCount: number;
  rules: string[];
  matches: KnowledgeRetrievalPreviewMatch[];
};

export type KnowledgeStrategyComparisonDocument = {
  documentId: number;
  title: string;
  scope: KnowledgeDocumentScope;
  sourceCategory: KnowledgeSourceCategory;
  baseScore: number | null;
  compareScore: number | null;
  scoreDelta: number | null;
  selectedByBase: boolean;
  selectedByCompare: boolean;
  tags: string[];
};

export type KnowledgeStrategyComparison = {
  baseGoalId: number;
  baseGoalTitle: string;
  compareGoalId: number;
  compareGoalTitle: string;
  basePreference: {
    goalId: number;
    preferredDocumentIds: number[];
    preferredScope: KnowledgeDocumentScope | null;
    preferredCategories: string[];
  };
  comparePreference: {
    goalId: number;
    preferredDocumentIds: number[];
    preferredScope: KnowledgeDocumentScope | null;
    preferredCategories: string[];
  };
  differences: string[];
  onlyInBase: KnowledgeStrategyComparisonDocument[];
  onlyInCompare: KnowledgeStrategyComparisonDocument[];
  sharedDocuments: KnowledgeStrategyComparisonDocument[];
};

export function formatFileSize(fileSizeBytes: number, locale: "zh" | "en") {
  if (fileSizeBytes < 1024) {
    return `${fileSizeBytes} ${locale === "zh" ? "字节" : "B"}`;
  }
  if (fileSizeBytes < 1024 * 1024) {
    return `${(fileSizeBytes / 1024).toFixed(1)} KB`;
  }
  return `${(fileSizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function knowledgeStatusLabel(
  status: KnowledgeDocumentStatus,
  locale: "zh" | "en",
) {
  if (locale === "zh") {
    const labels: Record<KnowledgeDocumentStatus, string> = {
      FAILED: "失败",
      PENDING: "待导入",
      PROCESSING: "导入中",
      READY: "可用",
    };
    return labels[status];
  }

  const labels: Record<KnowledgeDocumentStatus, string> = {
    FAILED: "Failed",
    PENDING: "Pending",
    PROCESSING: "Processing",
    READY: "Ready",
  };
  return labels[status];
}

export function knowledgeScopeLabel(
  scope: KnowledgeDocumentScope,
  locale: "zh" | "en",
) {
  if (locale === "zh") {
    return {
      PERSONAL: "个人资料",
      PLATFORM: "平台资料",
    }[scope];
  }

  return {
    PERSONAL: "Personal",
    PLATFORM: "Platform",
  }[scope];
}

export function knowledgePriorityLabel(
  priority: number,
  locale: "zh" | "en",
) {
  if (locale === "zh") {
    return `优先级 ${priority}`;
  }
  return `Priority ${priority}`;
}

export function knowledgeSourceCategoryLabel(
  category: KnowledgeSourceCategory,
  locale: "zh" | "en",
) {
  if (locale === "zh") {
    return {
      NOTE: "笔记",
      RESUME: "简历",
      PROJECT: "项目",
      COURSE: "课程",
      REFERENCE: "资料",
      OTHER: "其他",
    }[category];
  }

  return {
    NOTE: "Note",
    RESUME: "Resume",
    PROJECT: "Project",
    COURSE: "Course",
    REFERENCE: "Reference",
    OTHER: "Other",
  }[category];
}
