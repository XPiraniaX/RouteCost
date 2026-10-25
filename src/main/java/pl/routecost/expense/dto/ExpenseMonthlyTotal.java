package pl.routecost.expense.dto;

import java.math.BigDecimal;

public record ExpenseMonthlyTotal(
        int year,
        int month,
        String currency,
        BigDecimal totalAmount,
        long count
) {
}
