import {
  markSessionExpiredNotice,
  SESSION_EXPIRED_EVENT,
} from './session';
import { clearAccessToken, getAccessToken } from './storage';

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  body?: unknown;
  auth?: boolean;
};

export class ApiError extends Error {
  status: number | null;
  responseBody: string | null;
  kind: 'http' | 'network';

  private constructor(
    message: string,
    kind: 'http' | 'network',
    status: number | null,
    responseBody: string | null,
  ) {
    super(message);
    this.name = 'ApiError';
    this.kind = kind;
    this.status = status;
    this.responseBody = responseBody;
  }

  static http(status: number, responseBody: string | null): ApiError {
    return new ApiError(
      responseBody || `Request failed: ${status}`,
      'http',
      status,
      responseBody,
    );
  }

  static network(cause: unknown): ApiError {
    const message =
      cause instanceof Error
        ? cause.message
        : 'Network request failed before reaching backend';
    return new ApiError(message, 'network', null, null);
  }
}

export function isApiError(value: unknown): value is ApiError {
  return value instanceof ApiError;
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers({
    'Content-Type': 'application/json',
  });

  if (options.auth !== false) {
    const token = getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch (error) {
    throw ApiError.network(error);
  }

  if (!response.ok) {
    const errorBody = await response.text();

    if (response.status === 401 && options.auth !== false) {
      clearAccessToken();
      markSessionExpiredNotice();
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }

    throw ApiError.http(response.status, errorBody || null);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
