import { cookies } from "next/headers";

export const AUTH_COOKIE_NAME = "ai_planner_token";
export const AUTH_USERNAME_COOKIE_NAME = "ai_planner_username";

export function authHeadersFromRequest(
  request: Request,
): Record<string, string> {
  const headers: Record<string, string> = {
    "X-Request-Id": request.headers.get("X-Request-Id") ?? crypto.randomUUID(),
  };
  const authorization = request.headers.get("authorization");
  if (authorization) {
    return { ...headers, Authorization: authorization };
  }

  const token = parseCookieHeader(request.headers.get("cookie") ?? "")[
    AUTH_COOKIE_NAME
  ];
  return token ? { ...headers, Authorization: `Bearer ${token}` } : headers;
}

export async function authHeadersFromCookies(): Promise<
  Record<string, string>
> {
  const token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
  const headers: Record<string, string> = {
    "X-Request-Id": crypto.randomUUID(),
  };
  return token ? { ...headers, Authorization: `Bearer ${token}` } : headers;
}

export async function authDisplayFromCookies() {
  const cookieStore = await cookies();
  const token = cookieStore.get(AUTH_COOKIE_NAME)?.value;
  const username = cookieStore.get(AUTH_USERNAME_COOKIE_NAME)?.value;

  return token && username ? { username } : null;
}

export function authCookieAttributes(
  maxAgeSeconds = 60 * 60 * 24,
  httpOnly = true,
) {
  return {
    httpOnly,
    maxAge: maxAgeSeconds,
    path: "/",
    sameSite: "lax" as const,
    secure: shouldUseSecureCookie(),
  };
}

function shouldUseSecureCookie() {
  const explicit = process.env.AUTH_COOKIE_SECURE;
  if (explicit === "true") {
    return true;
  }
  if (explicit === "false") {
    return false;
  }

  const frontendUrl =
    process.env.FRONTEND_BASE_URL ?? process.env.NEXT_PUBLIC_FRONTEND_BASE_URL;
  return frontendUrl ? frontendUrl.startsWith("https://") : false;
}

function parseCookieHeader(header: string) {
  return header.split(";").reduce<Record<string, string>>((cookies, part) => {
    const [rawName, ...rawValue] = part.trim().split("=");
    if (!rawName) {
      return cookies;
    }
    cookies[rawName] = decodeURIComponent(rawValue.join("="));
    return cookies;
  }, {});
}
