export const INVALID_EMAIL_MESSAGE = 'Enter a valid email address.';

const EMAIL_FORMAT_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isEmailFormatValid(email: string): boolean {
  return EMAIL_FORMAT_REGEX.test(email.trim());
}
