import type { Metadata } from "next";
import Link from "next/link";
import {
  CalendarCheck,
  LayoutDashboard,
  ListChecks,
  LogIn,
  Plus,
  Target,
} from "lucide-react";
import "./globals.css";

export const metadata: Metadata = {
  title: "AI Developer Learning Planner",
  description: "Frontend workspace for the AI learning planner MVP.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const navigationItems = [
    { href: "/", label: "Dashboard", icon: LayoutDashboard },
    { href: "/goals", label: "Goals", icon: Target },
    { href: "/goals/new", label: "New Goal", icon: Plus },
    { href: "/plans", label: "Plans", icon: ListChecks },
    { href: "/tasks/today", label: "Today", icon: CalendarCheck },
    { href: "/login", label: "Login", icon: LogIn },
  ];

  return (
    <html lang="en" className="h-full antialiased">
      <body className="flex min-h-full flex-col">
        <header className="border-b border-slate-200 bg-white">
          <div className="mx-auto flex min-h-16 w-full max-w-7xl flex-col gap-3 px-4 py-3 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
            <Link className="flex items-center gap-3" href="/">
              <span className="flex size-9 items-center justify-center rounded-md bg-slate-950 text-sm font-semibold text-white">
                AI
              </span>
              <span className="flex flex-col">
                <span className="text-sm font-semibold text-slate-950">
                  AI Developer Learning Planner
                </span>
                <span className="text-xs text-slate-500">MVP workspace</span>
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
              </ul>
            </nav>
          </div>
        </header>

        {children}
      </body>
    </html>
  );
}
