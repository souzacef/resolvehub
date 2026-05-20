import type { UserRole } from '../../types/api';

type JwtPayload = {
  sub?: unknown;
  role?: unknown;
  uid?: unknown;
  oid?: unknown;
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

export function extractOrganizationIdFromJwt(token: string | null): string | null {
  const parsed = parsePayload(token);
  if (!parsed) {
    return null;
  }

  if (typeof parsed.oid !== 'string' || parsed.oid.trim().length === 0) {
    return null;
  }

  return parsed.oid.trim();
}

export function extractEmailFromJwt(token: string | null): string | null {
  const parsed = parsePayload(token);
  if (!parsed) {
    return null;
  }

  if (typeof parsed.sub !== 'string' || parsed.sub.trim().length === 0) {
    return null;
  }

  return parsed.sub.trim();
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
