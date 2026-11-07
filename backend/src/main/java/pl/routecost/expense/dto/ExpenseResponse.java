package pl.routecost.expense.dto;

import pl.routecost.expense.Expense;
import pl.routecost.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ExpenseResponse(
        Long id,
        Long vehicleId,
        LocalDate expenseDate,
        ExpenseCategory category,
        BigDecimal amount,
        String currency,
        String description,
        Integer odometer,
        OffsetDateTime createdAt
) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getVehicle().getId(),
                expense.getExpenseDate(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getDescription(),
                expense.getOdometer(),
                expense.getCreatedAt()
        );
    }
}
