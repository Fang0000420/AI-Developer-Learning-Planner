import { authHeadersFromCookies } from "./backend-auth";
import { getBackendBaseUrl } from "./backend-url";
import type {
  ApiErrorResponse,
  Goal,
  GoalDecomposition,
  ProjectRecommendation,
  SkillGapAnalysis,
  SkillProfile,
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

export async function fetchBackendGoals(): Promise<BackendResult<Goal[]>> {
  try {
    const response = await fetch(`${getBackendBaseUrl()}/api/goals`, {
      cache: "no-store",
      headers: await authHeadersFromCookies(),
    });
    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend goals request failed."),
      };
    }

    return {
      data: Array.isArray(payload) ? (payload as Goal[]) : [],
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Backend goals request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}

export async function fetchBackendGoal(
  goalId: string,
): Promise<BackendResult<Goal>> {
  try {
    const response = await fetch(`${getBackendBaseUrl()}/api/goals/${goalId}`, {
      cache: "no-store",
      headers: await authHeadersFromCookies(),
    });
    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend goal detail request failed."),
      };
    }

    return {
      data: payload as Goal,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend goal detail request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}

export async function fetchBackendGoalProfile(
  goalId: string,
): Promise<BackendResult<SkillProfile | null>> {
  try {
    const response = await fetch(
      `${getBackendBaseUrl()}/api/goals/${goalId}/profile`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );

    if (response.status === 204) {
      return {
        data: null,
        error: null,
      };
    }

    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend profile request failed."),
      };
    }

    return {
      data: payload as SkillProfile,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend profile request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}

export async function fetchBackendGoalDecomposition(
  goalId: string,
): Promise<BackendResult<GoalDecomposition | null>> {
  try {
    const response = await fetch(
      `${getBackendBaseUrl()}/api/goals/${goalId}/decomposition`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );

    if (response.status === 204) {
      return {
        data: null,
        error: null,
      };
    }

    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend decomposition request failed."),
      };
    }

    return {
      data: payload as GoalDecomposition,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend decomposition request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}

export async function fetchBackendSkillGapAnalysis(
  goalId: string,
): Promise<BackendResult<SkillGapAnalysis | null>> {
  try {
    const response = await fetch(
      `${getBackendBaseUrl()}/api/goals/${goalId}/skill-gap`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );

    if (response.status === 204) {
      return {
        data: null,
        error: null,
      };
    }

    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(payload, "Backend skill gap request failed."),
      };
    }

    return {
      data: payload as SkillGapAnalysis,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend skill gap request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}

export async function fetchBackendProjectRecommendation(
  goalId: string,
): Promise<BackendResult<ProjectRecommendation | null>> {
  try {
    const response = await fetch(
      `${getBackendBaseUrl()}/api/goals/${goalId}/project-recommendation`,
      {
        cache: "no-store",
        headers: await authHeadersFromCookies(),
      },
    );

    if (response.status === 204) {
      return {
        data: null,
        error: null,
      };
    }

    const payload = await parseJson(response);

    if (!response.ok) {
      return {
        data: null,
        error: normalizeError(
          payload,
          "Backend project recommendation request failed.",
        ),
      };
    }

    return {
      data: payload as ProjectRecommendation,
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : "Backend project recommendation request failed.";

    return {
      data: null,
      error: buildError(message),
    };
  }
}
