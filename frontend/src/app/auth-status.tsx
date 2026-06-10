"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, User } from "lucide-react";

type AuthStatusProps = {
  signOutLabel: string;
  username: string;
};

export function AuthStatus({ signOutLabel, username }: AuthStatusProps) {
  const router = useRouter();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  async function logout() {
    setIsLoggingOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
    router.refresh();
  }

  return (
    <div className="flex h-10 items-center gap-2 rounded-md bg-slate-50 px-3 text-sm font-medium text-slate-700">
      <User aria-hidden="true" className="size-4" />
      <span className="max-w-36 truncate">{username}</span>
      <button
        aria-label={signOutLabel}
        className="inline-flex size-7 items-center justify-center rounded text-slate-500 transition hover:bg-slate-200 hover:text-slate-950 disabled:opacity-50"
        disabled={isLoggingOut}
        onClick={logout}
        type="button"
      >
        <LogOut aria-hidden="true" className="size-4" />
      </button>
    </div>
  );
}
