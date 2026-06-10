import { authHeadersFromCookies } from "./backend-auth";
import { getBackendBaseUrl } from "./backend-url";
import type {
  AgentRunDetail,
  AgentRunSummary,
  ApiErrorResponse,
} from "./goals";

type BackendResult<T> =
  | {
      data: T;
      error: null;
    }
  | {
      data: null;
      error: ApiErrorResponse;
    };

function buildError(message: string): ApiErrorResponse {
  return {
    errors: { backend: message },
    message,
    status: "BAD_GATEWAY",
    timestamp: new Date().toISOString(),
  };
}

async function parseJson(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    return null;
  }

  return response.json() as Promise<unknown>;
}

function normalizeError(payload: unknown, fallbackMessage: string) {
  if (payload && typeof payload === "object") {
    const maybeError = payload as ApiErrorResponse;
    if (maybeError.message || maybeError.errors || maybeError.status) {
      return maybeError;
    }
  }

  return buildError(fallbackMessage);
}

export async function fetchBackendAgentRuns(
  searchParams?: Record<string, string | undefined>,
): Promise<BackendResult<AgentRunSummary[]>> {
  try {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(searchParams ?? {})) {
      if (value) {
        params.set(key, value);
      }
    }
    const query = params.size > 0 ? `?${params.toString()}` : "";
    const response = await fetch(
      `${getBackendBaseUrl()}/api/agent-runs${query}`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );
    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend agent runs request failed."),
      };
    }

    return {
      data: Array.isArray(payload) ? (payload as AgentRunSummary[]) : [],
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend agent runs request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}

export async function fetchBackendAgentRun(
  runId: string,
): Promise<BackendResult<AgentRunDetail>> {
  try {
    const response = await fetch(
      `${getBackendBaseUrl()}/api/agent-runs/${runId}`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );
    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend agent run request failed."),
      };
    }

    return {
      data: payload as AgentRunDetail,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend agent run request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}
