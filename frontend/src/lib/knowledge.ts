export type KnowledgeDocumentStatus =
  | "PENDING"
  | "PROCESSING"
  | "READY"
  | "FAILED";

export type KnowledgeDocumentScope = "PERSONAL" | "PLATFORM";

export type KnowledgeDocumentSummary = {
  id: number;
  userId: number;
  scope: KnowledgeDocumentScope;
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
