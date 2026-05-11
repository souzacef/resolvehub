import type { UserRole } from '../../types/api';

type JwtPayload = {
  role?: unknown;
  uid?: unknown;
};

const roles: UserRole[] = ['CUSTOMER', 'AGENT', 'MANAGER', 'ADMIN'];

export function extractRoleFromJwt(token: string | null): UserRole | null {
  const parsed = parsePayload(token);
  if (!parsed) {
    return null;
  }

  if (typeof parsed.role === 'string' && roles.includes(parsed.role as UserRole)) {
    return parsed.role as UserRole;
  }

  return null;
}

export function extractUserIdFromJwt(token: string | null): string | null {
  const parsed = parsePayload(token);
  if (!parsed) {
    return null;
  }

  if (typeof parsed.uid !== 'string' || parsed.uid.trim().length === 0) {
    return null;
  }

  return parsed.uid.trim();
}

function parsePayload(token: string | null): JwtPayload | null {
  if (!token) {
    return null;
  }

  const sections = token.split('.');
  if (sections.length < 2) {
    return null;
  }

  try {
    const payload = decodeBase64Url(sections[1]);
    return JSON.parse(payload) as JwtPayload;
  } catch {
    return null;
  }
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
  return atob(padded);
}
