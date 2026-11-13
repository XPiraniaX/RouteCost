import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { api } from "../../api/http";
import { validId } from "../../utils/ids";
import { expenseFiltersSchema } from "./expenseSchema";
import type {
  Expense,
  ExpenseFilters,
  ExpensePage,
  ExpenseRequest,
  ExpenseStatistics,
  SortOrder,
} from "./types";

const root = (id: number) => `/api/vehicles/${id}`;
export const expenseApi = {
  list: async (
    id: number,
    page: number,
    order: SortOrder,
    filters: ExpenseFilters,
  ) =>
    (
      await api.get<ExpensePage>(`${root(id)}/expenses`, {
        params: {
          page,
          size: 10,
          sort: [`expenseDate,${order}`, `id,${order}`],
          ...(filters.category ? { category: filters.category } : {}),
          ...(filters.dateFrom ? { dateFrom: filters.dateFrom } : {}),
          ...(filters.dateTo ? { dateTo: filters.dateTo } : {}),
        },
      })
    ).data,
  get: async (id: number, expenseId: number) =>
    (await api.get<Expense>(`${root(id)}/expenses/${expenseId}`)).data,
  statistics: async (id: number) =>
    (await api.get<ExpenseStatistics>(`${root(id)}/expense-statistics`)).data,
  save: async (id: number, data: ExpenseRequest, expenseId?: number) =>
    (expenseId
      ? await api.put<Expense>(`${root(id)}/expenses/${expenseId}`, data)
      : await api.post<Expense>(`${root(id)}/expenses`, data)
    ).data,
  remove: async (id: number, expenseId: number) => {
    await api.delete(`${root(id)}/expenses/${expenseId}`);
  },
};
export const useExpenses = (
  id: number,
  page: number,
  order: SortOrder,
  filters: ExpenseFilters,
) =>
  useQuery({
    queryKey: ["expenses", id, "list", page, order, filters],
    queryFn: () => expenseApi.list(id, page, order, filters),
    enabled: validId(id) && expenseFiltersSchema.safeParse(filters).success,
  });
export const useExpense = (id: number, expenseId: number) =>
  useQuery({
    queryKey: ["expenses", id, "entry", expenseId],
    queryFn: () => expenseApi.get(id, expenseId),
    enabled: validId(id) && validId(expenseId),
  });
export const useExpenseStatistics = (id: number) =>
  useQuery({
    queryKey: ["expenses", id, "statistics"],
    queryFn: () => expenseApi.statistics(id),
    enabled: validId(id),
  });
export const invalidateExpenses = (client: QueryClient, id: number) =>
  Promise.all([
    client.invalidateQueries({ queryKey: ["expenses", id] }),
    client.invalidateQueries({ queryKey: ["vehicles"] }),
    client.invalidateQueries({ queryKey: ["dashboard"] }),
  ]);
export function useSaveExpense(id: number, expenseId?: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (data: ExpenseRequest) => expenseApi.save(id, data, expenseId),
    onSuccess: () => invalidateExpenses(client, id),
  });
}
export function useDeleteExpense(id: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (expenseId: number) => expenseApi.remove(id, expenseId),
    onSuccess: () => invalidateExpenses(client, id),
  });
}
