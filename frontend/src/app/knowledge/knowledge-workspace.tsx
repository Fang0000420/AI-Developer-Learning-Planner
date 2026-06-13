"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  BookOpen,
  CheckCircle2,
  FileText,
  LoaderCircle,
  RefreshCcw,
  Upload,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { getApiErrorMessage, pollJob } from "@/lib/client-jobs";
import type { ApiErrorResponse } from "@/lib/goals";
import {
  formatFileSize,
  knowledgeStatusLabel,
  type KnowledgeDocumentSummary,
  type KnowledgeUploadResponse,
} from "@/lib/knowledge";
import type { Locale } from "@/lib/i18n";

type KnowledgeWorkspaceProps = {
  initialDocuments: KnowledgeDocumentSummary[];
  initialError?: string | null;
  locale: Locale;
};

export function KnowledgeWorkspace({
  initialDocuments,
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

  const sortedDocuments = useMemo(
    () =>
      [...documents].sort((left, right) => {
        const rightValue = right.createdAt ? Date.parse(right.createdAt) : 0;
        const leftValue = left.createdAt ? Date.parse(left.createdAt) : 0;
        return rightValue - leftValue;
      }),
    [documents],
  );

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
                ? "这里展示文档的导入状态、可用块数量以及是否参与后续路径判断。"
                : "This list shows ingestion status, available chunk count, and whether the document can participate in later path decisions."}
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
            {sortedDocuments.map((document) => {
              const isPending = pendingDocumentIds.includes(document.id);
              return (
                <article
                  className="rounded-md border border-slate-200 p-4"
                  key={document.id}
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="inline-flex h-7 items-center rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                          {knowledgeStatusLabel(document.status, locale)}
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
                    </div>

                    <div className="flex shrink-0 flex-col gap-2 lg:w-44">
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
