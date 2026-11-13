import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { api } from "../../api/http";
import { vehicleApi } from "../vehicles/api";

export interface FuelRequest {
  entryDate: string;
  odometer: number;
  liters: number;
  totalCost: number;
  fullTank: boolean;
}
export interface FuelEntry extends FuelRequest {
  id: number;
  vehicleId: number;
  pricePerLiter: number;
  consumptionFromPreviousFullTank: number | null;
}
export interface FuelPage {
  content: FuelEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface FuelStatistics {
  vehicleId: number;
  averageConsumptionLitersPer100Km: number | null;
  totalLiters: number;
  totalCost: number;
  fuelEntryCount: number;
  validConsumptionIntervals: number;
}
export type SortOrder = "asc" | "desc";
const root = (id: number) => `/api/vehicles/${id}`;
export const fuelApi = {
  list: async (id: number, page: number, order: SortOrder) =>
    (
      await api.get<FuelPage>(`${root(id)}/fuel-entries`, {
        params: { page, size: 10, sort: [`entryDate,${order}`, `id,${order}`] },
        paramsSerializer: { indexes: null },
      })
    ).data,
  get: async (id: number, entryId: number) =>
    (await api.get<FuelEntry>(`${root(id)}/fuel-entries/${entryId}`)).data,
  statistics: async (id: number) =>
    (await api.get<FuelStatistics>(`${root(id)}/fuel-statistics`)).data,
  save: async (id: number, data: FuelRequest, entryId?: number) =>
    (entryId
      ? await api.put<FuelEntry>(`${root(id)}/fuel-entries/${entryId}`, data)
      : await api.post<FuelEntry>(`${root(id)}/fuel-entries`, data)
    ).data,
  remove: async (id: number, entryId: number) => {
    await api.delete(`${root(id)}/fuel-entries/${entryId}`);
  },
};
export const validId = (id: number) => Number.isSafeInteger(id) && id > 0;
export const useFuelEntries = (id: number, page: number, order: SortOrder) =>
  useQuery({
    queryKey: ["fuel", id, "list", page, order],
    queryFn: () => fuelApi.list(id, page, order),
    enabled: validId(id),
  });
export const useFuelEntry = (id: number, entryId: number) =>
  useQuery({
    queryKey: ["fuel", id, "entry", entryId],
    queryFn: () => fuelApi.get(id, entryId),
    enabled: validId(id) && validId(entryId),
  });
export const useFuelStatistics = (id: number) =>
  useQuery({
    queryKey: ["fuel", id, "statistics"],
    queryFn: () => fuelApi.statistics(id),
    enabled: validId(id),
  });
export const useVehicleOptions = () =>
  useInfiniteQuery({
    queryKey: ["vehicles", "selection"],
    initialPageParam: 0,
    queryFn: ({ pageParam }) => vehicleApi.list(pageParam, "", 50),
    getNextPageParam: (page) =>
      page.page + 1 < page.totalPages ? page.page + 1 : undefined,
  });
export const invalidateFuel = (client: QueryClient, id: number) =>
  Promise.all([
    client.invalidateQueries({ queryKey: ["fuel", id] }),
    client.invalidateQueries({ queryKey: ["vehicles"] }),
    client.invalidateQueries({ queryKey: ["dashboard"] }),
  ]);
export function useSaveFuel(id: number, entryId?: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (data: FuelRequest) => fuelApi.save(id, data, entryId),
    onSuccess: () => invalidateFuel(client, id),
  });
}
export function useDeleteFuel(id: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (entryId: number) => fuelApi.remove(id, entryId),
    onSuccess: () => invalidateFuel(client, id),
  });
}
