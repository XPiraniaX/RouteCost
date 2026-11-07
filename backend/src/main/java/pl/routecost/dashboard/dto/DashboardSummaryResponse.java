package pl.routecost.dashboard.dto;

public record DashboardSummaryResponse(
        long vehicleCount,
        long fuelEntryCount,
        long expenseCount,
        long activeReminderCount,
        long overdueReminderCount
) {
}
