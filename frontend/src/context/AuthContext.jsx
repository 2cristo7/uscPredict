import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { setAccessToken, clearAccessToken, authAPI } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const login = useCallback(async (email, password) => {
    const response = await authAPI.v1.login(email, password);

    const authHeader = response.headers['authorization'];
    const jwt = authHeader?.replace('Bearer ', '');

    if (jwt) {
      setAccessToken(jwt);
      setUser(response.data);
    }

    return response.data;
  }, []);

  const register = useCallback(async (name, email, password) => {
    await authAPI.v1.register(name, email, password);
    // Auto-login after successful registration
    return await login(email, password);
  }, [login]);

  const logout = useCallback(async () => {
    try {
      await authAPI.v1.logout();
    } catch {
      // Continue with logout even if server call fails
    }
    clearAccessToken();
    setUser(null);
  }, []);

  const refreshToken = useCallback(async () => {
    try {
      const response = await authAPI.v1.refresh();

      const authHeader = response.headers['authorization'];
      const jwt = authHeader?.replace('Bearer ', '');

      if (jwt) {
        setAccessToken(jwt);
        setUser(response.data);
        return jwt;
      }
    } catch (e) {
      clearAccessToken();
      setUser(null);
      throw e;
    }
  }, []);

  // Listen for logout events from interceptor
  useEffect(() => {
    const handleLogout = () => {
      setUser(null);
    };
    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  // Try to refresh token on mount
  useEffect(() => {
    refreshToken()
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [refreshToken]);

  const value = {
    user,
    loading,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
    login,
    register,
    logout,
    refreshToken
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
