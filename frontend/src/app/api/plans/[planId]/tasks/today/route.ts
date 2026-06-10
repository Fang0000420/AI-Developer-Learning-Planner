import { NextResponse } from "next/server";
import { authHeadersFromRequest } from "@/lib/backend-auth";
import { getBackendBaseUrl } from "@/lib/backend-url";

export const dynamic = "force-dynamic";

function getPlanId(request: Request) {
  const parts = new URL(request.url).pathname.split("/").filter(Boolean);
  const plansIndex = parts.indexOf("plans");
  return plansIndex >= 0 ? parts.at(plansIndex + 1) : undefined;
}

function backendUnavailableResponse(error: unknown) {
  const message =
    error instanceof Error
      ? error.message
      : "Backend daily task API is unavailable.";

  return NextResponse.json(
    {
      errors: { backend: message },
      message: "Backend daily task API is unavailable.",
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
    const planId = getPlanId(request);
    const { search } = new URL(request.url);
    const response = await fetch(
      `${getBackendBaseUrl()}/api/plans/${planId}/tasks/today${search}`,
      {
        cache: "no-store",
        headers: authHeadersFromRequest(request),
      },
    );

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
