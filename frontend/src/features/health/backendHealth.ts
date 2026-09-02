const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

interface HealthResponse {
  status?: unknown;
}

export async function isBackendUp(
  signal?: AbortSignal,
  fetchImplementation: typeof fetch = fetch,
): Promise<boolean> {
  try {
    const response = await fetchImplementation(`${baseUrl}/actuator/health`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      signal,
    });

    if (!response.ok) {
      return false;
    }

    const body = (await response.json()) as HealthResponse;
    return body.status === 'UP';
  } catch {
    return false;
  }
}
