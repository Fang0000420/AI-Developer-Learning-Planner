"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  BookOpen,
  CheckCircle2,
  FileText,
  LoaderCircle,
  RefreshCcw,
  Search,
  Upload,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { getApiErrorMessage, pollJob } from "@/lib/client-jobs";
import type {
  ApiErrorResponse,
  Goal,
  GoalKnowledgePreference,
} from "@/lib/goals";
import {
  formatFileSize,
  knowledgePriorityLabel,
  knowledgeScopeLabel,
  knowledgeSourceCategoryLabel,
  knowledgeStatusLabel,
  type KnowledgeRetrievalPreview,
  type KnowledgeStrategyComparison,
  type KnowledgeSourceCategory,
  type KnowledgeDocumentSummary,
  type KnowledgeUploadResponse,
} from "@/lib/knowledge";
import type { Locale } from "@/lib/i18n";

type KnowledgeWorkspaceProps = {
  initialDocuments: KnowledgeDocumentSummary[];
  initialGoals: Goal[];
  initialSelectedGoalId?: string;
  initialError?: string | null;
  locale: Locale;
};

export function KnowledgeWorkspace({
  initialDocuments,
  initialGoals,
  initialSelectedGoalId = "",
  initialError = null,
  locale,
}: KnowledgeWorkspaceProps) {
  const router = useRouter();
  const [documents, setDocuments] =
    useState<KnowledgeDocumentSummary[]>(initialDocuments);
  const [error, setError] = useState<string | null>(initialError);
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [pendingDocumentIds, setPendingDocumentIds] = useState<number[]>([]);
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<number[]>([]);
  const [batchGroupName, setBatchGroupName] = useState("");
  const [batchTags, setBatchTags] = useState("");
  const [batchCategory, setBatchCategory] =
    useState<KnowledgeSourceCategory>("NOTE");
  const [searchText, setSearchText] = useState("");
  const [filterScope, setFilterScope] = useState<"ALL" | "PERSONAL" | "PLATFORM">(
    "ALL",
  );
  const [filterCategory, setFilterCategory] =
    useState<"ALL" | KnowledgeSourceCategory>("ALL");
  const [filterEnabled, setFilterEnabled] = useState<"ALL" | "ENABLED" | "DISABLED">(
    "ALL",
  );
  const [filterGroup, setFilterGroup] = useState("ALL");
  const [filterTag, setFilterTag] = useState("ALL");
  const [previewGoalId, setPreviewGoalId] = useState(
    initialSelectedGoalId || (initialGoals[0] ? String(initialGoals[0].id) : ""),
  );
  const [compareGoalId, setCompareGoalId] = useState(
    initialGoals.find((goal) => String(goal.id) !== (initialSelectedGoalId || (initialGoals[0] ? String(initialGoals[0].id) : "")))
      ? String(
          initialGoals.find(
            (goal) =>
              String(goal.id) !==
              (initialSelectedGoalId ||
                (initialGoals[0] ? String(initialGoals[0].id) : "")),
          )?.id,
        )
      : "",
  );
  const [previewLoading, setPreviewLoading] = useState(false);
  const [retrievalPreview, setRetrievalPreview] =
    useState<KnowledgeRetrievalPreview | null>(null);
  const [comparisonLoading, setComparisonLoading] = useState(false);
  const [strategyComparison, setStrategyComparison] =
    useState<KnowledgeStrategyComparison | null>(null);
  const [goalPreferenceLoading, setGoalPreferenceLoading] = useState(false);
  const [goalPreference, setGoalPreference] =
    useState<GoalKnowledgePreference | null>(null);

  const sortedDocuments = useMemo(
    () =>
      [...documents].sort((left, right) => {
        const rightValue = right.createdAt ? Date.parse(right.createdAt) : 0;
        const leftValue = left.createdAt ? Date.parse(left.createdAt) : 0;
        return rightValue - leftValue;
      }),
    [documents],
  );

  const availableGroups = useMemo(
    () =>
      Array.from(
        new Set(
          documents
            .map((document) => document.groupName)
            .filter((value): value is string => Boolean(value)),
        ),
      ).sort(),
    [documents],
  );

  const availableTags = useMemo(
    () => Array.from(new Set(documents.flatMap((document) => document.tags))).sort(),
    [documents],
  );

  const filteredDocuments = useMemo(() => {
    const normalizedSearch = searchText.trim().toLowerCase();
    return sortedDocuments.filter((document) => {
      if (
        normalizedSearch &&
        ![
          document.title,
          document.summary ?? "",
          document.originalFileName,
          document.groupName ?? "",
          document.tags.join(" "),
        ]
          .join(" ")
          .toLowerCase()
          .includes(normalizedSearch)
      ) {
        return false;
      }
      if (filterScope !== "ALL" && document.scope !== filterScope) {
        return false;
      }
      if (filterCategory !== "ALL" && document.sourceCategory !== filterCategory) {
        return false;
      }
      if (filterEnabled === "ENABLED" && !document.enabled) {
        return false;
      }
      if (filterEnabled === "DISABLED" && document.enabled) {
        return false;
      }
      if (filterGroup !== "ALL" && document.groupName !== filterGroup) {
        return false;
      }
      if (filterTag !== "ALL" && !document.tags.includes(filterTag)) {
        return false;
      }
      return true;
    });
  }, [
    filterCategory,
    filterEnabled,
    filterGroup,
    filterScope,
    filterTag,
    searchText,
    sortedDocuments,
  ]);

  async function refreshDocuments() {
    const response = await fetch("/api/knowledge/documents", { cache: "no-store" });
    if (!response.ok) {
      return;
    }
    const payload = (await response.json()) as KnowledgeDocumentSummary[];
    setDocuments(payload);
  }

  async function handleUpload(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      setError(locale === "zh" ? "请先选择一个文件。" : "Select a file first.");
      return;
    }

    setError(null);
    setIsUploading(true);

    try {
      const formData = new FormData();
      if (title.trim()) {
        formData.append("title", title.trim());
      }
      formData.append("file", file);

      const response = await fetch("/api/knowledge/documents", {
        body: formData,
        method: "POST",
      });
      const payload = (await response.json()) as
        | KnowledgeUploadResponse
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh" ? "知识库文档上传失败。" : "Knowledge upload failed.",
          ),
        );
        return;
      }

      const upload = payload as KnowledgeUploadResponse;
      setDocuments((current) => [upload.document, ...current]);
      setPendingDocumentIds((current) => [...current, upload.document.id]);
      setFile(null);
      setTitle("");

      const ingested = await pollJob<KnowledgeDocumentSummary>(upload.jobId);
      setDocuments((current) =>
        current.map((item) => (item.id === ingested.id ? ingested : item)),
      );
      setPendingDocumentIds((current) =>
        current.filter((item) => item !== upload.document.id),
      );
      router.refresh();
    } catch (requestError) {
      await refreshDocuments();
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "知识库文档上传失败。"
            : "Knowledge upload failed.",
      );
    } finally {
      setIsUploading(false);
    }
  }

  async function handleToggle(document: KnowledgeDocumentSummary) {
    setError(null);
    setPendingDocumentIds((current) => [...current, document.id]);

    try {
      const response = await fetch(`/api/knowledge/documents/${document.id}/enabled`, {
        body: JSON.stringify({ enabled: !document.enabled }),
        headers: { "Content-Type": "application/json" },
        method: "PATCH",
      });
      const payload = (await response.json()) as
        | KnowledgeDocumentSummary
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh" ? "知识库状态更新失败。" : "Knowledge update failed.",
          ),
        );
        return;
      }
      const updated = payload as KnowledgeDocumentSummary;
      setDocuments((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "知识库状态更新失败。"
            : "Knowledge update failed.",
      );
    } finally {
      setPendingDocumentIds((current) =>
        current.filter((item) => item !== document.id),
      );
    }
  }

  async function handleSettingsChange(
    document: KnowledgeDocumentSummary,
    updates: Partial<Pick<KnowledgeDocumentSummary, "scope" | "retrievalPriority">>,
  ) {
    setError(null);
    setPendingDocumentIds((current) => [...current, document.id]);
    try {
      const response = await fetch(
        `/api/knowledge/documents/${document.id}/settings`,
        {
          body: JSON.stringify({
            scope: updates.scope ?? document.scope,
            retrievalPriority:
              updates.retrievalPriority ?? document.retrievalPriority,
          }),
          headers: { "Content-Type": "application/json" },
          method: "PATCH",
        },
      );
      const payload = (await response.json()) as
        | KnowledgeDocumentSummary
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh" ? "知识库设置更新失败。" : "Knowledge settings update failed.",
          ),
        );
        return;
      }
      const updated = payload as KnowledgeDocumentSummary;
      setDocuments((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "知识库设置更新失败。"
            : "Knowledge settings update failed.",
      );
    } finally {
      setPendingDocumentIds((current) =>
        current.filter((item) => item !== document.id),
      );
    }
  }

  async function handleMetadataChange(
    document: KnowledgeDocumentSummary,
    updates: Partial<
      Pick<
        KnowledgeDocumentSummary,
        "scope" | "retrievalPriority" | "sourceCategory" | "groupName" | "tags"
      >
    >,
  ) {
    setError(null);
    setPendingDocumentIds((current) => [...current, document.id]);
    try {
      const response = await fetch(
        `/api/knowledge/documents/${document.id}/metadata`,
        {
          body: JSON.stringify({
            scope: updates.scope ?? document.scope,
            retrievalPriority:
              updates.retrievalPriority ?? document.retrievalPriority,
            sourceCategory: updates.sourceCategory ?? document.sourceCategory,
            groupName: updates.groupName ?? document.groupName,
            tags: updates.tags ?? document.tags,
          }),
          headers: { "Content-Type": "application/json" },
          method: "PATCH",
        },
      );
      const payload = (await response.json()) as
        | KnowledgeDocumentSummary
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh" ? "知识库元数据更新失败。" : "Knowledge metadata update failed.",
          ),
        );
        return;
      }
      const updated = payload as KnowledgeDocumentSummary;
      setDocuments((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "知识库元数据更新失败。"
            : "Knowledge metadata update failed.",
      );
    } finally {
      setPendingDocumentIds((current) =>
        current.filter((item) => item !== document.id),
      );
    }
  }

  async function handleBatchUpdate(
    updates: {
      enabled?: boolean;
      sourceCategory?: KnowledgeSourceCategory;
      groupName?: string;
      tags?: string[];
    },
  ) {
    if (selectedDocumentIds.length === 0) {
      setError(
        locale === "zh"
          ? "请先选择至少一份文档。"
          : "Select at least one document first.",
      );
      return;
    }
    setError(null);
    setPendingDocumentIds((current) => [
      ...current,
      ...selectedDocumentIds.filter((id) => !current.includes(id)),
    ]);
    try {
      const response = await fetch("/api/knowledge/documents/batch", {
        body: JSON.stringify({
          documentIds: selectedDocumentIds,
          ...updates,
        }),
        headers: { "Content-Type": "application/json" },
        method: "PATCH",
      });
      const payload = (await response.json()) as
        | KnowledgeDocumentSummary[]
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh" ? "批量治理失败。" : "Batch governance failed.",
          ),
        );
        return;
      }
      const updatedDocuments = payload as KnowledgeDocumentSummary[];
      setDocuments((current) =>
        current.map((item) => {
          const updated = updatedDocuments.find((candidate) => candidate.id === item.id);
          return updated ?? item;
        }),
      );
      setSelectedDocumentIds([]);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "批量治理失败。"
            : "Batch governance failed.",
      );
    } finally {
      setPendingDocumentIds((current) =>
        current.filter((item) => !selectedDocumentIds.includes(item)),
      );
    }
  }

  function toggleSelection(documentId: number) {
    setSelectedDocumentIds((current) =>
      current.includes(documentId)
        ? current.filter((item) => item !== documentId)
        : [...current, documentId],
    );
  }

  function allSelected() {
    return (
      filteredDocuments.length > 0 &&
      filteredDocuments.every((document) => selectedDocumentIds.includes(document.id))
    );
  }

  async function loadRetrievalPreview() {
    if (!previewGoalId) {
      setError(
        locale === "zh" ? "请先选择一个目标。" : "Select a goal first.",
      );
      return;
    }
    setError(null);
    setPreviewLoading(true);
    try {
      const response = await fetch(
        `/api/knowledge/documents/retrieval-preview/${previewGoalId}`,
        {
          cache: "no-store",
        },
      );
      const payload = (await response.json()) as
        | KnowledgeRetrievalPreview
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh"
              ? "检索预览加载失败。"
              : "Retrieval preview failed to load.",
          ),
        );
        return;
      }
      setRetrievalPreview(payload as KnowledgeRetrievalPreview);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "检索预览加载失败。"
            : "Retrieval preview failed to load.",
      );
    } finally {
      setPreviewLoading(false);
    }
  }

  async function loadGoalPreference(goalId: string) {
    if (!goalId) {
      setGoalPreference(null);
      return;
    }
    setGoalPreferenceLoading(true);
    try {
      const response = await fetch(`/api/goals/${goalId}/knowledge-preference`, {
        cache: "no-store",
      });
      const payload = (await response.json()) as
        | GoalKnowledgePreference
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh"
              ? "目标知识偏好加载失败。"
              : "Failed to load goal knowledge preferences.",
          ),
        );
        return;
      }
      const preference = payload as GoalKnowledgePreference;
      setGoalPreference(preference);
      setSelectedDocumentIds(preference.preferredDocumentIds);
      setFilterScope(
        preference.preferredScope ? preference.preferredScope : "ALL",
      );
      setFilterCategory(
        preference.preferredCategories[0]
          ? (preference.preferredCategories[0] as typeof filterCategory)
          : "ALL",
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "目标知识偏好加载失败。"
            : "Failed to load goal knowledge preferences.",
      );
    } finally {
      setGoalPreferenceLoading(false);
    }
  }

  async function loadStrategyComparison() {
    if (!previewGoalId || !compareGoalId) {
      setError(
        locale === "zh"
          ? "请选择两个目标进行对比。"
          : "Select two goals to compare.",
      );
      return;
    }
    if (previewGoalId === compareGoalId) {
      setError(
        locale === "zh"
          ? "请为对比选择两个不同的目标。"
          : "Choose two different goals for comparison.",
      );
      return;
    }
    setError(null);
    setComparisonLoading(true);
    try {
      const response = await fetch(
        `/api/knowledge/documents/strategy-compare?baseGoalId=${previewGoalId}&compareGoalId=${compareGoalId}`,
        { cache: "no-store" },
      );
      const payload = (await response.json()) as
        | KnowledgeStrategyComparison
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh"
              ? "知识策略对比加载失败。"
              : "Knowledge strategy comparison failed to load.",
          ),
        );
        return;
      }
      setStrategyComparison(payload as KnowledgeStrategyComparison);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "知识策略对比加载失败。"
            : "Knowledge strategy comparison failed to load.",
      );
    } finally {
      setComparisonLoading(false);
    }
  }

  async function saveGoalPreference() {
    if (!previewGoalId) {
      setError(locale === "zh" ? "请先选择一个目标。" : "Select a goal first.");
      return;
    }
    setError(null);
    setGoalPreferenceLoading(true);
    try {
      const response = await fetch(`/api/goals/${previewGoalId}/knowledge-preference`, {
        body: JSON.stringify({
          preferredDocumentIds: selectedDocumentIds,
          preferredScope: filterScope === "ALL" ? null : filterScope,
          preferredCategories:
            filterCategory === "ALL" ? [] : [filterCategory],
        }),
        headers: { "Content-Type": "application/json" },
        method: "PATCH",
      });
      const payload = (await response.json()) as
        | GoalKnowledgePreference
        | ApiErrorResponse;
      if (!response.ok) {
        setError(
          getApiErrorMessage(
            payload as ApiErrorResponse,
            locale === "zh"
              ? "目标知识偏好保存失败。"
              : "Failed to save goal knowledge preferences.",
          ),
        );
        return;
      }
      setGoalPreference(payload as GoalKnowledgePreference);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : locale === "zh"
            ? "目标知识偏好保存失败。"
            : "Failed to save goal knowledge preferences.",
      );
    } finally {
      setGoalPreferenceLoading(false);
    }
  }

  function retrievalHint(document: KnowledgeDocumentSummary) {
    if (locale === "zh") {
      if (!document.enabled) {
        return "当前已停用，不会参与后续检索。";
      }
      return `${knowledgeScopeLabel(document.scope, locale)}，${knowledgePriorityLabel(document.retrievalPriority, locale)}，${knowledgeSourceCategoryLabel(document.sourceCategory, locale)}。`;
    }
    if (!document.enabled) {
      return "Disabled documents do not participate in retrieval.";
    }
    return `${knowledgeScopeLabel(document.scope, locale)}, ${knowledgePriorityLabel(document.retrievalPriority, locale)}, ${knowledgeSourceCategoryLabel(document.sourceCategory, locale)}.`;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
      <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
            <Upload aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-base font-semibold text-slate-950">
              {locale === "zh" ? "上传文档" : "Upload Document"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "第一版先支持文本、Markdown、CSV、JSON、XML 文件，系统会异步导入并切分为知识块。"
                : "The first version supports text, Markdown, CSV, JSON, and XML files. The system ingests them asynchronously and splits them into chunks."}
            </p>
          </div>
        </div>

        <form className="mt-5 space-y-4" onSubmit={handleUpload}>
          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="knowledge-title"
            >
              {locale === "zh" ? "标题" : "Title"}
            </label>
            <input
              className="h-11 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
              id="knowledge-title"
              onChange={(event) => setTitle(event.target.value)}
              placeholder={
                locale === "zh" ? "可选，默认使用文件名" : "Optional, defaults to file name"
              }
              value={title}
            />
          </div>

          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="knowledge-file"
            >
              {locale === "zh" ? "文件" : "File"}
            </label>
            <input
              accept=".txt,.md,.csv,.json,.xml,text/plain,text/markdown,text/csv,application/json,application/xml"
              className="block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-700 file:mr-3 file:rounded-md file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-medium file:text-slate-700"
              id="knowledge-file"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              type="file"
            />
          </div>

          <button
            className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            disabled={isUploading}
            type="submit"
          >
            {isUploading ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : (
              <Upload aria-hidden="true" className="size-4" />
            )}
            {isUploading
              ? locale === "zh"
                ? "上传并导入中"
                : "Uploading and ingesting"
              : locale === "zh"
                ? "上传到知识库"
                : "Upload to Knowledge Base"}
          </button>
        </form>

        {error ? (
          <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 p-3 text-sm leading-6 text-rose-700">
            {error}
          </div>
        ) : null}
      </section>

      <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-sky-50 text-sky-700">
            <BookOpen aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-base font-semibold text-slate-950">
              {locale === "zh" ? "我的知识库" : "My Knowledge Base"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              {locale === "zh"
                ? "这里展示文档的导入状态、作用域、可用块数量以及是否参与后续画像、路径和计划判断。个人资料会优先参与检索。"
                : "This list shows ingestion status, scope, available chunk count, and whether the document can participate in later profile, path, and planning decisions. Personal materials are prioritized in retrieval."}
            </p>
          </div>
        </div>

        {sortedDocuments.length === 0 ? (
          <p className="mt-6 rounded-md border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
            {locale === "zh"
              ? "还没有知识库文档。先上传一份学习笔记、简历、项目总结或资料摘录。"
              : "There are no knowledge documents yet. Start with a note, résumé, project summary, or reference excerpt."}
          </p>
        ) : (
          <div className="mt-6 space-y-4">
            <section className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-4">
                <div>
                  <h3 className="text-sm font-semibold text-slate-950">
                    {locale === "zh" ? "筛选" : "Filters"}
                  </h3>
                  <p className="mt-1 text-sm text-slate-600">
                    {locale === "zh"
                      ? "按来源分类、分组、标签、作用域和启用状态快速筛选资料。"
                      : "Filter materials by source category, group, tag, scope, and enablement."}
                  </p>
                </div>
                <div className="grid gap-3 lg:grid-cols-6">
                  <div className="lg:col-span-2">
                    <label className="mb-2 block text-xs font-medium text-slate-500">
                      {locale === "zh" ? "搜索" : "Search"}
                    </label>
                    <div className="relative">
                      <Search
                        aria-hidden="true"
                        className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400"
                      />
                      <input
                        className="h-10 w-full rounded-md border border-slate-300 bg-white pl-9 pr-3 text-sm text-slate-700"
                        onChange={(event) => setSearchText(event.target.value)}
                        placeholder={
                          locale === "zh"
                            ? "标题、摘要、标签、分组"
                            : "Title, summary, tags, group"
                        }
                        value={searchText}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="mb-2 block text-xs font-medium text-slate-500">
                      {locale === "zh" ? "作用域" : "Scope"}
                    </label>
                    <select
                      className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                      onChange={(event) =>
                        setFilterScope(
                          event.target.value as typeof filterScope,
                        )
                      }
                      value={filterScope}
                    >
                      <option value="ALL">{locale === "zh" ? "全部" : "All"}</option>
                      <option value="PERSONAL">
                        {locale === "zh" ? "个人资料" : "Personal"}
                      </option>
                      <option value="PLATFORM">
                        {locale === "zh" ? "平台资料" : "Platform"}
                      </option>
                    </select>
                  </div>
                  <div>
                    <label className="mb-2 block text-xs font-medium text-slate-500">
                      {locale === "zh" ? "来源分类" : "Source Category"}
                    </label>
                    <select
                      className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                      onChange={(event) =>
                        setFilterCategory(
                          event.target.value as typeof filterCategory,
                        )
                      }
                      value={filterCategory}
                    >
                      <option value="ALL">{locale === "zh" ? "全部" : "All"}</option>
                      {(
                        [
                          "NOTE",
                          "RESUME",
                          "PROJECT",
                          "COURSE",
                          "REFERENCE",
                          "OTHER",
                        ] as const
                      ).map((category) => (
                        <option key={category} value={category}>
                          {knowledgeSourceCategoryLabel(category, locale)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-2 block text-xs font-medium text-slate-500">
                      {locale === "zh" ? "启用状态" : "Enabled"}
                    </label>
                    <select
                      className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                      onChange={(event) =>
                        setFilterEnabled(
                          event.target.value as typeof filterEnabled,
                        )
                      }
                      value={filterEnabled}
                    >
                      <option value="ALL">{locale === "zh" ? "全部" : "All"}</option>
                      <option value="ENABLED">
                        {locale === "zh" ? "已启用" : "Enabled"}
                      </option>
                      <option value="DISABLED">
                        {locale === "zh" ? "已停用" : "Disabled"}
                      </option>
                    </select>
                  </div>
                  <div>
                    <label className="mb-2 block text-xs font-medium text-slate-500">
                      {locale === "zh" ? "分组" : "Group"}
                    </label>
                    <select
                      className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                      onChange={(event) => setFilterGroup(event.target.value)}
                      value={filterGroup}
                    >
                      <option value="ALL">{locale === "zh" ? "全部" : "All"}</option>
                      {availableGroups.map((group) => (
                        <option key={group} value={group}>
                          {group}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-2 block text-xs font-medium text-slate-500">
                      {locale === "zh" ? "标签" : "Tag"}
                    </label>
                    <select
                      className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                      onChange={(event) => setFilterTag(event.target.value)}
                      value={filterTag}
                    >
                      <option value="ALL">{locale === "zh" ? "全部" : "All"}</option>
                      {availableTags.map((tag) => (
                        <option key={tag} value={tag}>
                          #{tag}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <p className="text-sm text-slate-500">
                  {locale === "zh"
                    ? `当前筛选结果：${filteredDocuments.length} / ${documents.length}`
                    : `Filtered result: ${filteredDocuments.length} / ${documents.length}`}
                </p>
              </div>
            </section>

            <section className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <h3 className="text-sm font-semibold text-slate-950">
                    {locale === "zh" ? "检索解释面板" : "Retrieval Explanation"}
                  </h3>
                  <p className="mt-1 text-sm text-slate-600">
                    {locale === "zh"
                      ? "选择一个目标，预览当前知识库会命中哪些资料、为什么排在前面，以及哪些资料会进入最终上下文。"
                      : "Choose a goal to preview which materials match, why they rank highly, and which ones enter the final context."}
                  </p>
                </div>
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
                  <select
                    className="h-10 min-w-64 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                    onChange={(event) => {
                      const nextGoalId = event.target.value;
                      setPreviewGoalId(nextGoalId);
                      if (nextGoalId) {
                        void loadGoalPreference(nextGoalId);
                      } else {
                        setGoalPreference(null);
                      }
                    }}
                    value={previewGoalId}
                  >
                    <option value="">
                      {locale === "zh" ? "选择目标" : "Select a goal"}
                    </option>
                    {initialGoals.map((goal) => (
                      <option key={goal.id} value={goal.id}>
                        {goal.title}
                      </option>
                    ))}
                  </select>
                  <button
                    className="inline-flex h-10 items-center justify-center rounded-md bg-slate-950 px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-400"
                    disabled={previewLoading || !previewGoalId}
                    onClick={loadRetrievalPreview}
                    type="button"
                  >
                    {previewLoading
                      ? locale === "zh"
                        ? "加载中"
                        : "Loading"
                      : locale === "zh"
                        ? "预览当前检索"
                        : "Preview Retrieval"}
                  </button>
                  <button
                    className="inline-flex h-10 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
                    disabled={goalPreferenceLoading || !previewGoalId}
                    onClick={() => void loadGoalPreference(previewGoalId)}
                    type="button"
                  >
                    {goalPreferenceLoading
                      ? locale === "zh"
                        ? "加载中"
                        : "Loading"
                      : locale === "zh"
                        ? "加载目标偏好"
                        : "Load Goal Preference"}
                  </button>
                  <button
                    className="inline-flex h-10 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
                    disabled={goalPreferenceLoading || !previewGoalId}
                    onClick={saveGoalPreference}
                    type="button"
                  >
                    {goalPreferenceLoading
                      ? locale === "zh"
                        ? "保存中"
                        : "Saving"
                      : locale === "zh"
                        ? "保存为目标偏好"
                        : "Save as Goal Preference"}
                  </button>
                </div>
              </div>

              {goalPreference ? (
                <div className="mt-4 rounded-md border border-slate-200 bg-white p-4 text-sm">
                  <div className="font-medium text-slate-500">
                    {locale === "zh" ? "当前目标偏好" : "Current goal preference"}
                  </div>
                  <div className="mt-2 space-y-2 text-slate-700">
                    <p>
                      {locale === "zh" ? "优先文档数：" : "Preferred docs: "}
                      {goalPreference.preferredDocumentIds.length}
                    </p>
                    <p>
                      {locale === "zh" ? "固定作用域：" : "Fixed scope: "}
                      {goalPreference.preferredScope
                        ? knowledgeScopeLabel(goalPreference.preferredScope, locale)
                        : locale === "zh"
                          ? "未固定"
                          : "Not fixed"}
                    </p>
                    <p>
                      {locale === "zh" ? "固定分类：" : "Fixed categories: "}
                      {goalPreference.preferredCategories.length > 0
                        ? goalPreference.preferredCategories
                            .map((category) =>
                              knowledgeSourceCategoryLabel(
                                category as KnowledgeSourceCategory,
                                locale,
                              ),
                            )
                            .join(", ")
                        : locale === "zh"
                          ? "未固定"
                          : "Not fixed"}
                    </p>
                  </div>
                </div>
              ) : null}

              {retrievalPreview ? (
                <div className="mt-4 space-y-4">
                  <div className="grid gap-3 md:grid-cols-3">
                    <div className="rounded-md border border-slate-200 bg-white p-3">
                      <div className="text-xs font-medium text-slate-500">
                        {locale === "zh" ? "目标" : "Goal"}
                      </div>
                      <p className="mt-1 text-sm font-semibold text-slate-900">
                        {retrievalPreview.goalTitle}
                      </p>
                    </div>
                    <div className="rounded-md border border-slate-200 bg-white p-3">
                      <div className="text-xs font-medium text-slate-500">
                        {locale === "zh" ? "命中文档" : "Matched docs"}
                      </div>
                      <p className="mt-1 text-sm font-semibold text-slate-900">
                        {retrievalPreview.matchedDocumentCount}
                      </p>
                    </div>
                    <div className="rounded-md border border-slate-200 bg-white p-3">
                      <div className="text-xs font-medium text-slate-500">
                        {locale === "zh" ? "进入上下文" : "In final context"}
                      </div>
                      <p className="mt-1 text-sm font-semibold text-slate-900">
                        {retrievalPreview.contextDocumentCount}
                      </p>
                    </div>
                  </div>

                  <div className="rounded-md border border-slate-200 bg-white p-4">
                    <div className="text-sm font-medium text-slate-500">
                      {locale === "zh" ? "排序规则" : "Ranking rules"}
                    </div>
                    <ul className="mt-3 space-y-2 text-sm text-slate-700">
                      {retrievalPreview.rules.map((rule) => (
                        <li key={rule}>{rule}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="space-y-3">
                    {retrievalPreview.matches.map((match) => (
                      <article
                        className="rounded-md border border-slate-200 bg-white p-4"
                        key={match.documentId}
                      >
                        <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="inline-flex h-7 items-center rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                                {knowledgeScopeLabel(match.scope, locale)}
                              </span>
                              <span className="inline-flex h-7 items-center rounded-md bg-violet-50 px-2 text-xs font-semibold text-violet-700">
                                {knowledgeSourceCategoryLabel(
                                  match.sourceCategory,
                                  locale,
                                )}
                              </span>
                              <span className="inline-flex h-7 items-center rounded-md bg-amber-50 px-2 text-xs font-semibold text-amber-700">
                                {knowledgePriorityLabel(
                                  match.retrievalPriority,
                                  locale,
                                )}
                              </span>
                              {match.selectedForContext ? (
                                <span className="inline-flex h-7 items-center rounded-md bg-emerald-50 px-2 text-xs font-semibold text-emerald-700">
                                  {locale === "zh"
                                    ? "进入最终上下文"
                                    : "Selected for context"}
                                </span>
                              ) : null}
                            </div>
                            <h4 className="mt-3 text-base font-semibold text-slate-950">
                              {match.title}
                            </h4>
                            <p className="mt-2 text-sm text-slate-500">
                              {locale === "zh" ? "相关性得分：" : "Relevance score: "}
                              {match.score}
                            </p>
                            <ul className="mt-3 space-y-1 text-sm text-slate-700">
                              {match.reasons.map((reason) => (
                                <li key={reason}>{reason}</li>
                              ))}
                            </ul>
                          </div>
                        </div>

                        {match.excerpts.length > 0 ? (
                          <div className="mt-4 grid gap-2">
                            {match.excerpts.map((excerpt, index) => (
                              <div
                                className="rounded-md border border-slate-200 bg-slate-50 p-3 text-sm leading-6 text-slate-700"
                                key={`${match.documentId}-${index}`}
                              >
                                {excerpt}
                              </div>
                            ))}
                          </div>
                        ) : null}
                      </article>
                    ))}
                  </div>
                </div>
              ) : null}
            </section>

            <section className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <h3 className="text-sm font-semibold text-slate-950">
                    {locale === "zh" ? "目标间知识策略对比" : "Goal Strategy Comparison"}
                  </h3>
                  <p className="mt-1 text-sm text-slate-600">
                    {locale === "zh"
                      ? "比较两个目标的知识偏好与实际命中文档差异。"
                      : "Compare knowledge preferences and matched documents between two goals."}
                  </p>
                </div>
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
                  <select
                    className="h-10 min-w-56 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                    onChange={(event) => {
                      const nextGoalId = event.target.value;
                      setPreviewGoalId(nextGoalId);
                      if (nextGoalId) {
                        void loadGoalPreference(nextGoalId);
                      } else {
                        setGoalPreference(null);
                      }
                    }}
                    value={previewGoalId}
                  >
                    <option value="">
                      {locale === "zh" ? "左侧目标" : "Base goal"}
                    </option>
                    {initialGoals.map((goal) => (
                      <option key={`base-${goal.id}`} value={goal.id}>
                        {goal.title}
                      </option>
                    ))}
                  </select>
                  <select
                    className="h-10 min-w-56 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                    onChange={(event) => setCompareGoalId(event.target.value)}
                    value={compareGoalId}
                  >
                    <option value="">
                      {locale === "zh" ? "右侧目标" : "Compare goal"}
                    </option>
                    {initialGoals.map((goal) => (
                      <option key={`compare-${goal.id}`} value={goal.id}>
                        {goal.title}
                      </option>
                    ))}
                  </select>
                  <button
                    className="inline-flex h-10 items-center justify-center rounded-md bg-slate-950 px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-400"
                    disabled={comparisonLoading || !previewGoalId || !compareGoalId}
                    onClick={loadStrategyComparison}
                    type="button"
                  >
                    {comparisonLoading
                      ? locale === "zh"
                        ? "对比中"
                        : "Comparing"
                      : locale === "zh"
                        ? "开始对比"
                        : "Compare"}
                  </button>
                </div>
              </div>

              {strategyComparison ? (
                <div className="mt-4 space-y-4">
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-md border border-slate-200 bg-white p-4">
                      <div className="text-sm font-semibold text-slate-950">
                        {strategyComparison.baseGoalTitle}
                      </div>
                      <div className="mt-2 space-y-1 text-sm text-slate-600">
                        <p>
                          {locale === "zh" ? "优先文档数：" : "Preferred docs: "}
                          {strategyComparison.basePreference.preferredDocumentIds.length}
                        </p>
                        <p>
                          {locale === "zh" ? "固定作用域：" : "Fixed scope: "}
                          {strategyComparison.basePreference.preferredScope
                            ? knowledgeScopeLabel(
                                strategyComparison.basePreference.preferredScope,
                                locale,
                              )
                            : locale === "zh"
                              ? "未固定"
                              : "Not fixed"}
                        </p>
                      </div>
                    </div>
                    <div className="rounded-md border border-slate-200 bg-white p-4">
                      <div className="text-sm font-semibold text-slate-950">
                        {strategyComparison.compareGoalTitle}
                      </div>
                      <div className="mt-2 space-y-1 text-sm text-slate-600">
                        <p>
                          {locale === "zh" ? "优先文档数：" : "Preferred docs: "}
                          {strategyComparison.comparePreference.preferredDocumentIds.length}
                        </p>
                        <p>
                          {locale === "zh" ? "固定作用域：" : "Fixed scope: "}
                          {strategyComparison.comparePreference.preferredScope
                            ? knowledgeScopeLabel(
                                strategyComparison.comparePreference.preferredScope,
                                locale,
                              )
                            : locale === "zh"
                              ? "未固定"
                              : "Not fixed"}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="rounded-md border border-slate-200 bg-white p-4">
                    <div className="text-sm font-medium text-slate-500">
                      {locale === "zh" ? "主要差异" : "Main differences"}
                    </div>
                    <ul className="mt-3 space-y-2 text-sm text-slate-700">
                      {strategyComparison.differences.map((difference) => (
                        <li key={difference}>{difference}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="grid gap-4 xl:grid-cols-3">
                    <div className="rounded-md border border-slate-200 bg-white p-4">
                      <div className="text-sm font-medium text-slate-500">
                        {locale === "zh"
                          ? `仅 ${strategyComparison.baseGoalTitle} 命中`
                          : `Only ${strategyComparison.baseGoalTitle}`}
                      </div>
                      <div className="mt-3 space-y-2">
                        {strategyComparison.onlyInBase.length === 0 ? (
                          <p className="text-sm text-slate-500">
                            {locale === "zh" ? "无" : "None"}
                          </p>
                        ) : (
                          strategyComparison.onlyInBase.map((item) => (
                            <div
                              className="rounded-md border border-slate-200 bg-slate-50 p-3"
                              key={`base-only-${item.documentId}`}
                            >
                              <div className="text-sm font-semibold text-slate-900">
                                {item.title}
                              </div>
                              <div className="mt-1 text-xs text-slate-500">
                                {locale === "zh" ? "分数：" : "Score: "}
                                {item.baseScore ?? "-"}
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>

                    <div className="rounded-md border border-slate-200 bg-white p-4 xl:col-span-1">
                      <div className="text-sm font-medium text-slate-500">
                        {locale === "zh" ? "共享文档差异" : "Shared document deltas"}
                      </div>
                      <div className="mt-3 space-y-2">
                        {strategyComparison.sharedDocuments.length === 0 ? (
                          <p className="text-sm text-slate-500">
                            {locale === "zh" ? "无共享命中" : "No shared matches"}
                          </p>
                        ) : (
                          strategyComparison.sharedDocuments.map((item) => (
                            <div
                              className="rounded-md border border-slate-200 bg-slate-50 p-3"
                              key={`shared-${item.documentId}`}
                            >
                              <div className="text-sm font-semibold text-slate-900">
                                {item.title}
                              </div>
                              <div className="mt-1 text-xs text-slate-500">
                                {locale === "zh" ? "左侧：" : "Base: "}
                                {item.baseScore ?? "-"}
                                {" | "}
                                {locale === "zh" ? "右侧：" : "Compare: "}
                                {item.compareScore ?? "-"}
                              </div>
                              <div className="mt-1 text-xs text-slate-500">
                                {locale === "zh" ? "分差：" : "Delta: "}
                                {item.scoreDelta ?? "-"}
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>

                    <div className="rounded-md border border-slate-200 bg-white p-4">
                      <div className="text-sm font-medium text-slate-500">
                        {locale === "zh"
                          ? `仅 ${strategyComparison.compareGoalTitle} 命中`
                          : `Only ${strategyComparison.compareGoalTitle}`}
                      </div>
                      <div className="mt-3 space-y-2">
                        {strategyComparison.onlyInCompare.length === 0 ? (
                          <p className="text-sm text-slate-500">
                            {locale === "zh" ? "无" : "None"}
                          </p>
                        ) : (
                          strategyComparison.onlyInCompare.map((item) => (
                            <div
                              className="rounded-md border border-slate-200 bg-slate-50 p-3"
                              key={`compare-only-${item.documentId}`}
                            >
                              <div className="text-sm font-semibold text-slate-900">
                                {item.title}
                              </div>
                              <div className="mt-1 text-xs text-slate-500">
                                {locale === "zh" ? "分数：" : "Score: "}
                                {item.compareScore ?? "-"}
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ) : null}
            </section>

            <section className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <h3 className="text-sm font-semibold text-slate-950">
                    {locale === "zh" ? "批量治理" : "Batch Governance"}
                  </h3>
                  <p className="mt-1 text-sm text-slate-600">
                    {locale === "zh"
                      ? "可按来源分类、分组和标签统一治理多份资料。"
                      : "Manage multiple materials together by source category, group, and tags."}
                  </p>
                </div>
                <label className="inline-flex items-center gap-2 text-sm text-slate-700">
                  <input
                    checked={allSelected()}
                    onChange={(event) =>
                      setSelectedDocumentIds(
                        event.target.checked
                          ? filteredDocuments.map((document) => document.id)
                          : [],
                      )
                    }
                    type="checkbox"
                  />
                  {locale === "zh" ? "全选当前列表" : "Select all visible"}
                </label>
              </div>

              <div className="mt-4 grid gap-3 lg:grid-cols-3">
                <div className="grid gap-2">
                  <label className="text-xs font-medium text-slate-500">
                    {locale === "zh" ? "来源分类" : "Source Category"}
                  </label>
                  <select
                    className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                    onChange={(event) =>
                      setBatchCategory(event.target.value as KnowledgeSourceCategory)
                    }
                    value={batchCategory}
                  >
                    {(
                      ["NOTE", "RESUME", "PROJECT", "COURSE", "REFERENCE", "OTHER"] as const
                    ).map((category) => (
                      <option key={category} value={category}>
                        {knowledgeSourceCategoryLabel(category, locale)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="grid gap-2">
                  <label className="text-xs font-medium text-slate-500">
                    {locale === "zh" ? "分组" : "Group"}
                  </label>
                  <input
                    className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                    onChange={(event) => setBatchGroupName(event.target.value)}
                    placeholder={locale === "zh" ? "如：求职资料" : "e.g. Job search"}
                    value={batchGroupName}
                  />
                </div>
                <div className="grid gap-2">
                  <label className="text-xs font-medium text-slate-500">
                    {locale === "zh" ? "标签" : "Tags"}
                  </label>
                  <input
                    className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                    onChange={(event) => setBatchTags(event.target.value)}
                    placeholder={locale === "zh" ? "逗号分隔" : "Comma separated"}
                    value={batchTags}
                  />
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  className="inline-flex h-10 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700"
                  onClick={() => handleBatchUpdate({ enabled: true })}
                  type="button"
                >
                  {locale === "zh" ? "批量启用" : "Enable Selected"}
                </button>
                <button
                  className="inline-flex h-10 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700"
                  onClick={() => handleBatchUpdate({ enabled: false })}
                  type="button"
                >
                  {locale === "zh" ? "批量停用" : "Disable Selected"}
                </button>
                <button
                  className="inline-flex h-10 items-center justify-center rounded-md bg-slate-950 px-4 text-sm font-semibold text-white"
                  onClick={() =>
                    handleBatchUpdate({
                      sourceCategory: batchCategory,
                      groupName: batchGroupName,
                      tags: batchTags
                        .split(",")
                        .map((item) => item.trim())
                        .filter(Boolean),
                    })
                  }
                  type="button"
                >
                  {locale === "zh" ? "应用治理设置" : "Apply Governance"}
                </button>
              </div>
            </section>

            {filteredDocuments.map((document) => {
              const isPending = pendingDocumentIds.includes(document.id);
              return (
                <article
                  className="rounded-md border border-slate-200 p-4"
                  key={document.id}
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <label className="inline-flex items-center gap-2 text-xs text-slate-500">
                          <input
                            checked={selectedDocumentIds.includes(document.id)}
                            onChange={() => toggleSelection(document.id)}
                            type="checkbox"
                          />
                          {locale === "zh" ? "选择" : "Select"}
                        </label>
                        <span className="inline-flex h-7 items-center rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                          {knowledgeStatusLabel(document.status, locale)}
                        </span>
                        <span className="inline-flex h-7 items-center rounded-md bg-emerald-50 px-2 text-xs font-semibold text-emerald-700">
                          {knowledgeScopeLabel(document.scope, locale)}
                        </span>
                        <span className="inline-flex h-7 items-center rounded-md bg-violet-50 px-2 text-xs font-semibold text-violet-700">
                          {knowledgeSourceCategoryLabel(document.sourceCategory, locale)}
                        </span>
                        <span className="text-xs text-slate-500">
                          {formatFileSize(document.fileSizeBytes, locale)}
                        </span>
                        <span className="text-xs text-slate-500">
                          {locale === "zh"
                            ? `${document.chunkCount} 个块`
                            : `${document.chunkCount} chunks`}
                        </span>
                      </div>
                      <h3 className="mt-3 text-base font-semibold text-slate-950">
                        {document.title}
                      </h3>
                      <p className="mt-1 text-sm text-slate-500">
                        {document.originalFileName}
                      </p>
                      <p className="mt-3 text-sm leading-6 text-slate-600">
                        {document.summary ||
                          (locale === "zh"
                            ? "导入完成后会在这里显示摘要。"
                            : "A summary will appear here after ingestion completes.")}
                      </p>
                      <p className="mt-2 text-sm text-slate-500">
                        {retrievalHint(document)}
                      </p>
                      {document.groupName ? (
                        <p className="mt-2 text-sm text-slate-500">
                          {locale === "zh" ? "分组：" : "Group: "}
                          {document.groupName}
                        </p>
                      ) : null}
                      {document.tags.length > 0 ? (
                        <div className="mt-2 flex flex-wrap gap-2">
                          {document.tags.map((tag) => (
                            <span
                              className="inline-flex h-6 items-center rounded-md bg-slate-100 px-2 text-xs font-medium text-slate-600"
                              key={tag}
                            >
                              #{tag}
                            </span>
                          ))}
                        </div>
                      ) : null}
                    </div>

                    <div className="flex shrink-0 flex-col gap-3 lg:w-64">
                      <div className="grid gap-2">
                        <label className="text-xs font-medium text-slate-500">
                          {locale === "zh" ? "来源分类" : "Source Category"}
                        </label>
                        <select
                          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                          disabled={isPending}
                          onChange={(event) =>
                            handleMetadataChange(document, {
                              sourceCategory:
                                event.target.value as KnowledgeSourceCategory,
                            })
                          }
                          value={document.sourceCategory}
                        >
                          {(
                            ["NOTE", "RESUME", "PROJECT", "COURSE", "REFERENCE", "OTHER"] as const
                          ).map((category) => (
                            <option key={category} value={category}>
                              {knowledgeSourceCategoryLabel(category, locale)}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="grid gap-2">
                        <label className="text-xs font-medium text-slate-500">
                          {locale === "zh" ? "作用域" : "Scope"}
                        </label>
                        <select
                          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                          disabled={isPending}
                          onChange={(event) =>
                            handleSettingsChange(document, {
                              scope: event.target
                                .value as KnowledgeDocumentSummary["scope"],
                            })
                          }
                          value={document.scope}
                        >
                          <option value="PERSONAL">
                            {locale === "zh" ? "个人资料" : "Personal"}
                          </option>
                          <option value="PLATFORM">
                            {locale === "zh" ? "平台资料" : "Platform"}
                          </option>
                        </select>
                      </div>

                      <div className="grid gap-2">
                        <label className="text-xs font-medium text-slate-500">
                          {locale === "zh" ? "检索优先级" : "Retrieval Priority"}
                        </label>
                        <select
                          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                          disabled={isPending}
                          onChange={(event) =>
                            handleSettingsChange(document, {
                              retrievalPriority: Number(event.target.value),
                            })
                          }
                          value={document.retrievalPriority}
                        >
                          {[1, 2, 3, 4, 5].map((priority) => (
                            <option key={priority} value={priority}>
                              {knowledgePriorityLabel(priority, locale)}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="grid gap-2">
                        <label className="text-xs font-medium text-slate-500">
                          {locale === "zh" ? "分组" : "Group"}
                        </label>
                        <input
                          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                          defaultValue={document.groupName ?? ""}
                          disabled={isPending}
                          onBlur={(event) =>
                            handleMetadataChange(document, {
                              groupName: event.target.value,
                            })
                          }
                        />
                      </div>

                      <div className="grid gap-2">
                        <label className="text-xs font-medium text-slate-500">
                          {locale === "zh" ? "标签" : "Tags"}
                        </label>
                        <input
                          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700"
                          defaultValue={document.tags.join(", ")}
                          disabled={isPending}
                          onBlur={(event) =>
                            handleMetadataChange(document, {
                              tags: event.target.value
                                .split(",")
                                .map((item) => item.trim())
                                .filter(Boolean),
                            })
                          }
                          placeholder={locale === "zh" ? "逗号分隔" : "Comma separated"}
                        />
                      </div>

                      <button
                        className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
                        disabled={isPending}
                        onClick={() => handleToggle(document)}
                        type="button"
                      >
                        {isPending ? (
                          <LoaderCircle
                            aria-hidden="true"
                            className="size-4 animate-spin"
                          />
                        ) : document.enabled ? (
                          <CheckCircle2
                            aria-hidden="true"
                            className="size-4 text-emerald-600"
                          />
                        ) : (
                          <RefreshCcw aria-hidden="true" className="size-4" />
                        )}
                        {document.enabled
                          ? locale === "zh"
                            ? "停用"
                            : "Disable"
                          : locale === "zh"
                            ? "启用"
                            : "Enable"}
                      </button>
                      <Link
                        className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800"
                        href={`/knowledge/${document.id}`}
                      >
                        <FileText aria-hidden="true" className="size-4" />
                        {locale === "zh" ? "查看详情" : "View Details"}
                      </Link>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
