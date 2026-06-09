import { NextResponse } from "next/server";
import { getBackendBaseUrl } from "@/lib/backend-url";

export const dynamic = "force-dynamic";

function getPlanId(request: Request) {
  const parts = new URL(request.url).pathname.split("/").filter(Boolean);
  const progressIndex = parts.indexOf("progress");
  return progressIndex >= 0 ? parts.at(progressIndex + 1) : undefined;
}

function backendUnavailableResponse(error: unknown) {
  const message =
    error instanceof Error
      ? error.message
      : "Backend progress API is unavailable.";

  return NextResponse.json(
    {
      errors: { backend: message },
      message: "Backend progress API is unavailable.",
      status: "BAD_GATEWAY",
      timestamp: new Date().toISOString(),
    },
    { status: 502 },
  );
}

async function proxyJsonResponse(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : {
        message: await response.text(),
        status: response.ok ? "OK" : "ERROR",
      };

  return NextResponse.json(payload, { status: response.status });
}

export async function GET(request: Request) {
  try {
    const url = new URL(request.url);
    const planId = getPlanId(request);
    const response = await fetch(
      `${getBackendBaseUrl()}/api/progress/${planId}${url.search}`,
      {
        cache: "no-store",
      },
    );

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
