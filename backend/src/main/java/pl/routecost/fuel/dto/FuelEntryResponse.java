package pl.routecost.fuel.dto;

import pl.routecost.fuel.FuelEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record FuelEntryResponse(
        Long id,
        Long vehicleId,
        LocalDate entryDate,
        int odometer,
        BigDecimal liters,
        BigDecimal totalCost,
        BigDecimal pricePerLiter,
        boolean fullTank,
        BigDecimal consumptionFromPreviousFullTank
) {

    public static FuelEntryResponse from(FuelEntry entry, BigDecimal consumptionFromPreviousFullTank) {
        BigDecimal pricePerLiter = entry.getTotalCost()
                .divide(entry.getLiters(), 4, RoundingMode.HALF_UP);
        return new FuelEntryResponse(
                entry.getId(),
                entry.getVehicle().getId(),
                entry.getEntryDate(),
                entry.getOdometer(),
                entry.getLiters(),
                entry.getTotalCost(),
                pricePerLiter,
                entry.isFullTank(),
                consumptionFromPreviousFullTank
        );
    }
}
