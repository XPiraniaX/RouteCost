package pl.routecost.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pl.routecost.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull @PastOrPresent LocalDate expenseDate,
        @NotNull ExpenseCategory category,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal amount,
        @Size(max = 1000) String description,
        @PositiveOrZero Integer odometer
) {
}
