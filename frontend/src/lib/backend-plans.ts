import { getBackendBaseUrl } from "./backend-url";
import type { ApiErrorResponse, LearningPlan } from "./goals";

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

export async function fetchBackendPlan(
  planId: string,
): Promise<BackendResult<LearningPlan>> {
  try {
    const response = await fetch(`${getBackendBaseUrl()}/api/plans/${planId}`, {
      cache: "no-store",
    });
    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend plan request failed."),
      };
    }

    return {
      data: payload as LearningPlan,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Backend plan request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}
