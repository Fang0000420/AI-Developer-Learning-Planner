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
    const response = await fetch(`${getBackendBaseUrl()}/api/knowledge/documents`, {
      cache: "no-store",
      headers: authHeadersFromRequest(request),
    });
    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}

export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const response = await fetch(`${getBackendBaseUrl()}/api/knowledge/documents`, {
      body: formData,
      cache: "no-store",
      headers: authHeadersFromRequest(request),
      method: "POST",
    });
    return proxyJsonResponse(response);
  } catch (error) {
    return backendUnavailableResponse(error);
  }
}
