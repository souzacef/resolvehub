import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { register } from '../features/auth/auth';
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
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      await register({
        organizationName,
        name,
        email,
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

        <form onSubmit={handleSubmit}>
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
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

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
