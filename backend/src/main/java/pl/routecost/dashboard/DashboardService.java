package pl.routecost.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.dashboard.dto.DashboardSummaryResponse;
import pl.routecost.dashboard.dto.FuelCostSummary;
import pl.routecost.dashboard.dto.VehicleCostSummaryResponse;
import pl.routecost.expense.ExpenseRepository;
import pl.routecost.expense.ExpenseService;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.fuel.FuelService;
import pl.routecost.reminder.ReminderRepository;
import pl.routecost.reminder.ReminderStatus;
import pl.routecost.vehicle.VehicleRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final ExpenseRepository expenseRepository;
    private final ReminderRepository reminderRepository;
    private final FuelService fuelService;
    private final ExpenseService expenseService;

    public DashboardService(
            VehicleRepository vehicleRepository,
            FuelEntryRepository fuelEntryRepository,
            ExpenseRepository expenseRepository,
            ReminderRepository reminderRepository,
            FuelService fuelService,
            ExpenseService expenseService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.fuelEntryRepository = fuelEntryRepository;
        this.expenseRepository = expenseRepository;
        this.reminderRepository = reminderRepository;
        this.fuelService = fuelService;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse findSummary(Long ownerId) {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        BigDecimal totalFuelCost = zeroIfNull(fuelEntryRepository.sumTotalCostByOwnerId(ownerId));
        BigDecimal totalExpenseCost = zeroIfNull(expenseRepository.sumAmountByOwnerId(ownerId));
        return new DashboardSummaryResponse(
                vehicleRepository.countByOwner_Id(ownerId),
                fuelEntryRepository.countByVehicle_Owner_Id(ownerId),
                expenseRepository.countByVehicle_Owner_Id(ownerId),
                reminderRepository.countByVehicle_Owner_IdAndStatus(ownerId, ReminderStatus.ACTIVE),
                reminderRepository.countByVehicle_Owner_IdAndStatusAndDueDateBefore(
                        ownerId, ReminderStatus.ACTIVE, LocalDate.now()
                ),
                totalFuelCost.add(totalExpenseCost),
                totalFuelCost,
                zeroIfNull(fuelEntryRepository.sumTotalCostByOwnerIdFromDate(ownerId, threeMonthsAgo))
                        .add(zeroIfNull(expenseRepository.sumAmountByOwnerIdFromDate(ownerId, threeMonthsAgo)))
        );
    }

    @Transactional(readOnly = true)
    public VehicleCostSummaryResponse findVehicleCostSummary(Long ownerId, Long vehicleId) {
        var fuelStatistics = fuelService.findFuelStatistics(ownerId, vehicleId);
        var expenseStatistics = expenseService.findExpenseStatistics(ownerId, vehicleId);
        var monthlyFuelCosts = fuelEntryRepository.findMonthlyCostsByVehicleId(vehicleId);
        boolean hasAverageConsumption = fuelStatistics.averageConsumptionLitersPer100Km() != null;
        BigDecimal expenseTotal = expenseStatistics.totalAmount();
        BigDecimal totalMaintenanceCost = fuelStatistics.totalCost().add(expenseTotal);
        LocalDate oldestCostDate = Stream.of(
                        fuelEntryRepository.findFirstByVehicle_IdOrderByEntryDateAscIdAsc(vehicleId)
                                .map(entry -> entry.getEntryDate()).orElse(null),
                        expenseRepository.findFirstByVehicle_IdOrderByExpenseDateAscIdAsc(vehicleId)
                                .map(expense -> expense.getExpenseDate()).orElse(null)
                )
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        int historyMonthCount = oldestCostDate == null
                ? 0
                : (int) ChronoUnit.MONTHS.between(YearMonth.from(oldestCostDate), YearMonth.now()) + 1;
        boolean hasMaintenanceHistory = historyMonthCount > 0;
        BigDecimal averageMonthlyMaintenanceCost = hasMaintenanceHistory
                ? totalMaintenanceCost.divide(BigDecimal.valueOf(historyMonthCount), 2, RoundingMode.HALF_UP)
                : null;

        return new VehicleCostSummaryResponse(
                vehicleId,
                totalMaintenanceCost,
                averageMonthlyMaintenanceCost,
                historyMonthCount,
                hasMaintenanceHistory,
                new FuelCostSummary(
                        fuelStatistics.totalCost(),
                        fuelStatistics.fuelEntryCount(),
                        fuelStatistics.fuelEntryCount() > 0,
                        fuelStatistics.averageConsumptionLitersPer100Km(),
                        hasAverageConsumption,
                        fuelStatistics.validConsumptionIntervals(),
                        monthlyFuelCosts
                ),
                expenseStatistics
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
