import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { api } from "../../api/http";

export const fuels = {
  PETROL: "Benzyna",
  DIESEL: "Diesel",
  LPG: "LPG",
  HYBRID: "Hybryda",
  ELECTRIC: "Elektryczny",
  OTHER: "Inny",
} as const;
export type FuelType = keyof typeof fuels;
export interface VehicleRequest {
  make: string;
  model: string;
  productionYear: number;
  fuelType: FuelType;
  currentOdometer: number;
  purchaseDate: string;
  purchasePrice: number;
}
export interface Vehicle extends VehicleRequest {
  id: number;
  createdAt: string;
}
export interface VehiclePage {
  content: Vehicle[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface CostSummary {
  vehicleId: number;
  fuel: {
    totalCost: number;
    currency: null;
    fuelEntryCount: number;
    hasFuelData: boolean;
    averageConsumptionLitersPer100Km: number | null;
    averageConsumptionAvailable: boolean;
    validConsumptionIntervals: number;
    monthlyTotals: {
      year: number;
      month: number;
      totalCost: number;
      count: number;
    }[];
  };
  otherExpenses: {
    expenseCount: number;
    totalsByCurrency: {
      currency: string;
      totalAmount: number;
      count: number;
    }[];
    totalsByCategory: {
      category: string;
      currency: string;
      totalAmount: number;
      count: number;
    }[];
    monthlyTotals: {
      year: number;
      month: number;
      currency: string;
      totalAmount: number;
      count: number;
    }[];
  };
}
export const vehicleApi = {
  list: async (page: number, fuelType = "", size = 8) =>
    (
      await api.get<VehiclePage>("/api/vehicles", {
        params: { page, size, ...(fuelType ? { fuelType } : {}) },
      })
    ).data,
  get: async (id: number) =>
    (await api.get<Vehicle>(`/api/vehicles/${id}`)).data,
  save: async (data: VehicleRequest, id?: number) =>
    (id
      ? await api.put<Vehicle>(`/api/vehicles/${id}`, data)
      : await api.post<Vehicle>("/api/vehicles", data)
    ).data,
  remove: async (id: number) => {
    await api.delete(`/api/vehicles/${id}`);
  },
};
export const useVehicles = (page = 0, fuel = "", size = 8) =>
  useQuery({
    queryKey: ["vehicles", "list", page, fuel, size],
    queryFn: () => vehicleApi.list(page, fuel, size),
  });
export const useVehicle = (id: number) =>
  useQuery({
    queryKey: ["vehicles", id],
    queryFn: () => vehicleApi.get(id),
    enabled: Number.isSafeInteger(id) && id > 0,
  });
export const useCosts = (id: number) =>
  useQuery({
    queryKey: ["vehicles", id, "costs"],
    queryFn: async () =>
      (await api.get<CostSummary>(`/api/vehicles/${id}/cost-summary`)).data,
    enabled: Number.isSafeInteger(id) && id > 0,
  });

export const useVehicleOptions = () =>
  useInfiniteQuery({
    queryKey: ["vehicles", "selection"],
    initialPageParam: 0,
    queryFn: ({ pageParam }) => vehicleApi.list(pageParam, "", 50),
    getNextPageParam: (page) =>
      page.page + 1 < page.totalPages ? page.page + 1 : undefined,
  });
