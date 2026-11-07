import { createContext } from "react";
import type { AuthResponse, UserProfile } from "../../types/auth";

export interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  establishSession: (response: AuthResponse) => Promise<void>;
  signOut: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
