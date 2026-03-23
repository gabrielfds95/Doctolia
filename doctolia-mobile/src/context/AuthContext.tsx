import React, { createContext, useContext, useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axiosInstance from '../api/axiosInstance';
import { AuthResponse } from '../model/types';

// — Types —
interface AuthContextType {
  user: AuthResponse | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

// — Création du contexte —
const AuthContext = createContext<AuthContextType | null>(null);

// — Provider : enveloppe l'app entière —
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [loading, setLoading] = useState(true);

  // Recharge l'utilisateur depuis le stockage au démarrage (useEffect sans dépendances = mount)
  useEffect(() => {
    AsyncStorage.getItem('current_user').then((json) => {
      if (json) setUser(JSON.parse(json));
      setLoading(false);
    });
  }, []);

  const login = async (username: string, password: string): Promise<void> => {
    const { data } = await axiosInstance.post<AuthResponse>('/login', { username, password });
    await AsyncStorage.setItem('jwt_token', data.token);
    await AsyncStorage.setItem('current_user', JSON.stringify(data));
    setUser(data);
  };

  const logout = async (): Promise<void> => {
    await AsyncStorage.removeItem('jwt_token');
    await AsyncStorage.removeItem('current_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// — Hook personnalisé pour consommer le contexte —
export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans un AuthProvider');
  return ctx;
}
