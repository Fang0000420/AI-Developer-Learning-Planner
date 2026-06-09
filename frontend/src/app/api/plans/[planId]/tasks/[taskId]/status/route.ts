import { NextResponse } from "next/server";
import { getBackendBaseUrl } from "@/lib/backend-url";

export const dynamic = "force-dynamic";

function getIds(request: Request) {
  const parts = new URL(request.url).pathname.split("/").filter(Boolean);
  const plansIndex = parts.indexOf("plans");
  const tasksIndex = parts.indexOf("tasks");

  return {
    planId: plansIndex >= 0 ? parts.at(plansIndex + 1) : undefined,
    taskId: tasksIndex >= 0 ? parts.at(tasksIndex + 1) : undefined,
  };
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

export async function PUT(request: Request) {
  try {
    const { planId, taskId } = getIds(request);
    const response = await fetch(
      `${getBackendBaseUrl()}/api/plans/${planId}/tasks/${taskId}/status`,
      {
        body: await request.text(),
        cache: "no-store",
        headers: {
          "content-type":
            request.headers.get("content-type") ?? "application/json",
        },
        method: "PUT",
      },
    );

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
