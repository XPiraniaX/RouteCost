import { Navigate, Route, Routes } from "react-router-dom";
import { AuthLayout } from "./layouts/AuthLayout";
import { AppLayout } from "./layouts/AppLayout";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { VehiclesPage } from "./features/vehicles/VehiclesPage";
import { VehicleFormPage } from "./features/vehicles/VehicleFormPage";
import { VehicleDetailPage } from "./features/vehicles/VehicleDetailPage";
import { LoginPage } from "./features/auth/LoginPage";
import { RegisterPage } from "./features/auth/RegisterPage";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { PublicOnlyRoute } from "./routes/PublicOnlyRoute";
import { FuelPage } from "./features/fuel/FuelPage";
import { FuelFormPage } from "./features/fuel/FuelFormPage";

export function App() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/vehicles" element={<VehiclesPage />} />
          <Route path="/fuel" element={<FuelPage />} />
          <Route path="/fuel/:vehicleId/new" element={<FuelFormPage />} />
          <Route
            path="/fuel/:vehicleId/:entryId/edit"
            element={<FuelFormPage />}
          />
          <Route path="/vehicles/new" element={<VehicleFormPage />} />
          <Route path="/vehicles/:vehicleId" element={<VehicleDetailPage />} />
          <Route
            path="/vehicles/:vehicleId/edit"
            element={<VehicleFormPage />}
          />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
