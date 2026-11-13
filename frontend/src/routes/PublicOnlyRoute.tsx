import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";
import { Loading } from "../components/ui";

export function PublicOnlyRoute() {
  const { isAuthenticated, isInitializing } = useAuth();
  if (isInitializing) return <Loading />;
  return isAuthenticated ? <Navigate to="/" replace /> : <Outlet />;
}
