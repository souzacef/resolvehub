import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { isBackendUp } from './backendHealth';

export const SERVICE_STATUS_POLL_INTERVAL_MS = 2_500;
export const SERVICE_STATUS_CHECKING_WINDOW_MS = 10_000;
export const SERVICE_STATUS_SOFT_RETRY_MS = 150_000;
export const SERVICE_STATUS_STARTUP_WINDOW_MS = 240_000;

export type BackendHealthPhase =
  | 'checking'
  | 'waking'
  | 'still-waking'
  | 'ready'
  | 'unavailable';

interface BackendHealthContextValue {
  phase: BackendHealthPhase;
  ready: boolean;
  canRetry: boolean;
  retry: () => void;
}

const BackendHealthContext = createContext<BackendHealthContextValue | null>(null);

export function BackendHealthProvider({ children }: { children: ReactNode }) {
  const [attempt, setAttempt] = useState(0);
  const [phase, setPhase] = useState<BackendHealthPhase>('checking');

  useEffect(() => {
    let cancelled = false;
    let retryTimer: number | undefined;
    const controller = new AbortController();

    const wakingTimer = window.setTimeout(() => {
      if (!cancelled) {
        setPhase('waking');
      }
    }, SERVICE_STATUS_CHECKING_WINDOW_MS);

    const softRetryTimer = window.setTimeout(() => {
      if (!cancelled) {
        setPhase('still-waking');
      }
    }, SERVICE_STATUS_SOFT_RETRY_MS);

    const unavailableTimer = window.setTimeout(() => {
      if (cancelled) {
        return;
      }

      cancelled = true;
      controller.abort();
      if (retryTimer !== undefined) {
        window.clearTimeout(retryTimer);
      }
      setPhase('unavailable');
    }, SERVICE_STATUS_STARTUP_WINDOW_MS);

    function clearPhaseTimers() {
      window.clearTimeout(wakingTimer);
      window.clearTimeout(softRetryTimer);
      window.clearTimeout(unavailableTimer);
    }

    async function checkBackend() {
      const ready = await isBackendUp(controller.signal);
      if (cancelled) {
        return;
      }

      if (ready) {
        cancelled = true;
        clearPhaseTimers();
        setPhase('ready');
        return;
      }

      retryTimer = window.setTimeout(() => {
        void checkBackend();
      }, SERVICE_STATUS_POLL_INTERVAL_MS);
    }

    setPhase('checking');
    void checkBackend();

    return () => {
      cancelled = true;
      controller.abort();
      clearPhaseTimers();
      if (retryTimer !== undefined) {
        window.clearTimeout(retryTimer);
      }
    };
  }, [attempt]);

  const retry = useCallback(() => {
    setPhase('checking');
    setAttempt((current) => current + 1);
  }, []);

  const value = useMemo<BackendHealthContextValue>(
    () => ({
      phase,
      ready: phase === 'ready',
      canRetry: phase === 'still-waking' || phase === 'unavailable',
      retry,
    }),
    [phase, retry],
  );

  return (
    <BackendHealthContext.Provider value={value}>
      {children}
    </BackendHealthContext.Provider>
  );
}

export function useBackendHealth(): BackendHealthContextValue {
  const context = useContext(BackendHealthContext);
  if (context === null) {
    throw new Error('useBackendHealth must be used within BackendHealthProvider');
  }
  return context;
}
