import Link from "next/link";
import { ArrowLeft, BookOpenText, Target } from "lucide-react";
import { fetchBackendGoals } from "@/lib/backend-goals";
import { fetchBackendKnowledgeDocuments } from "@/lib/backend-knowledge";
import { dictionaries } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";
import { KnowledgeWorkspace } from "./knowledge-workspace";

export const dynamic = "force-dynamic";

type KnowledgePageProps = {
  searchParams?: Promise<Record<string, string | string[] | undefined>>;
};

export default async function KnowledgePage({
  searchParams,
}: KnowledgePageProps) {
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
  const resolvedSearchParams = searchParams ? await searchParams : {};
  const initialGoalId =
    typeof resolvedSearchParams.goalId === "string"
      ? resolvedSearchParams.goalId
      : "";
  const [{ data: documents, error }, { data: goals }] = await Promise.all([
    fetchBackendKnowledgeDocuments(),
    fetchBackendGoals(),
  ]);

  return (
    <main className="flex-1 bg-background text-foreground">
      <section className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <Link
              className="inline-flex h-10 items-center gap-2 rounded-md text-sm font-medium text-slate-600 transition-colors hover:text-slate-950"
              href="/"
            >
              <ArrowLeft aria-hidden="true" className="size-4" />
              {t.common.dashboard}
            </Link>
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-sky-50 px-3 text-sm font-medium text-sky-700">
              <BookOpenText aria-hidden="true" className="size-4" />
              {locale === "zh" ? "知识库" : "Knowledge Base"}
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              {locale === "zh" ? "个人知识库" : "Personal Knowledge Base"}
            </h1>
            <p className="mt-3 max-w-3xl text-base leading-7 text-slate-600">
              {locale === "zh"
                ? "把学习笔记、简历、项目总结和资料摘录导入系统，作为后续画像分析、路径推荐和计划生成的证据基础。"
                : "Import notes, résumés, project summaries, and reference excerpts so they can become evidence for future profile analysis, path recommendation, and plan generation."}
            </p>
          </div>

          <Link
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-slate-800"
            href="/goals"
          >
            <Target aria-hidden="true" className="size-4" />
            {locale === "zh" ? "打开目标" : "Open Goals"}
          </Link>
        </div>

        <KnowledgeWorkspace
          initialDocuments={documents}
          initialGoals={goals ?? []}
          initialSelectedGoalId={initialGoalId}
          initialError={error?.message ?? null}
          locale={locale}
        />
      </section>
    </main>
  );
}
