package pl.routecost.expense.dto;

import java.util.List;
import java.math.BigDecimal;

public record ExpenseStatisticsResponse(
        Long vehicleId,
        long expenseCount,
        BigDecimal totalAmount,
        List<ExpenseCategoryTotal> totalsByCategory,
        List<ExpenseMonthlyTotal> monthlyTotals
) {
}
