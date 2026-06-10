"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogIn, UserPlus } from "lucide-react";
import type { ApiErrorResponse } from "@/lib/goals";
import { dictionaries, type Locale } from "@/lib/i18n";

type Mode = "login" | "register";

function errorMessage(payload: ApiErrorResponse, fallback: string) {
  const firstError = payload.errors ? Object.values(payload.errors)[0] : null;
  return firstError || payload.message || fallback;
}

type AuthFormProps = {
  locale: Locale;
};

export function AuthForm({ locale }: AuthFormProps) {
  const t = dictionaries[locale].auth;
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);

    const response = await fetch(`/api/auth/${mode}`, {
      body: JSON.stringify(
        mode === "register"
          ? { username, email: email || undefined, password }
          : { username, password },
      ),
      headers: { "Content-Type": "application/json" },
      method: "POST",
    });
    const payload = await response.json();

    if (!response.ok) {
      setError(errorMessage(payload as ApiErrorResponse, t.failed));
      setIsSubmitting(false);
      return;
    }

    router.push("/goals");
    router.refresh();
  }

  const isLogin = mode === "login";
  const Icon = isLogin ? LogIn : UserPlus;

  return (
    <form
      className="mx-auto flex w-full max-w-md flex-col gap-5 rounded-md border border-slate-200 bg-white p-6 shadow-sm"
      onSubmit={submit}
    >
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-slate-950">
            {isLogin ? t.signIn : t.createAccount}
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {isLogin ? t.loginDescription : t.registerDescription}
          </p>
        </div>
        <span className="flex size-10 items-center justify-center rounded-md bg-slate-950 text-white">
          <Icon aria-hidden="true" className="size-5" />
        </span>
      </div>

      <div className="grid grid-cols-2 rounded-md border border-slate-200 p-1">
        <button
          className={`rounded px-3 py-2 text-sm font-medium ${
            isLogin ? "bg-slate-950 text-white" : "text-slate-600"
          }`}
          onClick={() => setMode("login")}
          type="button"
        >
          {t.signIn}
        </button>
        <button
          className={`rounded px-3 py-2 text-sm font-medium ${
            !isLogin ? "bg-slate-950 text-white" : "text-slate-600"
          }`}
          onClick={() => setMode("register")}
          type="button"
        >
          {t.register}
        </button>
      </div>

      <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
        {t.username}
        <input
          className="h-11 rounded-md border border-slate-300 px-3 text-sm outline-none transition focus:border-slate-950"
          maxLength={100}
          minLength={3}
          onChange={(event) => setUsername(event.target.value)}
          required
          value={username}
        />
      </label>

      {!isLogin ? (
        <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
          {t.email}
          <input
            className="h-11 rounded-md border border-slate-300 px-3 text-sm outline-none transition focus:border-slate-950"
            maxLength={255}
            onChange={(event) => setEmail(event.target.value)}
            type="email"
            value={email}
          />
        </label>
      ) : null}

      <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
        {t.password}
        <input
          className="h-11 rounded-md border border-slate-300 px-3 text-sm outline-none transition focus:border-slate-950"
          maxLength={100}
          minLength={8}
          onChange={(event) => setPassword(event.target.value)}
          required
          type="password"
          value={password}
        />
      </label>

      {error ? (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      ) : null}

      <button
        className="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        disabled={isSubmitting}
        type="submit"
      >
        <Icon aria-hidden="true" className="size-4" />
        {isSubmitting ? t.working : isLogin ? t.signIn : t.register}
      </button>
    </form>
  );
}
