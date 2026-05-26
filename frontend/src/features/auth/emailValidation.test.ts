import { describe, expect, it } from 'vitest';
import { INVALID_EMAIL_MESSAGE, isEmailFormatValid } from './emailValidation';

describe('email validation', () => {
  it('returns true for valid emails', () => {
    expect(isEmailFormatValid('person@resolvehub.com')).toBe(true);
  });

  it('trims before validating', () => {
    expect(isEmailFormatValid('  person@resolvehub.com  ')).toBe(true);
  });

  it('returns false for invalid emails', () => {
    expect(isEmailFormatValid('not-an-email')).toBe(false);
    expect(isEmailFormatValid('')).toBe(false);
  });

  it('exports the standard invalid email message', () => {
    expect(INVALID_EMAIL_MESSAGE).toBe('Enter a valid email address.');
  });
});
