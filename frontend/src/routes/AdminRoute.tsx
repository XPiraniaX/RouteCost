import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";
import { Loading } from "../components/ui";

export function AdminRoute() {
  const { user, isInitializing } = useAuth();
  if (isInitializing) return <Loading />;
  if (user?.role !== "ADMIN") return <Navigate to="/" replace />;
  return <Outlet />;
}
