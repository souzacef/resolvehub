import { describe, expect, it } from 'vitest';
import {
  isPasswordPolicyCompliant,
  PASSWORD_POLICY_HINT,
  validatePasswordPolicy,
} from './passwordPolicy';

describe('password policy', () => {
  it('accepts Password123!', () => {
    expect(isPasswordPolicyCompliant('Password123!')).toBe(true);
    expect(validatePasswordPolicy('Password123!')).toBeNull();
  });

  it('rejects passwords with spaces', () => {
    expect(isPasswordPolicyCompliant('Teste de senha')).toBe(false);
    expect(validatePasswordPolicy('Teste de senha')).toBe(PASSWORD_POLICY_HINT);
  });

  it('rejects lowercase-only strings', () => {
    expect(isPasswordPolicyCompliant('lowercaseonlypassword')).toBe(false);
  });

  it('rejects passwords missing a number', () => {
    expect(isPasswordPolicyCompliant('Password!')).toBe(false);
  });

  it('rejects passwords missing a special character', () => {
    expect(isPasswordPolicyCompliant('Password123')).toBe(false);
  });

  it('rejects passwords shorter than 8 characters', () => {
    expect(isPasswordPolicyCompliant('Aa1!')).toBe(false);
  });
});
