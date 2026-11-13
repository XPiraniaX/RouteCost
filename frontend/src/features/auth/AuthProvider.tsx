import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { getCurrentUser, logoutSession } from "../../api/auth";
import { toApiError } from "../../api/errors";
import {
  refreshAccessToken,
  setAccessToken,
  setUnauthorizedHandler,
} from "../../api/http";
import { AuthContext } from "./authContext";
import type { AuthResponse, UserProfile } from "../../types/auth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const clearLocalSession = useCallback(() => {
    setAccessToken(null);
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  const signOut = useCallback(async () => {
    setAuthError(null);
    try {
      await logoutSession();
      clearLocalSession();
    } catch (error) {
      setAuthError(toApiError(error).message);
    }
  }, [clearLocalSession]);

  useEffect(() => {
    let mounted = true;
    setUnauthorizedHandler(() => {
      if (mounted) clearLocalSession();
    });

    void (async () => {
      try {
        const token = await refreshAccessToken();
        if (!token) return;
        const currentUser = await getCurrentUser();
        if (mounted) setUser(currentUser);
      } catch {
        setAccessToken(null);
        if (mounted) setUser(null);
      } finally {
        if (mounted) setIsInitializing(false);
      }
    })();

    return () => {
      mounted = false;
      setUnauthorizedHandler(null);
    };
  }, [clearLocalSession]);

  const establishSession = useCallback(async (response: AuthResponse) => {
    setAuthError(null);
    setAccessToken(response.accessToken);
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    } catch (error) {
      setAccessToken(null);
      setUser(null);
      try {
        await logoutSession();
      } catch {
        // Keep the original authentication error visible to the caller.
      }
      throw error;
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: user !== null,
      isInitializing,
      authError,
      establishSession,
      signOut,
    }),
    [user, isInitializing, authError, establishSession, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
