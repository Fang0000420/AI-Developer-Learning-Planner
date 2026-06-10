import { NextResponse } from "next/server";
import { authHeadersFromRequest } from "@/lib/backend-auth";
import { getBackendBaseUrl } from "@/lib/backend-url";

export const dynamic = "force-dynamic";

function getGoalId(request: Request) {
  const pathname = new URL(request.url).pathname;
  return pathname.split("/").filter(Boolean).at(-1);
}

function backendUnavailableResponse(error: unknown) {
  const message =
    error instanceof Error
      ? error.message
      : "Backend goals API is unavailable.";

  return NextResponse.json(
    {
      errors: { backend: message },
      message: "Backend goals API is unavailable.",
      status: "BAD_GATEWAY",
      timestamp: new Date().toISOString(),
    },
    { status: 502 },
  );
}

async function proxyJsonResponse(response: Response) {
  if (response.status === 204) {
    return new Response(null, { status: 204 });
  }

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
    const goalId = getGoalId(request);
    const response = await fetch(`${getBackendBaseUrl()}/api/goals/${goalId}`, {
      cache: "no-store",
      headers: authHeadersFromRequest(request),
    });

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}

export async function PUT(request: Request) {
  try {
    const goalId = getGoalId(request);
    const response = await fetch(`${getBackendBaseUrl()}/api/goals/${goalId}`, {
      body: await request.text(),
      cache: "no-store",
      headers: {
        ...authHeadersFromRequest(request),
        "content-type":
          request.headers.get("content-type") ?? "application/json",
      },
      method: "PUT",
    });

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}

export async function DELETE(request: Request) {
  try {
    const goalId = getGoalId(request);
    const response = await fetch(`${getBackendBaseUrl()}/api/goals/${goalId}`, {
      cache: "no-store",
      headers: authHeadersFromRequest(request),
      method: "DELETE",
    });

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
