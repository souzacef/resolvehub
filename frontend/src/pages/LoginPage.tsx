import { FormEvent, useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
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
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    setIsSubmitting(true);
    setError(null);

    try {
      await login({ email, password });
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

        <form onSubmit={handleSubmit}>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

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
