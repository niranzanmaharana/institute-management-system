import { HttpErrorResponse } from '@angular/common/http';

/** Body shape returned by IMS `ApiError` JSON. */
export interface ApiErrorBody {
  timestamp?: string;
  traceId?: string;
  status?: number;
  error?: string;
  message?: string;
  details?: { field: string; message: string }[];
}

const NETWORK_MESSAGE =
  'Cannot reach the server. Check your internet connection or that the API is running (gateway on http://localhost:8088).';

const AUTH_MESSAGE = 'Authentication required. Please sign in again.';
const FORBIDDEN_MESSAGE = 'You do not have permission to perform this action.';
const SERVER_MESSAGE = 'The server encountered an error. Please try again later.';

/**
 * Maps an HttpClient failure to a user-facing message.
 * Prefers API `message` when present; handles 401, network (status 0), and 5xx distinctly.
 */
export function httpErrorMessage(err: unknown, fallback: string): string {
  const http = asHttpError(err);
  if (!http) {
    return fallback;
  }

  if (isNetworkFailure(http)) {
    return NETWORK_MESSAGE;
  }

  const apiMessage = extractApiMessage(http);

  if (http.status === 401) {
    return apiMessage || AUTH_MESSAGE;
  }
  if (http.status === 403) {
    return apiMessage || FORBIDDEN_MESSAGE;
  }
  if (http.status >= 500) {
    return apiMessage || SERVER_MESSAGE;
  }

  return apiMessage || fallback;
}

/** Load/list helpers: 404 → "{label} not found.", else shared mapping + load fallback. */
export function httpLoadError(err: unknown, entityLabel: string): string {
  const http = asHttpError(err);
  if (http?.status === 404) {
    return httpErrorMessage(err, `${capitalize(entityLabel)} not found.`);
  }
  return httpErrorMessage(err, `Could not load ${entityLabel}.`);
}

export function isNetworkFailure(err: unknown): boolean {
  const http = asHttpError(err);
  if (!http) {
    return false;
  }
  // status 0: offline, DNS, CORS, connection refused
  return http.status === 0;
}

function asHttpError(err: unknown): HttpErrorResponse | null {
  if (err instanceof HttpErrorResponse) {
    return err;
  }
  if (err && typeof err === 'object' && 'status' in err) {
    return err as HttpErrorResponse;
  }
  return null;
}

function extractApiMessage(http: HttpErrorResponse): string | null {
  const body = http.error;
  if (body && typeof body === 'object' && !Array.isArray(body)) {
    const msg = (body as ApiErrorBody).message;
    if (typeof msg === 'string' && msg.trim().length > 0) {
      return msg.trim();
    }
  }
  if (typeof body === 'string' && body.trim().length > 0) {
    try {
      const parsed = JSON.parse(body) as ApiErrorBody;
      if (typeof parsed?.message === 'string' && parsed.message.trim()) {
        return parsed.message.trim();
      }
    } catch {
      // not JSON
    }
  }
  return null;
}

function capitalize(value: string): string {
  if (!value) {
    return value;
  }
  return value.charAt(0).toUpperCase() + value.slice(1);
}
