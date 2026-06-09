import type { ApiErrorResponse, AsyncJob } from "./goals";

export function getApiErrorMessage(
  error: ApiErrorResponse,
  fallbackMessage: string,
) {
  if (error.errors) {
    const firstError = Object.values(error.errors)[0];
    if (firstError) {
      return firstError;
    }
  }

  return error.message || fallbackMessage;
}

export async function postJson<T>(endpoint: string, body?: object) {
  const response = await fetch(endpoint, {
    body: body ? JSON.stringify(body) : undefined,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    method: "POST",
  });
  const payload = (await response.json()) as T | ApiErrorResponse;

  if (!response.ok) {
    throw new Error(
      getApiErrorMessage(payload as ApiErrorResponse, "Request failed."),
    );
  }

  return payload as T;
}

export async function pollJob<T>(
  jobId: string,
  onUpdate?: (job: AsyncJob<T>) => void,
) {
  for (let attempt = 0; attempt < 80; attempt++) {
    const response = await fetch(`/api/jobs/${jobId}`, {
      cache: "no-store",
    });
    const payload = (await response.json()) as AsyncJob<T> | ApiErrorResponse;

    if (!response.ok) {
      throw new Error(
        getApiErrorMessage(payload as ApiErrorResponse, "Job request failed."),
      );
    }

    const job = payload as AsyncJob<T>;
    onUpdate?.(job);

    if (job.status === "SUCCEEDED") {
      if (!job.result) {
        throw new Error("Async job succeeded without a result.");
      }
      return job.result;
    }

    if (job.status === "FAILED") {
      throw new Error(job.errorMessage || "Async job failed.");
    }

    await new Promise((resolve) => setTimeout(resolve, 1500));
  }

  throw new Error("Async job timed out while waiting for completion.");
}
