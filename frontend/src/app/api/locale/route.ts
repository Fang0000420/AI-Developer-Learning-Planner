import { NextResponse } from "next/server";
import { LOCALE_COOKIE, normalizeLocale } from "@/lib/i18n";

export async function POST(request: Request) {
  const body = (await request.json().catch(() => ({}))) as {
    locale?: string;
  };
  const locale = normalizeLocale(body.locale);
  const response = NextResponse.json({ locale });
  response.cookies.set(LOCALE_COOKIE, locale, {
    httpOnly: false,
    maxAge: 60 * 60 * 24 * 365,
    path: "/",
    sameSite: "lax",
  });
  return response;
}
