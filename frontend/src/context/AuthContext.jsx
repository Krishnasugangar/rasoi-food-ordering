import { createContext, useContext, useMemo, useState } from "react";
import api from "../api/axios";

const AuthContext = createContext(null);

function readUser() {
  const raw = localStorage.getItem("rasoi_user");
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  const value = useMemo(() => {
    function persist(nextUser, token) {
      localStorage.setItem("rasoi_token", token);
      localStorage.setItem("rasoi_user", JSON.stringify(nextUser));
      setUser(nextUser);
    }

    async function login(email, password) {
      const response = await api.post("/auth/login", { email, password });
      const { token, ...profile } = response.data;
      persist(profile, token);
      return profile;
    }

    async function register(payload) {
      const response = await api.post("/auth/register", payload);
      const { token, ...profile } = response.data;
      persist(profile, token);
      return profile;
    }

    function logout() {
      localStorage.removeItem("rasoi_token");
      localStorage.removeItem("rasoi_user");
      setUser(null);
    }

    return {
      user,
      isAdmin: user?.role === "ADMIN",
      login,
      register,
      logout,
    };
  }, [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
