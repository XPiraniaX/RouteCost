import { createContext } from "react";
import type { AuthResponse, UserProfile } from "../../types/auth";

export interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  authError: string | null;
  establishSession: (response: AuthResponse) => Promise<void>;
  signOut: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
