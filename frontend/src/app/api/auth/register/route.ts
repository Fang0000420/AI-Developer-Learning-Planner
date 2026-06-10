import { NextResponse } from "next/server";
import {
  AUTH_COOKIE_NAME,
  AUTH_USERNAME_COOKIE_NAME,
  authCookieAttributes,
} from "@/lib/backend-auth";
import { getBackendBaseUrl } from "@/lib/backend-url";
import type { AuthResponse } from "@/lib/auth";

export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  try {
    const response = await fetch(`${getBackendBaseUrl()}/api/auth/register`, {
      body: await request.text(),
      cache: "no-store",
      headers: {
        "content-type":
          request.headers.get("content-type") ?? "application/json",
      },
      method: "POST",
    });
    const payload = await response.json();
    const nextResponse = NextResponse.json(payload, {
      status: response.status,
    });

    if (response.ok) {
      const auth = payload as AuthResponse;
      nextResponse.cookies.set(
        AUTH_COOKIE_NAME,
        auth.token,
        authCookieAttributes(),
      );
      nextResponse.cookies.set(
        AUTH_USERNAME_COOKIE_NAME,
        auth.username,
        authCookieAttributes(undefined, true),
      );
    }

    return nextResponse;
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend auth API is unavailable.";
    return NextResponse.json(
      {
        errors: { backend: message },
        message: "Backend auth API is unavailable.",
        status: "BAD_GATEWAY",
        timestamp: new Date().toISOString(),
      },
      { status: 502 },
    );
  }
}
