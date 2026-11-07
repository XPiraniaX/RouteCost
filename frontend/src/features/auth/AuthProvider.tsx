import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { getCurrentUser } from "../../api/auth";
import { setAccessToken, setUnauthorizedHandler } from "../../api/http";
import { AuthContext } from "./authContext";
import type { AuthResponse, UserProfile } from "../../types/auth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const queryClient = useQueryClient();

  const signOut = useCallback(() => {
    setAccessToken(null);
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    setUnauthorizedHandler(signOut);
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  const establishSession = useCallback(async (response: AuthResponse) => {
    setAccessToken(response.accessToken);
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    } catch (error) {
      signOut();
      throw error;
    }
  }, [signOut]);

  const value = useMemo(() => ({
    user,
    isAuthenticated: user !== null,
    establishSession,
    signOut,
  }), [user, establishSession, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
