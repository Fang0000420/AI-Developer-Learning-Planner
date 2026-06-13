import { NextResponse } from "next/server";
import { authHeadersFromRequest } from "@/lib/backend-auth";
import { getBackendBaseUrl } from "@/lib/backend-url";

export const dynamic = "force-dynamic";

function backendUnavailableResponse(error: unknown) {
  const message =
    error instanceof Error
      ? error.message
      : "Backend knowledge API is unavailable.";

  return NextResponse.json(
    {
      errors: { backend: message },
      message: "Backend knowledge API is unavailable.",
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
    const baseGoalId = url.searchParams.get("baseGoalId");
    const compareGoalId = url.searchParams.get("compareGoalId");
    const response = await fetch(
      `${getBackendBaseUrl()}/api/knowledge/documents/strategy-compare?baseGoalId=${baseGoalId}&compareGoalId=${compareGoalId}`,
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
