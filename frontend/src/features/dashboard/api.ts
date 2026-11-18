import { useQuery } from "@tanstack/react-query";
import { api } from "../../api/http";

export interface DashboardSummary {
  vehicleCount: number;
  fuelEntryCount: number;
  expenseCount: number;
  activeReminderCount: number;
  overdueReminderCount: number;
  totalMaintenanceCost: number;
  totalFuelCost: number;
  maintenanceCostLastThreeMonths: number;
}

export const useSummary = () => useQuery({
  queryKey: ["dashboard"],
  queryFn: async () => (await api.get<DashboardSummary>("/api/dashboard/summary")).data,
});
