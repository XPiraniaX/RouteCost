package pl.routecost.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
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
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Creates local-only sample accounts when the explicit {@code dev} profile is active. */
@Component
@Profile("dev & !prod")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final List<AccountSpec> ACCOUNTS = List.of(
            new AccountSpec("demo.empty@routecost.local", "RouteCostDemo1!", "Demo", "Empty", Scenario.EMPTY),
            new AccountSpec("demo.standard@routecost.local", "RouteCostDemo2!", "Demo", "Standard", Scenario.STANDARD),
            new AccountSpec("demo.full@routecost.local", "RouteCostDemo3!", "Demo", "Full", Scenario.FULL)
    );

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final ExpenseRepository expenseRepository;
    private final ReminderRepository reminderRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataSeeder(
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            FuelEntryRepository fuelEntryRepository,
            ExpenseRepository expenseRepository,
            ReminderRepository reminderRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelEntryRepository = fuelEntryRepository;
        this.expenseRepository = expenseRepository;
        this.reminderRepository = reminderRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now(clock);
        for (AccountSpec account : ACCOUNTS) {
            if (userRepository.findByEmailIgnoreCase(account.email()).isPresent()) {
                log.info("Skipping existing local demo account {}", account.email());
                continue;
            }

            User user = userRepository.saveAndFlush(new User(
                    account.email(),
                    passwordEncoder.encode(account.password()),
                    account.firstName(),
                    account.lastName()
            ));
            seedScenario(user, account.scenario(), today);
            log.info("Created local demo account {} ({})", account.email(), account.scenario());
        }
    }

    private void seedScenario(User user, Scenario scenario, LocalDate today) {
        if (scenario == Scenario.EMPTY) {
            addVehicles(user, List.of(vehicleSpec("Toyota", "Corolla", 2019, FuelType.PETROL, 82_400, 45_000)));
            return;
        }

        List<Vehicle> vehicles = scenario == Scenario.STANDARD
                ? addVehicles(user, List.of(vehicleSpec("Skoda", "Octavia", 2020, FuelType.DIESEL, 108_600, 68_900)))
                : addVehicles(user, List.of(
                        vehicleSpec("Toyota", "Corolla", 2021, FuelType.HYBRID, 84_200, 92_500),
                        vehicleSpec("Kia", "Ceed", 2018, FuelType.PETROL, 126_700, 54_800)
                ));

        List<FuelEntry> fuelEntries = new ArrayList<>();
        List<Expense> expenses = new ArrayList<>();
        List<Reminder> reminders = new ArrayList<>();

        for (int vehicleIndex = 0; vehicleIndex < vehicles.size(); vehicleIndex++) {
            Vehicle vehicle = vehicles.get(vehicleIndex);
            int fuelCount = scenario == Scenario.STANDARD ? 6 : (vehicleIndex == 0 ? 11 : 7);
            int expenseCount = scenario == Scenario.STANDARD ? 5 : (vehicleIndex == 0 ? 11 : 7);
            fuelEntries.addAll(fuelEntries(vehicle, fuelCount, today));
            expenses.addAll(expenses(vehicle, expenseCount, today));
            reminders.addAll(reminders(vehicle, scenario, vehicleIndex, today));
        }

        fuelEntryRepository.saveAll(fuelEntries);
        expenseRepository.saveAll(expenses);
        reminderRepository.saveAll(reminders);
    }

    private List<Vehicle> addVehicles(User user, List<VehicleSpec> specs) {
        LocalDate today = LocalDate.now(clock);
        List<Vehicle> vehicles = specs.stream()
                .map(spec -> new Vehicle(
                        user,
                        spec.make(),
                        spec.model(),
                        spec.productionYear(),
                        spec.fuelType(),
                        spec.currentOdometer(),
                        today.minusYears(4),
                        money(spec.purchasePrice())
                ))
                .toList();
        return vehicleRepository.saveAll(vehicles);
    }

    private List<FuelEntry> fuelEntries(Vehicle vehicle, int count, LocalDate today) {
        List<FuelEntry> entries = new ArrayList<>();
        int odometerStep = 650;
        int firstOdometer = vehicle.getCurrentOdometer() - (count + 1) * odometerStep;
        for (int index = 0; index < count; index++) {
            LocalDate date = today.minusDays((long) (count - index) * 27 + 5);
            BigDecimal liters = BigDecimal.valueOf(40 + (index % 4) * 1.5).setScale(3);
            BigDecimal pricePerLiter = new BigDecimal("6.49");
            entries.add(new FuelEntry(
                    vehicle,
                    date,
                    firstOdometer + index * odometerStep,
                    liters,
                    liters.multiply(pricePerLiter).setScale(2, java.math.RoundingMode.HALF_UP),
                    index % 3 != 1
            ));
        }
        return entries;
    }

    private List<Expense> expenses(Vehicle vehicle, int count, LocalDate today) {
        ExpenseCategory[] categories = {
                ExpenseCategory.SERVICE,
                ExpenseCategory.INSURANCE,
                ExpenseCategory.CAR_WASH,
                ExpenseCategory.TIRES,
                ExpenseCategory.PARTS,
                ExpenseCategory.REPAIR,
                ExpenseCategory.TECHNICAL_INSPECTION,
                ExpenseCategory.OTHER,
                ExpenseCategory.SERVICE
        };
        String[] descriptions = {
                "Okresowy serwis i wymiana oleju", "Roczna polisa OC/AC", "Mycie i pielęgnacja auta",
                "Sezonowa wymiana opon", "Filtry i materiały eksploatacyjne", "Naprawa układu hamulcowego",
                "Badanie techniczne", "Opłata parkingowa w trasie", "Przegląd po sezonie"
        };
        BigDecimal[] amounts = {
                new BigDecimal("480.00"), new BigDecimal("1680.00"), new BigDecimal("45.00"),
                new BigDecimal("220.00"), new BigDecimal("165.00"), new BigDecimal("760.00"),
                new BigDecimal("99.00"), new BigDecimal("38.00"), new BigDecimal("390.00")
        };
        List<Expense> expenses = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            LocalDate date = today.minusDays((long) (count - index) * 27 + 5);
            int odometer = Math.min(vehicle.getCurrentOdometer(),
                    vehicle.getCurrentOdometer() - (count - index + 1) * 650);
            expenses.add(new Expense(
                    vehicle,
                    date,
                    categories[index % categories.length],
                    amounts[index % amounts.length],
                    "PLN",
                    descriptions[index % descriptions.length],
                    odometer
            ));
        }
        return expenses;
    }

    private List<Reminder> reminders(Vehicle vehicle, Scenario scenario, int vehicleIndex, LocalDate today) {
        List<Reminder> reminders = new ArrayList<>();
        if (scenario == Scenario.STANDARD) {
            reminders.add(reminder(vehicle, ReminderType.SERVICE, "Wymiana oleju", today.minusDays(18), 109_000, null));
            reminders.add(reminder(vehicle, ReminderType.TECHNICAL_INSPECTION, "Badanie techniczne", today.plusDays(5), 112_000, null));
            reminders.add(reminder(vehicle, ReminderType.INSURANCE, "Polisa OC", today.minusDays(75), null, ReminderStatus.COMPLETED));
            return reminders;
        }

        if (vehicleIndex == 0) {
            reminders.add(reminder(vehicle, ReminderType.SERVICE, "Przegląd okresowy", today.minusDays(12), 85_000, null));
            reminders.add(reminder(vehicle, ReminderType.OIL_CHANGE, "Wymiana oleju", today.plusDays(3), 86_000, null));
            reminders.add(reminder(vehicle, ReminderType.INSURANCE, "Odnowienie OC/AC", today.minusDays(55), null, ReminderStatus.COMPLETED));
            reminders.add(reminder(vehicle, ReminderType.TIRES, "Wymiana opon", today.minusDays(90), 80_000, ReminderStatus.CANCELLED));
        } else {
            reminders.add(reminder(vehicle, ReminderType.TECHNICAL_INSPECTION, "Badanie techniczne", today.plusDays(6), 128_000, null));
            reminders.add(reminder(vehicle, ReminderType.INSURANCE, "Polisa OC", today.plusDays(92), null, null));
            reminders.add(reminder(vehicle, ReminderType.SERVICE, "Kontrola układu hamulcowego", today.minusDays(42), 124_000, ReminderStatus.COMPLETED));
            reminders.add(reminder(vehicle, ReminderType.OTHER, "Kontrola klimatyzacji", today.minusDays(130), null, ReminderStatus.CANCELLED));
        }
        return reminders;
    }

    private Reminder reminder(
            Vehicle vehicle,
            ReminderType type,
            String title,
            LocalDate dueDate,
            Integer odometer,
            ReminderStatus finalStatus
    ) {
        Reminder reminder = new Reminder(vehicle, type, title, "Przypomnienie demonstracyjne", dueDate, odometer);
        if (finalStatus != null) {
            reminder.changeStatus(finalStatus);
        }
        return reminder;
    }

    private VehicleSpec vehicleSpec(
            String make,
            String model,
            int productionYear,
            FuelType fuelType,
            int currentOdometer,
            long purchasePrice
    ) {
        return new VehicleSpec(make, model, productionYear, fuelType, currentOdometer, purchasePrice);
    }

    private BigDecimal money(long value) {
        return BigDecimal.valueOf(value).setScale(2);
    }

    private enum Scenario { EMPTY, STANDARD, FULL }

    private record AccountSpec(String email, String password, String firstName, String lastName, Scenario scenario) { }

    private record VehicleSpec(
            String make,
            String model,
            int productionYear,
            FuelType fuelType,
            int currentOdometer,
            long purchasePrice
    ) { }
}
