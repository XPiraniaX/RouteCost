package pl.routecost.fuel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuelEntryRequest(
        @NotNull @PastOrPresent LocalDate entryDate,
        @NotNull @PositiveOrZero Integer odometer,
        @NotNull @DecimalMin(value = "0.000", inclusive = false) @Digits(integer = 7, fraction = 3)
        BigDecimal liters,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal totalCost,
        @NotNull Boolean fullTank
) {
}
