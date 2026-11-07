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
        return new DashboardSummaryResponse(
                vehicleRepository.countByOwner_Id(ownerId),
                fuelEntryRepository.countByVehicle_Owner_Id(ownerId),
                expenseRepository.countByVehicle_Owner_Id(ownerId),
                reminderRepository.countByVehicle_Owner_IdAndStatus(ownerId, ReminderStatus.ACTIVE),
                reminderRepository.countByVehicle_Owner_IdAndStatusAndDueDateBefore(
                        ownerId, ReminderStatus.ACTIVE, LocalDate.now()
                )
        );
    }

    @Transactional(readOnly = true)
    public VehicleCostSummaryResponse findVehicleCostSummary(Long ownerId, Long vehicleId) {
        var fuelStatistics = fuelService.findFuelStatistics(ownerId, vehicleId);
        var expenseStatistics = expenseService.findExpenseStatistics(ownerId, vehicleId);
        var monthlyFuelCosts = fuelEntryRepository.findMonthlyCostsByVehicleId(vehicleId);
        boolean hasAverageConsumption = fuelStatistics.averageConsumptionLitersPer100Km() != null;

        return new VehicleCostSummaryResponse(
                vehicleId,
                new FuelCostSummary(
                        fuelStatistics.totalCost(),
                        null,
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
}
