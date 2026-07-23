import { apiClient } from './client';

/** Shape returned by the backend GET /api/health endpoint. */
export interface HealthResponse {
  status: string;
  service: string;
}

/** Fetches the backend health status. */
export function fetchHealth(): Promise<HealthResponse> {
  return apiClient.get<HealthResponse>('/api/health');
}
