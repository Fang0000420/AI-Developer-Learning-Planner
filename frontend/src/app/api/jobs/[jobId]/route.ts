import { NextResponse } from "next/server";
import { getBackendBaseUrl } from "@/lib/backend-url";

export const dynamic = "force-dynamic";

type JobRouteProps = {
  params: Promise<{
    jobId: string;
  }>;
};

function backendUnavailableResponse(error: unknown) {
  const message =
    error instanceof Error ? error.message : "Backend job API is unavailable.";

  return NextResponse.json(
    {
      errors: { backend: message },
      message: "Backend job API is unavailable.",
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

export async function GET(_request: Request, { params }: JobRouteProps) {
  const { jobId } = await params;

  try {
    const response = await fetch(`${getBackendBaseUrl()}/api/jobs/${jobId}`, {
      cache: "no-store",
    });

    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
