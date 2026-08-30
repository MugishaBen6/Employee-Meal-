import React, { createContext, useContext, useState } from 'react';
import { JwtResponse, Role, User } from '../types';
import { authApi } from '../api/authApi';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<User>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
  getDashboardPath: () => string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('jwtToken'));
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const login = async (usernameOrEmail: string, password: string): Promise<User> => {
    setIsLoading(true);
    try {
      const response = await authApi.login({ usernameOrEmail, password });
      const jwtData: JwtResponse = response.data;
      localStorage.setItem('jwtToken', jwtData.token);
      localStorage.setItem('user', JSON.stringify(jwtData.user));
      setToken(jwtData.token);
      setUser(jwtData.user);
      return jwtData.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const hasRole = (...roles: Role[]) => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  const getDashboardPath = () => {
    if (!user) return '/login';
    switch (user.role) {
      case 'ADMIN':
        return '/admin/dashboard';
      case 'MANAGING_DIRECTOR':
        return '/director/dashboard';
      case 'ACCOUNTANT':
        return '/accountant/dashboard';
      case 'HR':
        return '/hr/dashboard';
      default:
        return '/dashboard';
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        logout,
        hasRole,
        getDashboardPath,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
