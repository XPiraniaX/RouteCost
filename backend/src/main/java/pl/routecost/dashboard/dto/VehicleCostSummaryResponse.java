package pl.routecost.dashboard.dto;

import pl.routecost.expense.dto.ExpenseStatisticsResponse;

import java.math.BigDecimal;

public record VehicleCostSummaryResponse(
        Long vehicleId,
        BigDecimal totalMaintenanceCost,
        BigDecimal averageMonthlyMaintenanceCost,
        int maintenanceHistoryMonthCount,
        boolean hasMaintenanceHistory,
        FuelCostSummary fuel,
        ExpenseStatisticsResponse otherExpenses
) {
}
