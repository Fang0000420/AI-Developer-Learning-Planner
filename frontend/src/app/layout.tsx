import type { Metadata } from "next";
import Link from "next/link";
import {
  Activity,
  BookOpenText,
  CalendarCheck,
  LayoutDashboard,
  ListChecks,
  LogIn,
  Plus,
  Target,
} from "lucide-react";
import { authDisplayFromCookies } from "@/lib/backend-auth";
import { dictionaries } from "@/lib/i18n";
import { getCurrentLocale } from "@/lib/i18n-server";
import { AuthStatus } from "./auth-status";
import "./globals.css";
import { LanguageSwitcher } from "./language-switcher";
import { ThemeToggle } from "./theme-toggle";

export const metadata: Metadata = {
  title: "AI Developer Learning Planner",
  description: "Frontend workspace for the AI learning planner MVP.",
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const auth = await authDisplayFromCookies();
  const locale = await getCurrentLocale();
  const t = dictionaries[locale];
  const navigationItems = [
    { href: "/", label: t.nav.dashboard, icon: LayoutDashboard },
    { href: "/goals", label: t.nav.goals, icon: Target },
    { href: "/goals/new", label: t.nav.newGoal, icon: Plus },
    { href: "/plans", label: t.nav.plans, icon: ListChecks },
    { href: "/knowledge", label: t.nav.knowledge, icon: BookOpenText },
    { href: "/tasks/today", label: t.nav.today, icon: CalendarCheck },
    { href: "/agent-runs", label: t.nav.agentRuns, icon: Activity },
    ...(auth ? [] : [{ href: "/login", label: t.nav.login, icon: LogIn }]),
  ];

  return (
    <html
      lang={locale === "zh" ? "zh-CN" : "en"}
      className="h-full antialiased"
    >
      <body className="flex min-h-full flex-col bg-background text-foreground transition-colors">
        <header className="border-b border-slate-200 bg-white transition-colors dark:border-slate-800 dark:bg-slate-950">
          <div className="mx-auto flex min-h-16 w-full max-w-7xl flex-col gap-3 px-4 py-3 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
            <Link className="flex items-center gap-3" href="/">
              <span className="flex size-9 items-center justify-center rounded-md bg-slate-950 text-sm font-semibold text-white">
                AI
              </span>
              <span className="flex flex-col">
                <span className="text-sm font-semibold text-slate-950">
                  AI Developer Learning Planner
                </span>
                <span className="text-xs text-slate-500">
                  {t.nav.workspace}
                </span>
              </span>
            </Link>

            <nav aria-label="Primary navigation">
              <ul className="flex flex-wrap gap-1">
                {navigationItems.map((item) => {
                  const Icon = item.icon;

                  return (
                    <li key={item.href}>
                      <Link
                        className="inline-flex h-10 items-center gap-2 rounded-md px-3 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950"
                        href={item.href}
                      >
                        <Icon aria-hidden="true" className="size-4" />
                        {item.label}
                      </Link>
                    </li>
                  );
                })}
                <li>
                  <div className="flex items-center gap-2">
                    <LanguageSwitcher locale={locale} />
                    <ThemeToggle locale={locale} />
                  </div>
                </li>
                {auth ? (
                  <li>
                    <AuthStatus
                      signOutLabel={t.nav.signOut}
                      username={auth.username}
                    />
                  </li>
                ) : null}
              </ul>
            </nav>
          </div>
        </header>

        {children}
      </body>
    </html>
  );
}
