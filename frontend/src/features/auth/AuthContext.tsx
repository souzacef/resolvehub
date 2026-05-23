import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { clearAccessToken, getAccessToken, setAccessToken } from '../../lib/storage';
import {
  hasSessionExpiredNotice,
  markSessionExpiredNotice,
  SESSION_EXPIRED_EVENT,
} from '../../lib/session';
import type { UserRole } from '../../types/api';
import {
  extractEmailFromJwt,
  extractExpirationFromJwt,
  extractOrganizationIdFromJwt,
  extractRoleFromJwt,
  extractUserIdFromJwt,
  isJwtExpired,
} from './jwt';
import { login as loginRequest } from './auth';

type LoginInput = {
  email: string;
  password: string;
};

type AuthContextValue = {
  token: string | null;
  role: UserRole | null;
  userId: string | null;
  organizationId: string | null;
  email: string | null;
  canCreateTickets: boolean;
  isAuthenticated: boolean;
  login: (input: LoginInput) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function getInitialToken(): string | null {
  const token = getAccessToken();
  if (!token) {
    return null;
  }

  if (isJwtExpired(token)) {
    clearAccessToken();
    markSessionExpiredNotice();
    return null;
  }

  return token;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [token, setToken] = useState<string | null>(() => getInitialToken());

  const clearSession = useCallback((options?: { expired?: boolean }) => {
    clearAccessToken();
    setToken(null);
    if (options?.expired) {
      markSessionExpiredNotice();
    }
  }, []);

  const login = useCallback(async (input: LoginInput) => {
    const response = await loginRequest(input);
    setAccessToken(response.accessToken);
    setToken(response.accessToken);
  }, []);

  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  useEffect(() => {
    if (!token && hasSessionExpiredNotice() && location.pathname !== '/login') {
      navigate('/login', { replace: true });
    }
  }, [location.pathname, navigate, token]);

  useEffect(() => {
    function handleSessionExpiredEvent() {
      clearSession({ expired: true });
      if (location.pathname !== '/login') {
        navigate('/login', { replace: true });
      }
    }

    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpiredEvent);
    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpiredEvent);
    };
  }, [clearSession, location.pathname, navigate]);

  useEffect(() => {
    if (!token) {
      return;
    }

    if (isJwtExpired(token)) {
      clearSession({ expired: true });
      if (location.pathname !== '/login') {
        navigate('/login', { replace: true });
      }
      return;
    }

    const expSeconds = extractExpirationFromJwt(token);
    if (expSeconds === null) {
      return;
    }

    const millisecondsUntilExpiration = expSeconds * 1000 - Date.now();
    if (millisecondsUntilExpiration <= 0) {
      clearSession({ expired: true });
      if (location.pathname !== '/login') {
        navigate('/login', { replace: true });
      }
      return;
    }

    const timeoutId = window.setTimeout(() => {
      clearSession({ expired: true });
      if (location.pathname !== '/login') {
        navigate('/login', { replace: true });
      }
    }, millisecondsUntilExpiration);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [clearSession, location.pathname, navigate, token]);

  const role = useMemo(() => extractRoleFromJwt(token), [token]);
  const userId = useMemo(() => extractUserIdFromJwt(token), [token]);
  const organizationId = useMemo(() => extractOrganizationIdFromJwt(token), [token]);
  const email = useMemo(() => extractEmailFromJwt(token), [token]);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      role,
      userId,
      organizationId,
      email,
      canCreateTickets: Boolean(role),
      isAuthenticated: Boolean(token) && !isJwtExpired(token),
      login,
      logout,
    }),
    [token, role, userId, organizationId, email, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
