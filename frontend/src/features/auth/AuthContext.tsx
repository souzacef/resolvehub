import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { clearAccessToken, getAccessToken, setAccessToken } from '../../lib/storage';
import type { UserRole } from '../../types/api';
import {
  extractEmailFromJwt,
  extractOrganizationIdFromJwt,
  extractRoleFromJwt,
  extractUserIdFromJwt,
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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    setToken(getAccessToken());
  }, []);

  const login = useCallback(async (input: LoginInput) => {
    const response = await loginRequest(input);
    setAccessToken(response.accessToken);
    setToken(response.accessToken);
  }, []);

  const logout = useCallback(() => {
    clearAccessToken();
    setToken(null);
  }, []);

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
      isAuthenticated: Boolean(token),
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
