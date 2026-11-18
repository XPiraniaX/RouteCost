import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api/http";

export interface UserNotification {
  id: number;
  title: string;
  message: string;
  vehicleId: number;
  vehicleName: string;
  reminderId: number;
  createdAt: string;
  read: boolean;
  readAt: string | null;
}

export interface NotificationPage {
  content: UserNotification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const notificationApi = {
  list: async (page = 0) => (await api.get<NotificationPage>("/api/notifications", { params: { page, size: 20 } })).data,
  unreadCount: async () => (await api.get<{ count: number }>("/api/notifications/unread-count")).data.count,
  markRead: async (id: number) => (await api.patch<UserNotification>(`/api/notifications/${id}/read`)).data,
  markAllRead: async () => { await api.patch("/api/notifications/read-all"); },
};

export function useUnreadNotificationCount(userId?: number) {
  return useQuery({
    queryKey: ["notifications", userId, "unread-count"],
    queryFn: notificationApi.unreadCount,
    enabled: userId !== undefined,
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  });
}

export function useNotifications(userId?: number, enabled = true, page = 0) {
  return useQuery({
    queryKey: ["notifications", userId, "list", page],
    queryFn: () => notificationApi.list(page),
    enabled: userId !== undefined && enabled,
    refetchOnWindowFocus: true,
  });
}

export function useNotificationActions(userId?: number) {
  const client = useQueryClient();
  const invalidate = () => Promise.all([
    client.invalidateQueries({ queryKey: ["notifications", userId] }),
  ]);
  const markRead = useMutation({ mutationFn: notificationApi.markRead, onSuccess: invalidate });
  const markAllRead = useMutation({ mutationFn: notificationApi.markAllRead, onSuccess: invalidate });
  return { markRead, markAllRead };
}
