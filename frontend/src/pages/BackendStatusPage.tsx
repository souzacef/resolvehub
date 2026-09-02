import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { isBackendUp } from '../features/health/backendHealth';

export const SERVICE_STATUS_POLL_INTERVAL_MS = 2_500;
export const SERVICE_STATUS_STARTUP_WINDOW_MS = 180_000;

type ServiceStatus = 'checking' | 'ready' | 'unavailable';

export function BackendStatusPage() {
  const [status, setStatus] = useState<ServiceStatus>('checking');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let retryTimer: number | undefined;
    const controller = new AbortController();
    const deadline = Date.now() + SERVICE_STATUS_STARTUP_WINDOW_MS;

    async function checkBackend() {
      const ready = await isBackendUp(controller.signal);
      if (cancelled) {
        return;
      }

      if (ready) {
        setStatus('ready');
        return;
      }

      if (Date.now() >= deadline) {
        setStatus('unavailable');
        return;
      }

      retryTimer = window.setTimeout(() => {
        void checkBackend();
      }, SERVICE_STATUS_POLL_INTERVAL_MS);
    }

    setStatus('checking');
    void checkBackend();

    return () => {
      cancelled = true;
      controller.abort();
      if (retryTimer !== undefined) {
        window.clearTimeout(retryTimer);
      }
    };
  }, [attempt]);

  const checking = status === 'checking';
  const ready = status === 'ready';

  return (
    <div className="login-page">
      <div className="login-card" style={{ maxWidth: '430px', textAlign: 'center' }}>
        <h1>ResolveHub status</h1>

        <div style={{ display: 'flex', justifyContent: 'center', margin: '1.5rem 0 1rem' }} aria-hidden="true">
          {checking ? (
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="12" stroke="#d6e4dc" strokeWidth="3" />
              <path d="M16 4a12 12 0 0 1 12 12" stroke="#0f7b53" strokeWidth="3" strokeLinecap="round">
                <animateTransform
                  attributeName="transform"
                  type="rotate"
                  from="0 16 16"
                  to="360 16 16"
                  dur="0.9s"
                  repeatCount="indefinite"
                />
              </path>
            </svg>
          ) : ready ? (
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="13" fill="#e7f6ef" stroke="#0f7b53" strokeWidth="2" />
              <path d="m10 16 4 4 8-9" stroke="#0f7b53" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          ) : (
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="13" fill="#fff7f6" stroke="#b42318" strokeWidth="2" />
              <path d="M16 9v9M16 23h.01" stroke="#b42318" strokeWidth="2.5" strokeLinecap="round" />
            </svg>
          )}
        </div>

        <div aria-live="polite">
          <p style={{ marginBottom: '0.65rem', color: 'var(--text)', fontWeight: 600 }}>
            {ready
              ? 'ResolveHub is ready.'
              : status === 'unavailable'
                ? 'ResolveHub is taking longer than expected to respond.'
                : 'Getting ResolveHub ready...'}
          </p>
          {checking ? (
            <p style={{ marginBottom: '1rem' }}>
              The service may take a couple of minutes to wake after a period of inactivity.
            </p>
          ) : null}
        </div>

        {status === 'unavailable' ? (
          <button type="button" onClick={() => setAttempt((current) => current + 1)}>
            Try again
          </button>
        ) : null}

        {ready ? (
          <p className="muted-text" style={{ marginTop: '1rem', marginBottom: 0 }}>
            <Link to="/login">Back to sign in</Link>
          </p>
        ) : null}
      </div>

      <footer className="login-footer">
        <span>Built by Carlos Eduardo Freire de Souza</span>
        <span className="login-footer-separator" aria-hidden="true">&middot;</span>
        <a href="https://github.com/souzacef">GitHub</a>
      </footer>
    </div>
  );
}
