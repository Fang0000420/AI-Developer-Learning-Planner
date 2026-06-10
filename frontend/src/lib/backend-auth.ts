import { cookies } from "next/headers";

export const AUTH_COOKIE_NAME = "ai_planner_token";

export function authHeadersFromRequest(
  request: Request,
): Record<string, string> {
  const authorization = request.headers.get("authorization");
  if (authorization) {
    return { Authorization: authorization };
  }

  const token = parseCookieHeader(request.headers.get("cookie") ?? "")[
    AUTH_COOKIE_NAME
  ];
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function authHeadersFromCookies(): Promise<
  Record<string, string>
> {
  const token = (await cookies()).get(AUTH_COOKIE_NAME)?.value;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function authCookieAttributes(maxAgeSeconds = 60 * 60 * 24) {
  return {
    httpOnly: true,
    maxAge: maxAgeSeconds,
    path: "/",
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
  };
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
