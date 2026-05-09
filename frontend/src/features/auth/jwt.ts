import type { UserRole } from '../../types/api';

type JwtPayload = {
  role?: unknown;
};

const roles: UserRole[] = ['CUSTOMER', 'AGENT', 'MANAGER', 'ADMIN'];

export function extractRoleFromJwt(token: string | null): UserRole | null {
  if (!token) {
    return null;
  }

  const sections = token.split('.');
  if (sections.length < 2) {
    return null;
  }

  try {
    const payload = decodeBase64Url(sections[1]);
    const parsed = JSON.parse(payload) as JwtPayload;

    if (typeof parsed.role === 'string' && roles.includes(parsed.role as UserRole)) {
      return parsed.role as UserRole;
    }

    return null;
  } catch {
    return null;
  }
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
  return atob(padded);
}
