package pl.routecost.expense.dto;

import java.math.BigDecimal;

public record ExpenseCurrencyTotal(String currency, BigDecimal totalAmount, long count) {
}
