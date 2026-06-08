import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

type BackendHealthResponse = {
  service?: string;
  status?: string;
};

function getBackendBaseUrl() {
  return (
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    process.env.BACKEND_BASE_URL ||
    "http://localhost:8080"
  ).replace(/\/$/, "");
}

export async function GET() {
  const backendBaseUrl = getBackendBaseUrl();
  const checkedAt = new Date().toISOString();
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 3000);

  try {
    const response = await fetch(`${backendBaseUrl}/api/health`, {
      cache: "no-store",
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`Backend health returned HTTP ${response.status}`);
    }

    const health = (await response.json()) as BackendHealthResponse;
    const status = health.status ?? "UNKNOWN";

    return NextResponse.json({
      backendBaseUrl,
      checkedAt,
      online: status === "UP",
      service: health.service ?? "backend",
      status,
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Backend health request failed";

    return NextResponse.json({
      backendBaseUrl,
      checkedAt,
      error: message,
      online: false,
      service: "backend",
      status: "DOWN",
    });
  } finally {
    clearTimeout(timeoutId);
  }
}
