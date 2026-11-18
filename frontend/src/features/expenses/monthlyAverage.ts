import type { ExpenseStatistics } from "./types";

type MonthlyTotal = ExpenseStatistics["monthlyTotals"][number];

export function averageMonthlyExpense(
  totalAmount: number,
  monthlyTotals: MonthlyTotal[],
  now = new Date(),
): number {
  if (monthlyTotals.length === 0) return 0;

  const earliest = monthlyTotals.reduce((first, month) =>
    month.year < first.year ||
    (month.year === first.year && month.month < first.month)
      ? month
      : first,
  );
  const months =
    (now.getFullYear() - earliest.year) * 12 +
    (now.getMonth() + 1 - earliest.month) +
    1;

  return months > 0 ? totalAmount / months : 0;
}
