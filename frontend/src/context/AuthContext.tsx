import {
  createContext,
  type ReactNode,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { api, ApiRequestError } from "../lib/api";
import type {
  RegisterResponse,
  SessionResponse,
} from "../types";

interface AuthContextValue {
  token: string | null;
  user: SessionResponse | null;
  isBootstrapping: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  register: (
    username: string,
    email: string,
    password: string,
  ) => Promise<RegisterResponse>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const TOKEN_KEY = "cashflowcoin.sessionToken";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem(TOKEN_KEY),
  );
  const [user, setUser] = useState<SessionResponse | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  useEffect(() => {
    let active = true;

    async function bootstrap() {
      if (!token) {
        if (active) {
          setIsBootstrapping(false);
        }
        return;
      }

      try {
        const session = await api.auth.session(token);
        if (active) {
          setUser(session);
        }
      } catch {
        localStorage.removeItem(TOKEN_KEY);
        if (active) {
          setToken(null);
          setUser(null);
        }
      } finally {
        if (active) {
          setIsBootstrapping(false);
        }
      }
    }

    void bootstrap();

    return () => {
      active = false;
    };
  }, [token]);

  async function login(identifier: string, password: string) {
    const response = await api.auth.login(identifier, password);
    localStorage.setItem(TOKEN_KEY, response.token);
    setToken(response.token);
    setUser({
      userId: response.userId,
      username: response.username,
      email: response.email,
      cashBalance: response.cashBalance,
    });
  }

  async function register(
    username: string,
    email: string,
    password: string,
  ) {
    return api.auth.register(username, email, password);
  }

  async function logout() {
    const currentToken = token;

    try {
      if (currentToken) {
        await api.auth.logout(currentToken);
      }
    } catch (error) {
      if (
        !(error instanceof ApiRequestError) ||
        error.status !== 401
      ) {
        throw error;
      }
    } finally {
      localStorage.removeItem(TOKEN_KEY);
      setToken(null);
      setUser(null);
    }
  }

  async function refreshSession() {
    if (!token) {
      return;
    }

    setUser(await api.auth.session(token));
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isBootstrapping,
      login,
      register,
      logout,
      refreshSession,
    }),
    [token, user, isBootstrapping],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}