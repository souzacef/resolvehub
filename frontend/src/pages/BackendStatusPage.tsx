import { Link } from 'react-router-dom';
import { useBackendHealth } from '../features/health/BackendHealthContext';

export function BackendStatusPage() {
  const { canRetry, phase, ready, retry } = useBackendHealth();
  const checking =
    phase === 'checking' || phase === 'waking' || phase === 'still-waking';

  const statusText = ready
    ? 'ResolveHub is ready.'
    : phase === 'still-waking'
      ? 'ResolveHub is still starting up...'
      : phase === 'unavailable'
        ? 'ResolveHub could not be reached yet.'
        : 'Getting ResolveHub ready...';

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
            {statusText}
          </p>
          {checking ? (
            <p style={{ marginBottom: '1rem' }}>
              The service may take a few minutes to wake after a period of inactivity.
            </p>
          ) : null}
        </div>

        {canRetry && !ready ? (
          <button type="button" onClick={retry}>
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
