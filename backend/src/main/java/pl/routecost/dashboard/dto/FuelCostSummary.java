package pl.routecost.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

public record FuelCostSummary(
        BigDecimal totalCost,
        @JsonInclude(JsonInclude.Include.ALWAYS) String currency,
        long fuelEntryCount,
        boolean hasFuelData,
        @JsonInclude(JsonInclude.Include.ALWAYS) BigDecimal averageConsumptionLitersPer100Km,
        boolean averageConsumptionAvailable,
        int validConsumptionIntervals,
        List<MonthlyFuelCost> monthlyTotals
) {
}
