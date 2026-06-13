import Link from "next/link";
import { ArrowLeft, BookText, CheckCircle2 } from "lucide-react";
import { fetchBackendKnowledgeDocument } from "@/lib/backend-knowledge";
import { formatGoalDate } from "@/lib/goals";
import {
  formatFileSize,
  knowledgeStatusLabel,
} from "@/lib/knowledge";
import { dictionaries } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";

export const dynamic = "force-dynamic";

type KnowledgeDetailPageProps = {
  params: Promise<{
    documentId: string;
  }>;
};

export default async function KnowledgeDetailPage({
  params,
}: KnowledgeDetailPageProps) {
  const { documentId } = await params;
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
  const { data: document, error } = await fetchBackendKnowledgeDocument(documentId);

  if (error || !document) {
    return (
      <main className="flex-1 bg-background text-foreground">
        <section className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
          <Link
            className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
            href="/knowledge"
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            {locale === "zh" ? "知识库" : "Knowledge Base"}
          </Link>
          <section className="mt-6 rounded-md border border-rose-200 bg-rose-50 p-5">
            <h1 className="text-xl font-semibold text-rose-950">
              {locale === "zh" ? "文档不可用" : "Document unavailable"}
            </h1>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {error?.message ||
                (locale === "zh"
                  ? "无法加载请求的知识库文档。"
                  : "The requested knowledge document could not be loaded.")}
            </p>
          </section>
        </section>
      </main>
    );
  }

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
        <Link
          className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
          href="/knowledge"
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          {locale === "zh" ? "知识库" : "Knowledge Base"}
        </Link>

        <section className="mt-6 rounded-md border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="inline-flex h-7 items-center rounded-md bg-slate-100 px-2 text-xs font-semibold text-slate-600">
                  {knowledgeStatusLabel(document.status, locale)}
                </span>
                {document.enabled ? (
                  <span className="inline-flex h-7 items-center gap-1 rounded-md bg-emerald-50 px-2 text-xs font-semibold text-emerald-700">
                    <CheckCircle2 aria-hidden="true" className="size-3.5" />
                    {locale === "zh" ? "已启用" : "Enabled"}
                  </span>
                ) : null}
              </div>
              <h1 className="mt-4 text-3xl font-semibold text-slate-950">
                {document.title}
              </h1>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                {document.summary ||
                  (locale === "zh"
                    ? "导入完成后会生成摘要。"
                    : "A summary appears after ingestion completes.")}
              </p>
            </div>

            <span className="flex size-12 items-center justify-center rounded-md bg-sky-50 text-sky-700">
              <BookText aria-hidden="true" className="size-6" />
            </span>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-4">
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-500">
                {locale === "zh" ? "文件" : "File"}
              </p>
              <p className="mt-2 text-sm font-semibold text-slate-950">
                {document.originalFileName}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-500">
                {locale === "zh" ? "大小" : "Size"}
              </p>
              <p className="mt-2 text-sm font-semibold text-slate-950">
                {formatFileSize(document.fileSizeBytes, locale)}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-500">
                {locale === "zh" ? "知识块" : "Chunks"}
              </p>
              <p className="mt-2 text-sm font-semibold text-slate-950">
                {document.chunkCount}
              </p>
            </div>
            <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-500">
                {t.common.updated}
              </p>
              <p className="mt-2 text-sm font-semibold text-slate-950">
                {formatGoalDate(document.updatedAt, locale)}
              </p>
            </div>
          </div>
        </section>

        <section className="mt-6 rounded-md border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-950">
            {locale === "zh" ? "文本预览" : "Text Preview"}
          </h2>
          <pre className="mt-4 whitespace-pre-wrap break-words rounded-md bg-slate-50 p-4 text-sm leading-7 text-slate-700">
            {document.previewText ||
              (locale === "zh"
                ? "当前还没有可预览的文本内容。"
                : "No preview text is available yet.")}
          </pre>
        </section>
      </section>
    </main>
  );
}
