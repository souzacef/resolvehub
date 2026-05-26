export const PASSWORD_POLICY_HINT =
  'Use at least 8 characters with uppercase, lowercase, number, special character, and no spaces.';

const PASSWORD_POLICY_REGEX =
  /^(?=\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,72}$/;

export function isPasswordPolicyCompliant(password: string): boolean {
  return PASSWORD_POLICY_REGEX.test(password);
}

export function validatePasswordPolicy(password: string): string | null {
  if (isPasswordPolicyCompliant(password)) {
    return null;
  }

  return PASSWORD_POLICY_HINT;
}
