package pl.routecost.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long vehicleCount,
        long fuelEntryCount,
        long expenseCount,
        long activeReminderCount,
        long overdueReminderCount,
        BigDecimal totalMaintenanceCost,
        BigDecimal totalFuelCost,
        BigDecimal maintenanceCostLastThreeMonths
) {
}
