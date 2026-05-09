import { describe, expect, it } from 'vitest';
import { extractRoleFromJwt } from './jwt';

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
