package pl.routecost.expense.dto;

import java.util.List;

public record ExpenseStatisticsResponse(
        Long vehicleId,
        long expenseCount,
        List<ExpenseCurrencyTotal> totalsByCurrency,
        List<ExpenseCategoryTotal> totalsByCategory,
        List<ExpenseMonthlyTotal> monthlyTotals
) {
}
