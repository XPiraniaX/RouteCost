package pl.routecost.fuel.dto;

import java.math.BigDecimal;

public record FuelStatisticsResponse(
        Long vehicleId,
        BigDecimal averageConsumptionLitersPer100Km,
        BigDecimal totalLiters,
        BigDecimal totalCost,
        long fuelEntryCount,
        int validConsumptionIntervals
) {
}
