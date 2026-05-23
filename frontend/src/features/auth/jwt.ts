import type { UserRole } from '../../types/api';

type JwtPayload = {
  sub?: unknown;
  role?: unknown;
  uid?: unknown;
  oid?: unknown;
  exp?: unknown;
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


export function extractExpirationFromJwt(token: string | null): number | null {
  const parsed = parsePayload(token);
  if (!parsed) {
    return null;
  }

  if (typeof parsed.exp === 'number' && Number.isFinite(parsed.exp)) {
    return parsed.exp;
  }

  if (typeof parsed.exp === 'string') {
    const parsedExp = Number.parseInt(parsed.exp, 10);
    if (!Number.isNaN(parsedExp)) {
      return parsedExp;
    }
  }

  return null;
}

export function isJwtExpired(token: string | null, nowMs: number = Date.now()): boolean {
  const expSeconds = extractExpirationFromJwt(token);
  if (expSeconds === null) {
    return false;
  }

  return expSeconds * 1000 <= nowMs;
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
