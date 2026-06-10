import { AuthForm } from "./auth-form";
import { getCurrentLocale } from "@/lib/i18n-server";

export default async function LoginPage() {
  const locale = await getCurrentLocale();

  return (
    <main className="flex flex-1 items-center bg-slate-50 px-4 py-12 sm:px-6 lg:px-8">
      <AuthForm locale={locale} />
    </main>
  );
}
