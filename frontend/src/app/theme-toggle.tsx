"use client";

import { Moon, Sun } from "lucide-react";
import { useEffect, useState } from "react";
import type { Locale } from "@/lib/i18n";

type Theme = "light" | "dark";

type ThemeToggleProps = {
  locale: Locale;
};

const STORAGE_KEY = "ai-learning-planner-theme";

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  root.classList.toggle("dark", theme === "dark");
  root.dataset.theme = theme;
}

export function ThemeToggle({ locale }: ThemeToggleProps) {
  const [mounted, setMounted] = useState(false);
  const [theme, setTheme] = useState<Theme>("light");

  useEffect(() => {
    const savedTheme = window.localStorage.getItem(STORAGE_KEY);
    const nextTheme: Theme =
      savedTheme === "dark" || savedTheme === "light"
        ? savedTheme
        : window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light";

    applyTheme(nextTheme);
    setTheme(nextTheme);
    setMounted(true);
  }, []);

  function toggleTheme() {
    const nextTheme: Theme = theme === "light" ? "dark" : "light";
    applyTheme(nextTheme);
    window.localStorage.setItem(STORAGE_KEY, nextTheme);
    setTheme(nextTheme);
  }

  const isDark = theme === "dark";
  const label = locale === "zh"
    ? isDark
      ? "切换到浅色模式"
      : "切换到深色模式"
    : isDark
      ? "Switch to light mode"
      : "Switch to dark mode";

  return (
    <button
      aria-label={label}
      className="inline-flex h-10 w-10 items-center justify-center rounded-md border border-slate-200 bg-slate-50 text-slate-600 transition-colors hover:bg-white hover:text-slate-950 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 dark:hover:text-white"
      disabled={!mounted}
      onClick={toggleTheme}
      type="button"
    >
      {isDark ? (
        <Moon aria-hidden="true" className="size-4" />
      ) : (
        <Sun aria-hidden="true" className="size-4" />
      )}
    </button>
  );
}
