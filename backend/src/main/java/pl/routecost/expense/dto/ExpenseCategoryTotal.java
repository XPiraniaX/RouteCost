package pl.routecost.expense.dto;

import pl.routecost.expense.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseCategoryTotal(
        ExpenseCategory category,
        String currency,
        BigDecimal totalAmount,
        long count
) {
}
