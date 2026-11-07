package pl.routecost.dashboard.dto;

import pl.routecost.expense.dto.ExpenseStatisticsResponse;

public record VehicleCostSummaryResponse(
        Long vehicleId,
        FuelCostSummary fuel,
        ExpenseStatisticsResponse otherExpenses
) {
}
