export const expenseCategories = {
  REPAIR: "Naprawy",
  PARTS: "Części",
  SERVICE: "Serwis",
  INSURANCE: "Ubezpieczenie",
  TECHNICAL_INSPECTION: "Przegląd techniczny",
  TIRES: "Opony",
  CAR_WASH: "Myjnia",
  OTHER: "Inne",
} as const;
export type ExpenseCategory = keyof typeof expenseCategories;
export const categoryLabel = (category: string) =>
  Object.hasOwn(expenseCategories, category)
    ? expenseCategories[category as ExpenseCategory]
    : category;
export interface ExpenseRequest {
  expenseDate: string;
  category: ExpenseCategory;
  amount: number;
  description: string | null;
  odometer: number | null;
}
export interface Expense extends ExpenseRequest {
  id: number;
  vehicleId: number;
  createdAt: string;
}
export interface ExpensePage {
  content: Expense[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface ExpenseStatistics {
  vehicleId: number;
  expenseCount: number;
  totalAmount: number;
  totalsByCategory: {
    category: ExpenseCategory;
    totalAmount: number;
    count: number;
  }[];
  monthlyTotals: {
    year: number;
    month: number;
    totalAmount: number;
    count: number;
  }[];
}
export interface ExpenseFilters {
  category: string;
  dateFrom: string;
  dateTo: string;
}
export type SortOrder = "asc" | "desc";
