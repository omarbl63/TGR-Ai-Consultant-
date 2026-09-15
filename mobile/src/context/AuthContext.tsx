import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { AuthApi, UserApi } from '../api/services';
import { setAuthToken } from '../api/client';
import { AuthResponse, RegisterPayload, User } from '../types';

const TOKEN_KEY = 'adminai.token';
const USER_KEY = 'adminai.user';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore the session across app reloads: keep the stored token and validate it
    // against the server. A simple refresh stays signed in; only an invalid/expired
    // token or an unreachable server (or an explicit logout) returns to the login screen.
    (async () => {
      try {
        const [[, token], [, userJson]] = await AsyncStorage.multiGet([TOKEN_KEY, USER_KEY]);
        if (!token) {
          return; // never signed in
        }
        setAuthToken(token);
        // Throws on 401 (invalid/expired) or on a network error (server down) -> sign out.
        const me = await UserApi.me();
        setUser(me ?? (userJson ? (JSON.parse(userJson) as User) : null));
      } catch {
        setAuthToken(null);
        await AsyncStorage.multiRemove([TOKEN_KEY, USER_KEY]);
        setUser(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const persist = async (res: AuthResponse) => {
    setAuthToken(res.accessToken);
    await AsyncStorage.multiSet([
      [TOKEN_KEY, res.accessToken],
      [USER_KEY, JSON.stringify(res.user)],
    ]);
    setUser(res.user);
  };

  const login = async (email: string, password: string) => {
    await persist(await AuthApi.login(email, password));
  };

  const register = async (payload: RegisterPayload) => {
    await persist(await AuthApi.register(payload));
  };

  const logout = async () => {
    setAuthToken(null);
    await AsyncStorage.multiRemove([TOKEN_KEY, USER_KEY]);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
