import { apiRequest } from '../../lib/apiClient';
import type { AuthResponse, LoginRequest } from '../../types/api';

export async function login(request: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: request,
    auth: false,
  });
}
