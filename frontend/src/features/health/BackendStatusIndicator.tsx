import { useEffect, useState } from 'react';
import { useBackendHealth, type BackendHealthPhase } from './BackendHealthContext';
import './backendStatusIndicator.css';

const READY_LABEL_DURATION_MS = 4_000;

function phaseLabel(phase: BackendHealthPhase): string {
  switch (phase) {
    case 'checking':
      return 'Checking service...';
    case 'waking':
      return 'Starting service...';
    case 'still-waking':
      return 'Still starting...';
    case 'ready':
      return 'Ready';
    case 'unavailable':
      return 'Unavailable';
  }
}

function StatusIcon({ phase }: { phase: BackendHealthPhase }) {
  if (phase === 'ready') {
    return (
      <svg className="backend-status-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <circle cx="10" cy="10" r="7" stroke="currentColor" strokeWidth="2" />
        <path d="m6.8 10 2 2 4.3-4.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  if (phase === 'unavailable') {
    return (
      <svg className="backend-status-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <circle cx="10" cy="10" r="7" stroke="currentColor" strokeWidth="2" />
        <path d="M10 6.2v4.7M10 13.9h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }

  return <span className="backend-status-spinner" aria-hidden="true" />;
}

export function BackendStatusIndicator() {
  const { canRetry, phase, retry } = useBackendHealth();
  const [readyLabelVisible, setReadyLabelVisible] = useState(true);

  useEffect(() => {
    if (phase !== 'ready') {
      setReadyLabelVisible(true);
      return;
    }

    const timer = window.setTimeout(() => {
      setReadyLabelVisible(false);
    }, READY_LABEL_DURATION_MS);

    return () => window.clearTimeout(timer);
  }, [phase]);

  const label = phaseLabel(phase);
  const showLabel = phase !== 'ready' || readyLabelVisible;

  return (
    <div className={`backend-status-indicator backend-status-indicator-${phase}`}>
      <a
        className="backend-status-indicator-link"
        href="/status"
        target="_blank"
        rel="noopener noreferrer"
        title="Open service details"
        aria-label={`${label}. Open service details`}
      >
        <StatusIcon phase={phase} />
        {showLabel ? <span aria-live="polite">{label}</span> : null}
      </a>

      {canRetry ? (
        <button
          className="backend-status-retry"
          type="button"
          onClick={retry}
          aria-label="Try again"
          title="Try again"
        >
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M15.8 7.1A6.3 6.3 0 1 0 16.2 12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            <path d="M15.8 3.8v3.7h-3.7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      ) : null}
    </div>
  );
}
