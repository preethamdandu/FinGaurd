import { useState, useEffect } from 'react';
import { auth, users } from '../services/api';
import { AuthContext } from './auth-context';

function initialLoading() {
  if (typeof window === 'undefined') return true;
  return !!localStorage.getItem('accessToken');
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(initialLoading);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;
    users.me().then(setUser).catch(() => localStorage.clear()).finally(() => setLoading(false));
  }, []);

  const login = async (email, password) => {
    const res = await auth.login({ email, password });
    setUser(res.user);
    return res;
  };

  const signup = async (data) => {
    await auth.signup(data);
  };

  const logout = async () => {
    await auth.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, setUser, login, signup, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}
