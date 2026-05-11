import { describe, expect, it } from 'vitest';
import { extractRoleFromJwt, extractUserIdFromJwt } from './jwt';

function buildToken(payload: object): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');
  const body = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');
  return `${header}.${body}.signature`;
}

describe('extractRoleFromJwt', () => {
  it('returns role from valid token payload', () => {
    const token = buildToken({ role: 'CUSTOMER' });
    expect(extractRoleFromJwt(token)).toBe('CUSTOMER');
  });

  it('returns null for invalid token format', () => {
    expect(extractRoleFromJwt('invalid')).toBeNull();
  });

  it('returns null for unknown role claim', () => {
    const token = buildToken({ role: 'UNKNOWN' });
    expect(extractRoleFromJwt(token)).toBeNull();
  });
});

describe('extractUserIdFromJwt', () => {
  it('returns uid from valid token payload', () => {
    const token = buildToken({ uid: '8f43f616-58c8-4a70-a4b0-1d7a60883262' });
    expect(extractUserIdFromJwt(token)).toBe('8f43f616-58c8-4a70-a4b0-1d7a60883262');
  });

  it('returns null when uid claim is missing', () => {
    const token = buildToken({ role: 'AGENT' });
    expect(extractUserIdFromJwt(token)).toBeNull();
  });

  it('returns uid instead of sub when both claims exist', () => {
    const token = buildToken({
      uid: '8f43f616-58c8-4a70-a4b0-1d7a60883262',
      oid: 'c3a4ee8d-f8ac-463e-a77f-2149ea8cdca8',
      role: 'AGENT',
      sub: 'agent@resolvehub.dev',
    });
    expect(extractUserIdFromJwt(token)).toBe('8f43f616-58c8-4a70-a4b0-1d7a60883262');
  });

  it('returns null for invalid token format', () => {
    expect(extractUserIdFromJwt('invalid')).toBeNull();
  });
});
