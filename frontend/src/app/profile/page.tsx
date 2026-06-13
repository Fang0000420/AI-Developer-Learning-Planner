import Link from "next/link";
import { ArrowLeft, Brain, Target } from "lucide-react";
import { fetchBackendCurrentUserProfile } from "@/lib/backend-profile";
import { dictionaries } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";
import { ProfileWorkspace } from "./profile-workspace";

export const dynamic = "force-dynamic";

export default async function ProfilePage() {
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
  const { data: profile, error } = await fetchBackendCurrentUserProfile();

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
            <p className="mt-5 inline-flex h-8 items-center gap-2 rounded-md bg-violet-50 px-3 text-sm font-medium text-violet-700">
              <Brain aria-hidden="true" className="size-4" />
              {locale === "zh" ? "动态画像" : "Dynamic Profile"}
            </p>
            <h1 className="mt-4 text-3xl font-semibold text-slate-950">
              {locale === "zh" ? "用户画像" : "User Profile"}
            </h1>
            <p className="mt-3 max-w-3xl text-base leading-7 text-slate-600">
              {locale === "zh"
                ? "这里展示会随着目标分析持续更新的长期画像，并保留最近几个目标快照，帮助你看到系统如何理解你的基础、节奏和风险点。"
                : "This page shows the long-term profile that evolves with goal analysis and preserves recent goal snapshots so you can see how the system reads your foundation, pace, and risk signals."}
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

        <ProfileWorkspace
          initialError={error?.message ?? null}
          initialProfile={profile}
          locale={locale}
        />
      </section>
    </main>
  );
}
