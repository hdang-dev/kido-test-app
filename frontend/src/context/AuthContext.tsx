import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { ApiError, setUnauthorizedHandler } from '../api/client';
import type { MeResponse } from '../api/types';

interface AuthState {
  user: MeResponse | null;
  loading: boolean;
  login: (email: string, password: string, rememberMe: boolean) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  setUser: (user: MeResponse | null) => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  // The initial GET also makes the backend issue the XSRF-TOKEN cookie needed for the first POST.
  const refresh = useCallback(async () => {
    try {
      setUser(await authApi.fetchMe());
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) setUser(null);
      else throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null)); // expired session → guards redirect to /login
    void refresh();
    return () => setUnauthorizedHandler(null);
  }, [refresh]);

  const login = async (email: string, password: string, rememberMe: boolean) => {
    setUser(await authApi.login({ email, password, rememberMe }));
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refresh, setUser }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
