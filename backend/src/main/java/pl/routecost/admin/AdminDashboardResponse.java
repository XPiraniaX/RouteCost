package pl.routecost.admin;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long adminUsers,
        long totalVehicles,
        long totalFuelEntries,
        long totalExpenses,
        long totalReminders
) {
}
