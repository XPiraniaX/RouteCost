import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { api } from "../../api/http";
import type { UserRole } from "../../types/auth";
import type {
  AdminDashboard,
  AdminUser,
  AdminUserFilters,
  AdminUserPage,
} from "./types";

const pageSize = 10;

export const adminApi = {
  dashboard: async () =>
    (await api.get<AdminDashboard>("/api/admin/dashboard")).data,
  users: async (page: number, filters: AdminUserFilters) =>
    (
      await api.get<AdminUserPage>("/api/admin/users", {
        params: {
          page,
          size: pageSize,
          sort: "createdAt,desc",
          ...(filters.search.trim() ? { search: filters.search.trim() } : {}),
          ...(filters.role ? { role: filters.role } : {}),
          ...(filters.enabled !== undefined ? { enabled: filters.enabled } : {}),
        },
      })
    ).data,
  changeRole: async (userId: number, role: UserRole) =>
    (await api.patch<AdminUser>(`/api/admin/users/${userId}/role`, { role })).data,
  changeStatus: async (userId: number, enabled: boolean) =>
    (
      await api.patch<AdminUser>(`/api/admin/users/${userId}/status`, { enabled })
    ).data,
};

export const useAdminDashboard = () =>
  useQuery({ queryKey: ["admin", "dashboard"], queryFn: adminApi.dashboard });

export const useAdminUsers = (page: number, filters: AdminUserFilters) =>
  useQuery({
    queryKey: ["admin", "users", page, filters],
    queryFn: () => adminApi.users(page, filters),
  });

export const invalidateAdmin = (client: QueryClient) =>
  Promise.all([
    client.invalidateQueries({ queryKey: ["admin", "dashboard"] }),
    client.invalidateQueries({ queryKey: ["admin", "users"] }),
  ]);

export function useChangeAdminRole() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: UserRole }) =>
      adminApi.changeRole(userId, role),
    onSuccess: () => invalidateAdmin(client),
  });
}

export function useChangeAdminStatus() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, enabled }: { userId: number; enabled: boolean }) =>
      adminApi.changeStatus(userId, enabled),
    onSuccess: () => invalidateAdmin(client),
  });
}
