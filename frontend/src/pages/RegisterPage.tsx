import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { register } from '../features/auth/auth';
import {
  INVALID_EMAIL_MESSAGE,
  isEmailFormatValid,
} from '../features/auth/emailValidation';
import {
  PASSWORD_POLICY_HINT,
  isPasswordPolicyCompliant,
  validatePasswordPolicy,
} from '../features/auth/passwordPolicy';
import { isApiError } from '../lib/apiClient';

const DUPLICATE_EMAIL_ERROR_MESSAGE =
  'This email is already registered. Try logging in instead.';
const REGISTRATION_VALIDATION_ERROR_MESSAGE =
  'Please check the registration fields and try again.';
const SERVICE_UNAVAILABLE_ERROR_MESSAGE =
  'Service is unavailable. Please try again later.';

export function RegisterPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [organizationName, setOrganizationName] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [emailTouched, setEmailTouched] = useState(false);
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const emailIsInvalid = emailTouched && !isEmailFormatValid(email);
  const emailIsValid = emailTouched && email.trim().length > 0 && !emailIsInvalid;
  const passwordHasValue = password.length > 0;
  const passwordIsValid = passwordHasValue && isPasswordPolicyCompliant(password);
  const passwordPolicyError =
    passwordHasValue && !passwordIsValid ? validatePasswordPolicy(password) : null;

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEmailTouched(true);
    setError(null);
    setSuccess(null);

    const hasRequiredFields =
      organizationName.trim().length > 0 &&
      name.trim().length > 0 &&
      password.length > 0;

    if (!hasRequiredFields) {
      setError(REGISTRATION_VALIDATION_ERROR_MESSAGE);
      return;
    }

    if (!isEmailFormatValid(email)) {
      setError(INVALID_EMAIL_MESSAGE);
      return;
    }

    const passwordValidationError = validatePasswordPolicy(password);
    if (passwordValidationError !== null) {
      return;
    }

    setIsSubmitting(true);

    try {
      await register({
        organizationName: organizationName.trim(),
        name: name.trim(),
        email: email.trim(),
        password,
      });

      setSuccess('Account created successfully. Redirecting to login...');
      setTimeout(() => {
        navigate('/login', { replace: true });
      }, 1000);
    } catch (submitError) {
      setError(mapRegisterError(submitError));
    } finally {
      setIsSubmitting(false);
    }
  }

  function mapRegisterError(submitError: unknown): string {
    if (!isApiError(submitError)) {
      return SERVICE_UNAVAILABLE_ERROR_MESSAGE;
    }

    if (submitError.kind === 'network') {
      return SERVICE_UNAVAILABLE_ERROR_MESSAGE;
    }

    if (isDuplicateEmailError(submitError.status, submitError.responseBody)) {
      return DUPLICATE_EMAIL_ERROR_MESSAGE;
    }

    if (
      submitError.responseBody !== null &&
      submitError.responseBody.includes(PASSWORD_POLICY_HINT)
    ) {
      return PASSWORD_POLICY_HINT;
    }

    if (submitError.status === 400 || submitError.status === 422) {
      return REGISTRATION_VALIDATION_ERROR_MESSAGE;
    }

    if (submitError.status !== null && submitError.status >= 500) {
      return SERVICE_UNAVAILABLE_ERROR_MESSAGE;
    }

    return REGISTRATION_VALIDATION_ERROR_MESSAGE;
  }

  function isDuplicateEmailError(
    status: number | null,
    responseBody: string | null,
  ): boolean {
    if (status === 409) {
      return true;
    }

    if (status !== 400 || !responseBody) {
      return false;
    }

    const normalizedBody = responseBody.toLowerCase();
    return (
      normalizedBody.includes('email already registered') ||
      normalizedBody.includes('already registered') ||
      normalizedBody.includes('duplicate email')
    );
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Create Account</h1>
        <p>Create your organization and first admin account.</p>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="organizationName">Organization name</label>
          <input
            id="organizationName"
            type="text"
            autoComplete="organization"
            required
            maxLength={120}
            value={organizationName}
            onChange={(event) => setOrganizationName(event.target.value)}
          />

          <label htmlFor="name">Your name</label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            required
            maxLength={120}
            value={name}
            onChange={(event) => setName(event.target.value)}
          />

          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="text"
            autoComplete="email"
            required
            value={email}
            className={emailIsInvalid ? 'input-invalid' : emailIsValid ? 'input-valid' : ''}
            onChange={(event) => {
              const nextValue = event.target.value;
              setEmail(nextValue);
              if (!emailTouched && nextValue.length > 0) {
                setEmailTouched(true);
              }
            }}
            onBlur={() => setEmailTouched(true)}
          />
          <p
            className={`field-feedback ${
              emailIsInvalid ? 'field-feedback-error' : 'field-feedback-muted'
            }`}
            role={emailIsInvalid ? 'alert' : undefined}
          >
            {emailIsInvalid ? INVALID_EMAIL_MESSAGE : ' '}
          </p>

          <label htmlFor="password">Password</label>
          <div className="password-input-wrapper">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              maxLength={72}
              required
              className={
                passwordHasValue
                  ? passwordIsValid
                    ? 'input-valid'
                    : 'input-invalid'
                  : ''
              }
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            <button
              type="button"
              className="password-toggle-button"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              onClick={() => setShowPassword((currentValue) => !currentValue)}
            >
              {showPassword ? 'Hide' : 'Show'}
            </button>
          </div>
          <p
            className={`field-feedback ${
              passwordHasValue
                ? passwordIsValid
                  ? 'field-feedback-success'
                  : 'field-feedback-error'
                : 'field-feedback-muted'
            }`}
            role={passwordHasValue && !passwordIsValid ? 'alert' : undefined}
          >
            {passwordHasValue
              ? passwordIsValid
                ? 'Password meets requirements.'
                : passwordPolicyError
              : PASSWORD_POLICY_HINT}
          </p>

          {error ? <p className="error-text">{error}</p> : null}
          {success ? <p className="muted-text">{success}</p> : null}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="muted-text" style={{ marginTop: '1rem' }}>
          Already have an account? <Link to="/login">Back to login</Link>
        </p>
      </div>
    </div>
  );
}
