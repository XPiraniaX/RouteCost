import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";
import { Loading } from "../components/ui";

export function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth();
  const location = useLocation();

  if (isInitializing) return <Loading />;
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
