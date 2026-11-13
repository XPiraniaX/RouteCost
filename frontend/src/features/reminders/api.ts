import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { api } from "../../api/http";
import { validId } from "../../utils/ids";
import { reminderFiltersSchema } from "./reminderSchema";
import type {
  Reminder,
  ReminderFilters,
  ReminderFinalStatus,
  ReminderPage,
  ReminderRequest,
} from "./types";

const root = (vehicleId: number) => `/api/vehicles/${vehicleId}/reminders`;

export const reminderApi = {
  list: async (vehicleId: number, page: number, filters: ReminderFilters) =>
    (
      await api.get<ReminderPage>(root(vehicleId), {
        params: {
          page,
          size: 10,
          sort: ["dueDate,asc", "id,asc"],
          ...(filters.status ? { status: filters.status } : {}),
          ...(filters.dueFrom ? { dueFrom: filters.dueFrom } : {}),
          ...(filters.dueTo ? { dueTo: filters.dueTo } : {}),
        },
        paramsSerializer: { indexes: null },
      })
    ).data,
  get: async (vehicleId: number, reminderId: number) =>
    (
      await api.get<Reminder>(`${root(vehicleId)}/${reminderId}`)
    ).data,
  save: async (
    vehicleId: number,
    data: ReminderRequest,
    reminderId?: number,
  ) =>
    (
      reminderId
        ? await api.put<Reminder>(`${root(vehicleId)}/${reminderId}`, data)
        : await api.post<Reminder>(root(vehicleId), data)
    ).data,
  changeStatus: async (
    vehicleId: number,
    reminderId: number,
    status: ReminderFinalStatus,
  ) =>
    (
      await api.patch<Reminder>(`${root(vehicleId)}/${reminderId}/status`, {
        status,
      })
    ).data,
  reactivate: async (vehicleId: number, reminderId: number) =>
    (
      await api.post<Reminder>(
        `${root(vehicleId)}/${reminderId}/reactivate`,
      )
    ).data,
  remove: async (vehicleId: number, reminderId: number) => {
    await api.delete(`${root(vehicleId)}/${reminderId}`);
  },
};

export const useReminders = (
  vehicleId: number,
  page: number,
  filters: ReminderFilters,
) =>
  useQuery({
    queryKey: ["reminders", vehicleId, "list", page, filters],
    queryFn: () => reminderApi.list(vehicleId, page, filters),
    enabled:
      validId(vehicleId) && reminderFiltersSchema.safeParse(filters).success,
  });

export const useReminder = (vehicleId: number, reminderId: number) =>
  useQuery({
    queryKey: ["reminders", vehicleId, "entry", reminderId],
    queryFn: () => reminderApi.get(vehicleId, reminderId),
    enabled: validId(vehicleId) && validId(reminderId),
  });

export const invalidateReminders = (client: QueryClient, vehicleId: number) =>
  Promise.all([
    client.invalidateQueries({ queryKey: ["reminders", vehicleId] }),
    client.invalidateQueries({ queryKey: ["dashboard"] }),
  ]);

export function useSaveReminder(vehicleId: number, reminderId?: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (data: ReminderRequest) =>
      reminderApi.save(vehicleId, data, reminderId),
    onSuccess: () => invalidateReminders(client, vehicleId),
  });
}

export function useChangeReminderStatus(vehicleId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({
      reminderId,
      status,
    }: {
      reminderId: number;
      status: ReminderFinalStatus;
    }) => reminderApi.changeStatus(vehicleId, reminderId, status),
    onSuccess: () => invalidateReminders(client, vehicleId),
  });
}

export function useReactivateReminder(vehicleId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (reminderId: number) =>
      reminderApi.reactivate(vehicleId, reminderId),
    onSuccess: () => invalidateReminders(client, vehicleId),
  });
}

export function useDeleteReminder(vehicleId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (reminderId: number) =>
      reminderApi.remove(vehicleId, reminderId),
    onSuccess: () => invalidateReminders(client, vehicleId),
  });
}
