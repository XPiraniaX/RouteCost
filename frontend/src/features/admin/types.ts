import type { UserRole } from "../../types/auth";

export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
}

export interface AdminUserPage {
  content: AdminUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminUserFilters {
  search: string;
  role: UserRole | "";
  enabled: boolean | undefined;
}

export interface AdminDashboard {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  adminUsers: number;
  totalVehicles: number;
  totalFuelEntries: number;
  totalExpenses: number;
  totalReminders: number;
}
