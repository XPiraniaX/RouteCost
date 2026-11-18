package pl.routecost.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.expense.Expense;
import pl.routecost.expense.ExpenseCategory;
import pl.routecost.expense.ExpenseRepository;
import pl.routecost.fuel.FuelEntry;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.reminder.Reminder;
import pl.routecost.reminder.ReminderRepository;
import pl.routecost.reminder.ReminderStatus;
import pl.routecost.reminder.ReminderType;
import pl.routecost.user.JwtService;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(properties = "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class DashboardIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void returnsZeroCountsForAnEmptyUserAndRequiresAuthentication() throws Exception {
        String token = createToken(createUser());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCount").value(0))
                .andExpect(jsonPath("$.fuelEntryCount").value(0))
                .andExpect(jsonPath("$.expenseCount").value(0))
                .andExpect(jsonPath("$.activeReminderCount").value(0))
                .andExpect(jsonPath("$.overdueReminderCount").value(0))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(0))
                .andExpect(jsonPath("$.totalFuelCost").value(0))
                .andExpect(jsonPath("$.maintenanceCostLastThreeMonths").value(0));
    }

    @Test
    void sumsCostsAcrossAllOwnedVehiclesAndLimitsRecentCostsToTheLastThreeMonths() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle first = createVehicle(owner, "Toyota");
        Vehicle second = createVehicle(owner, "Honda");
        Vehicle anotherUsersVehicle = createVehicle(createUser(), "Mazda");
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusMonths(3);
        LocalDate beforeCutoff = cutoff.minusDays(1);

        fuelEntryRepository.saveAndFlush(fuel(first, beforeCutoff.toString(), 10_000, "20.000", "100.00", true));
        fuelEntryRepository.saveAndFlush(fuel(first, cutoff.toString(), 10_100, "10.000", "50.00", true));
        fuelEntryRepository.saveAndFlush(fuel(second, today.toString(), 20_000, "40.000", "200.00", true));
        fuelEntryRepository.saveAndFlush(fuel(anotherUsersVehicle, today.toString(), 30_000, "60.000", "3000.00", true));
        expenseRepository.saveAndFlush(expense(first, beforeCutoff.toString(), "REPAIR", "400.00"));
        expenseRepository.saveAndFlush(expense(first, cutoff.toString(), "PARTS", "25.00"));
        expenseRepository.saveAndFlush(expense(second, today.toString(), "SERVICE", "50.00"));
        expenseRepository.saveAndFlush(expense(anotherUsersVehicle, today.toString(), "REPAIR", "2000.00"));

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFuelCost").value(350))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(825))
                .andExpect(jsonPath("$.maintenanceCostLastThreeMonths").value(325));
    }

    @Test
    void sumsDashboardCountsAcrossSeveralVehiclesAndOnlyCountsActiveOverdueReminders() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle first = createVehicle(owner, "Toyota");
        Vehicle second = createVehicle(owner, "Honda");
        Vehicle otherOwnersCar = createVehicle(createUser(), "Mazda");

        fuelEntryRepository.saveAndFlush(fuel(first, "2025-01-01", 10_000, "40.000", "200.00", true));
        fuelEntryRepository.saveAndFlush(fuel(second, "2025-01-01", 20_000, "35.000", "180.00", true));
        expenseRepository.saveAndFlush(expense(first, "2025-01-02", "REPAIR", "100.00"));
        expenseRepository.saveAndFlush(expense(second, "2025-01-02", "SERVICE", "80.00"));
        reminderRepository.saveAndFlush(reminder(first, LocalDate.now().plusDays(2)));
        reminderRepository.saveAndFlush(reminder(second, LocalDate.now().minusDays(2)));
        Reminder completed = reminder(otherOwnersCar, LocalDate.now().minusDays(1));
        completed.changeStatus(ReminderStatus.COMPLETED);
        reminderRepository.saveAndFlush(completed);

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCount").value(2))
                .andExpect(jsonPath("$.fuelEntryCount").value(2))
                .andExpect(jsonPath("$.expenseCount").value(2))
                .andExpect(jsonPath("$.activeReminderCount").value(2))
                .andExpect(jsonPath("$.overdueReminderCount").value(1));
    }

    @Test
    void reportsFuelConsumptionAndExpenseTotalsInPln() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle vehicle = createVehicle(owner, "Toyota");
        fuelEntryRepository.saveAndFlush(fuel(vehicle, "2025-01-10", 10_000, "40.000", "200.00", true));
        fuelEntryRepository.saveAndFlush(fuel(vehicle, "2025-02-10", 10_500, "45.000", "225.00", true));
        expenseRepository.saveAndFlush(expense(vehicle, "2025-02-12", "REPAIR", "100.00"));
        expenseRepository.saveAndFlush(expense(vehicle, "2025-02-13", "PARTS", "50.00"));
        expenseRepository.saveAndFlush(expense(vehicle, "2025-02-14", "INSURANCE", "20.00"));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/cost-summary", vehicle.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMaintenanceCost").value(595))
                .andExpect(jsonPath("$.fuel.totalCost").value(425))
                .andExpect(jsonPath("$.fuel.hasFuelData").value(true))
                .andExpect(jsonPath("$.fuel.averageConsumptionLitersPer100Km").value(9))
                .andExpect(jsonPath("$.fuel.averageConsumptionAvailable").value(true))
                .andExpect(jsonPath("$.fuel.validConsumptionIntervals").value(1))
                .andExpect(jsonPath("$.fuel.monthlyTotals.length()").value(2))
                .andExpect(jsonPath("$.otherExpenses.expenseCount").value(3))
                .andExpect(jsonPath("$.otherExpenses.totalAmount").value(170))
                .andExpect(jsonPath("$.otherExpenses.totalsByCategory.length()").value(3))
                .andExpect(jsonPath("$.otherExpenses.monthlyTotals.length()").value(1));
    }

    @Test
    void calculatesMonthlyMaintenanceAverageAcrossInclusiveCalendarMonths() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle vehicle = createVehicle(owner, "Toyota");
        LocalDate today = LocalDate.now();
        LocalDate oldestExpense = YearMonth.from(today).minusMonths(3).atDay(5);
        fuelEntryRepository.saveAndFlush(fuel(vehicle, today.toString(), 11_000, "40.000", "300.00", true));
        expenseRepository.saveAndFlush(expense(vehicle, oldestExpense.toString(), "REPAIR", "100.00"));
        int expectedMonths = (int) ChronoUnit.MONTHS.between(
                YearMonth.from(oldestExpense), YearMonth.from(today)
        ) + 1;

        mockMvc.perform(get("/api/vehicles/{vehicleId}/cost-summary", vehicle.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMaintenanceCost").value(400))
                .andExpect(jsonPath("$.averageMonthlyMaintenanceCost").value(100))
                .andExpect(jsonPath("$.maintenanceHistoryMonthCount").value(expectedMonths))
                .andExpect(jsonPath("$.hasMaintenanceHistory").value(true));
    }

    @Test
    void usesOneMonthForCurrentMonthOnlyHistory() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle vehicle = createVehicle(owner, "Toyota");
        LocalDate currentMonthDate = YearMonth.now().atDay(1);
        fuelEntryRepository.saveAndFlush(fuel(vehicle, currentMonthDate.toString(), 10_000, "30.000", "200.00", true));
        expenseRepository.saveAndFlush(expense(vehicle, currentMonthDate.toString(), "SERVICE", "100.00"));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/cost-summary", vehicle.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageMonthlyMaintenanceCost").value(300))
                .andExpect(jsonPath("$.maintenanceHistoryMonthCount").value(1));
    }

    @Test
    void reportsUnavailableConsumptionWhenNoFullTankIntervalCanBeFormed() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle vehicle = createVehicle(owner, "Toyota");
        fuelEntryRepository.saveAndFlush(fuel(vehicle, "2025-01-10", 10_000, "40.000", "200.00", false));
        expenseRepository.saveAndFlush(expense(vehicle, "2025-01-11", "REPAIR", "100.00"));
        expenseRepository.saveAndFlush(expense(vehicle, "2025-01-12", "PARTS", "20.00"));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/cost-summary", vehicle.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuel.hasFuelData").value(true))
                .andExpect(jsonPath("$.fuel.averageConsumptionLitersPer100Km").value(nullValue()))
                .andExpect(jsonPath("$.fuel.averageConsumptionAvailable").value(false))
                .andExpect(jsonPath("$.fuel.validConsumptionIntervals").value(0))
                .andExpect(jsonPath("$.otherExpenses.totalAmount").value(120));
    }

    @Test
    void reportsUnavailableConsumptionWhenThereAreNoFuelEntries() throws Exception {
        User owner = createUser();
        String token = createToken(owner);
        Vehicle vehicle = createVehicle(owner, "Toyota");

        mockMvc.perform(get("/api/vehicles/{vehicleId}/cost-summary", vehicle.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuel.totalCost").value(0))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(0))
                .andExpect(jsonPath("$.averageMonthlyMaintenanceCost").value(nullValue()))
                .andExpect(jsonPath("$.maintenanceHistoryMonthCount").value(0))
                .andExpect(jsonPath("$.hasMaintenanceHistory").value(false))
                .andExpect(jsonPath("$.fuel.fuelEntryCount").value(0))
                .andExpect(jsonPath("$.fuel.hasFuelData").value(false))
                .andExpect(jsonPath("$.fuel.averageConsumptionLitersPer100Km").value(nullValue()))
                .andExpect(jsonPath("$.fuel.averageConsumptionAvailable").value(false))
                .andExpect(jsonPath("$.fuel.monthlyTotals").isEmpty());
    }

    @Test
    void doesNotAllowCostSummaryForAnotherUsersVehicle() throws Exception {
        String token = createToken(createUser());
        Vehicle vehicle = createVehicle(createUser(), "Honda");

        mockMvc.perform(get("/api/vehicles/{vehicleId}/cost-summary", vehicle.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    private User createUser() {
        return userRepository.saveAndFlush(new User(
                "dashboard-" + UUID.randomUUID() + "@routecost.example", "hash", "Test", "User"
        ));
    }

    private String createToken(User user) {
        return jwtService.createAuthResponse(user).accessToken();
    }

    private Vehicle createVehicle(User owner, String make) {
        return vehicleRepository.saveAndFlush(new Vehicle(
                owner,
                make,
                "Model",
                2020,
                FuelType.PETROL,
                25_000,
                LocalDate.of(2020, 1, 1),
                new BigDecimal("10000.00")
        ));
    }

    private FuelEntry fuel(
            Vehicle vehicle,
            String date,
            int odometer,
            String liters,
            String cost,
            boolean fullTank
    ) {
        return new FuelEntry(
                vehicle, LocalDate.parse(date), odometer,
                new BigDecimal(liters), new BigDecimal(cost), fullTank
        );
    }

    private Expense expense(Vehicle vehicle, String date, String category, String amount) {
        return new Expense(
                vehicle, LocalDate.parse(date), ExpenseCategory.valueOf(category),
                new BigDecimal(amount), null, null
        );
    }

    private Reminder reminder(Vehicle vehicle, LocalDate dueDate) {
        return new Reminder(vehicle, ReminderType.SERVICE, "Service", null, dueDate, null);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
