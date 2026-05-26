import { FormEvent, useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import {
  INVALID_EMAIL_MESSAGE,
  isEmailFormatValid,
} from '../features/auth/emailValidation';
import { isApiError } from '../lib/apiClient';
import {
  consumeSessionExpiredNotice,
  SESSION_EXPIRED_MESSAGE,
} from '../lib/session';

const SERVICE_UNAVAILABLE_MESSAGE =
  'Service is unavailable. Please try again later.';
const LOGIN_FAILED_MESSAGE =
  'Login failed. Check your credentials or create an account.';

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [emailTouched, setEmailTouched] = useState(false);
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const emailIsInvalid = emailTouched && !isEmailFormatValid(email);
  const emailIsValid = emailTouched && email.trim().length > 0 && !emailIsInvalid;

  useEffect(() => {
    if (consumeSessionExpiredNotice()) {
      setError(SESSION_EXPIRED_MESSAGE);
    }
  }, []);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEmailTouched(true);
    setError(null);

    if (!isEmailFormatValid(email)) {
      setError(INVALID_EMAIL_MESSAGE);
      return;
    }

    if (password.length === 0) {
      setError('Enter your password.');
      return;
    }

    setIsSubmitting(true);

    try {
      await login({ email: email.trim(), password });
      navigate('/dashboard', { replace: true });
    } catch (submitError) {
      if (!isApiError(submitError)) {
        setError(SERVICE_UNAVAILABLE_MESSAGE);
      } else if (submitError.kind === 'network') {
        setError(SERVICE_UNAVAILABLE_MESSAGE);
      } else if (submitError.status !== null && submitError.status >= 500) {
        setError(SERVICE_UNAVAILABLE_MESSAGE);
      } else {
        setError(LOGIN_FAILED_MESSAGE);
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>ResolveHub</h1>
        <p>Sign in to continue to your support workspace.</p>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="text"
            autoComplete="email"
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
              autoComplete="current-password"
              required
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

          {error ? <p className="error-text">{error}</p> : null}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p className="muted-text" style={{ marginTop: '1rem' }}>
          New to ResolveHub? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
