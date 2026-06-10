"use client";

import { useRouter } from "next/navigation";
import type { Locale } from "@/lib/i18n";

type LanguageSwitcherProps = {
  locale: Locale;
};

export function LanguageSwitcher({ locale }: LanguageSwitcherProps) {
  const router = useRouter();

  async function switchLocale(nextLocale: Locale) {
    if (nextLocale === locale) {
      return;
    }
    await fetch("/api/locale", {
      body: JSON.stringify({ locale: nextLocale }),
      headers: { "Content-Type": "application/json" },
      method: "POST",
    });
    router.refresh();
  }

  return (
    <div
      aria-label="Language"
      className="grid h-10 grid-cols-2 rounded-md border border-slate-200 bg-slate-50 p-1"
    >
      {(["zh", "en"] as const).map((item) => (
        <button
          className={`rounded px-2 text-xs font-semibold transition ${
            locale === item
              ? "bg-slate-950 text-white"
              : "text-slate-600 hover:bg-white hover:text-slate-950"
          }`}
          key={item}
          onClick={() => switchLocale(item)}
          type="button"
        >
          {item === "zh" ? "CN" : "EN"}
        </button>
      ))}
    </div>
  );
}
