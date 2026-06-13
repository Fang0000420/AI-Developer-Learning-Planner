import { authHeadersFromCookies } from "./backend-auth";
import { getBackendBaseUrl } from "./backend-url";
import type { ApiErrorResponse } from "./goals";
import type {
  KnowledgeDocumentDetail,
  KnowledgeDocumentSummary,
} from "./knowledge";

type BackendResult<T> = {
  data: T;
  error: ApiErrorResponse | null;
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
    return {
      message: await response.text(),
      status: response.ok ? "OK" : "ERROR",
      errors: {},
      timestamp: new Date().toISOString(),
    };
  }
  return response.json();
}

function normalizeError(
  payload: unknown,
  fallbackMessage: string,
): ApiErrorResponse {
  if (
    payload &&
    typeof payload === "object" &&
    "message" in payload &&
    typeof payload.message === "string"
  ) {
    const record = payload as Partial<ApiErrorResponse>;
    const message = record.message ?? fallbackMessage;
    return {
      errors: record.errors ?? { backend: message },
      message,
      status: record.status ?? "BAD_GATEWAY",
      timestamp: record.timestamp ?? new Date().toISOString(),
    };
  }

  return buildError(fallbackMessage);
}

export async function fetchBackendKnowledgeDocuments(): Promise<
  BackendResult<KnowledgeDocumentSummary[]>
> {
  try {
    const response = await fetch(`${getBackendBaseUrl()}/api/knowledge/documents`, {
      cache: "no-store",
      headers: await authHeadersFromCookies(),
    });
    const payload = await parseJson(response);
    if (!response.ok) {
      return {
        data: [],
        error: normalizeError(payload, "Backend knowledge documents request failed."),
      };
    }
    return { data: payload as KnowledgeDocumentSummary[], error: null };
  } catch (error) {
    return {
      data: [],
      error: buildError(
        error instanceof Error
          ? error.message
          : "Backend knowledge documents request failed.",
      ),
    };
  }
}

export async function fetchBackendKnowledgeDocument(
  documentId: string,
): Promise<BackendResult<KnowledgeDocumentDetail | null>> {
  try {
    const response = await fetch(
      `${getBackendBaseUrl()}/api/knowledge/documents/${documentId}`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );
    const payload = await parseJson(response);
    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend knowledge document request failed."),
      };
    }
    return { data: payload as KnowledgeDocumentDetail, error: null };
  } catch (error) {
    return {
      data: null,
      error: buildError(
        error instanceof Error
          ? error.message
          : "Backend knowledge document request failed.",
      ),
    };
  }
}
