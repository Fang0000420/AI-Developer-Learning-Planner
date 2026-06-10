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
      : "Backend learning plan API is unavailable.";

  return NextResponse.json(
    {
      errors: { backend: message },
      message: "Backend learning plan API is unavailable.",
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
    const response = await fetch(`${getBackendBaseUrl()}/api/plans/${planId}`, {
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
    const planId = getPlanId(request);
    const response = await fetch(`${getBackendBaseUrl()}/api/plans/${planId}`, {
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
    const planId = getPlanId(request);
    const response = await fetch(`${getBackendBaseUrl()}/api/plans/${planId}`, {
      cache: "no-store",
      headers: authHeadersFromRequest(request),
      method: "DELETE",
    });

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
