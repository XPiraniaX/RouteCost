package pl.routecost.vehicle.dto;

import pl.routecost.vehicle.FuelType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleRequest(
        @NotBlank @Size(max = 100) String make,
        @NotBlank @Size(max = 100) String model,
        @Min(1886) @Max(2100) int productionYear,
        @NotNull FuelType fuelType,
        @PositiveOrZero int currentOdometer,
        @NotNull @PastOrPresent LocalDate purchaseDate,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal purchasePrice
) {
}
