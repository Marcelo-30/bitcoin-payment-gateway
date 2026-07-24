/**
 * Minimal reusable API client for the backend REST API.
 *
 * The base URL comes from the VITE_API_BASE_URL environment variable, falling
 * back to the local backend so the app runs out of the box in development.
 */
const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** Standard error body returned by the backend (see its GlobalExceptionHandler). */
interface ApiErrorBody {
  message?: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: { Accept: 'application/json', ...init?.headers },
    ...init,
  });

  if (!response.ok) {
    const message = await extractErrorMessage(response, path);
    throw new ApiError(response.status, message);
  }

  // 204 responses carry no body.
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

/** Prefer the backend's error message, falling back to a generic one. */
async function extractErrorMessage(response: Response, path: string): Promise<string> {
  try {
    const body = (await response.json()) as ApiErrorBody;
    if (body?.message) {
      return body.message;
    }
  } catch {
    // No JSON body; fall through to the generic message.
  }
  return `Request to ${path} failed with status ${response.status}`;
}

export const apiClient = {
  baseUrl,
  get: <T>(path: string, init?: RequestInit) => request<T>(path, { ...init, method: 'GET' }),
  post: <T>(path: string, body?: unknown, init?: RequestInit) =>
    request<T>(path, {
      ...init,
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...init?.headers },
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
};
