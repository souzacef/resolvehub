import { apiRequest } from '../../lib/apiClient';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
} from '../../types/api';

export async function login(request: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: request,
    auth: false,
  });
}

export async function register(
  request: RegisterRequest,
): Promise<RegisterResponse> {
  return apiRequest<RegisterResponse>('/api/auth/register', {
    method: 'POST',
    body: request,
    auth: false,
  });
}
