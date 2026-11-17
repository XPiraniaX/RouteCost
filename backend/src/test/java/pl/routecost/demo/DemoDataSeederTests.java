package pl.routecost.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.routecost.expense.Expense;
import pl.routecost.expense.ExpenseRepository;
import pl.routecost.fuel.FuelEntry;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.reminder.Reminder;
import pl.routecost.reminder.ReminderRepository;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.user.UserRole;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDataSeederTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final FuelEntryRepository fuelEntryRepository = mock(FuelEntryRepository.class);
    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private final ReminderRepository reminderRepository = mock(ReminderRepository.class);
    private final PasswordEncoder passwordEncoder = spy(new BCryptPasswordEncoder(4));
    private final Map<String, User> storedUsers = new HashMap<>();

    @BeforeEach
    void configureRepositories() {
        when(userRepository.findByEmailIgnoreCase(any())).thenAnswer(invocation ->
                Optional.ofNullable(storedUsers.get(invocation.getArgument(0))));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            storedUsers.put(user.getEmail(), user);
            return user;
        });
        when(vehicleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fuelEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsThreeActiveUserAccountsWithBcryptAndScenarioDataAssignedToTheirVehicles() {
        seeder().run(args());

        assertEquals(3, storedUsers.size());
        assertUser("demo.empty@routecost.local", "RouteCostDemo1!");
        assertUser("demo.standard@routecost.local", "RouteCostDemo2!");
        assertUser("demo.full@routecost.local", "RouteCostDemo3!");

        List<Vehicle> vehicles = capturedVehicles();
        List<FuelEntry> fuelEntries = capturedFuelEntries();
        List<Expense> expenses = capturedExpenses();
        List<Reminder> reminders = capturedReminders();

        assertEquals(4, vehicles.size());
        assertEquals(24, fuelEntries.size());
        assertEquals(23, expenses.size());
        assertEquals(11, reminders.size());
        assertScenarioCounts("demo.empty@routecost.local", vehicles, fuelEntries, expenses, reminders,
                1, 0, 0, 0);
        assertScenarioCounts("demo.standard@routecost.local", vehicles, fuelEntries, expenses, reminders,
                1, 6, 5, 3);
        assertScenarioCounts("demo.full@routecost.local", vehicles, fuelEntries, expenses, reminders,
                2, 18, 18, 8);
        Vehicle fullPrimaryVehicle = vehicles.stream()
                .filter(vehicle -> vehicle.getOwner().getEmail().equals("demo.full@routecost.local"))
                .filter(vehicle -> vehicle.getMake().equals("Toyota"))
                .findFirst().orElseThrow();
        assertEquals(11, fuelEntries.stream().filter(entry -> entry.getVehicle() == fullPrimaryVehicle).count());
        assertEquals(11, expenses.stream().filter(expense -> expense.getVehicle() == fullPrimaryVehicle).count());

        assertTrue(fuelEntries.stream().allMatch(entry ->
                entry.getOdometer() <= entry.getVehicle().getCurrentOdometer()));
        assertTrue(expenses.stream().allMatch(expense ->
                expense.getOdometer() <= expense.getVehicle().getCurrentOdometer()));
        assertChronology(vehicles, fuelEntries, expenses);
        assertTrue(expenses.stream().map(Expense::getCategory).distinct().count() >= 7);
        assertTrue(reminders.stream().anyMatch(reminder ->
                reminder.getStatus().name().equals("ACTIVE") && reminder.getDueDate().isBefore(today())));
        assertTrue(reminders.stream().anyMatch(reminder ->
                reminder.getStatus().name().equals("COMPLETED")));
        assertTrue(reminders.stream().anyMatch(reminder ->
                reminder.getStatus().name().equals("CANCELLED")));

        for (FuelEntry entry : fuelEntries) {
            assertTrue(entry.getVehicle().getOwner() == storedUsers.get(entry.getVehicle().getOwner().getEmail()));
        }
        for (Expense expense : expenses) {
            assertTrue(expense.getVehicle().getOwner() == storedUsers.get(expense.getVehicle().getOwner().getEmail()));
        }
        for (Reminder reminder : reminders) {
            assertTrue(reminder.getVehicle().getOwner() == storedUsers.get(reminder.getVehicle().getOwner().getEmail()));
        }
    }

    @Test
    void repeatedRunsDoNotCreateDuplicateAccountsOrRelatedRows() {
        DemoDataSeeder seeder = seeder();
        seeder.run(args());
        seeder.run(args());

        assertEquals(3, storedUsers.size());
        verify(userRepository, times(3)).saveAndFlush(any(User.class));
        verify(vehicleRepository, times(3)).saveAll(any());
        verify(fuelEntryRepository, times(2)).saveAll(any());
        verify(expenseRepository, times(2)).saveAll(any());
        verify(reminderRepository, times(2)).saveAll(any());
    }

    @Test
    void skipsExistingAccountsWithoutChangingCredentialsOrAddingTheirData() {
        for (String email : List.of(
                "demo.empty@routecost.local",
                "demo.standard@routecost.local",
                "demo.full@routecost.local"
        )) {
            storedUsers.put(email, new User(email, "existing-password-hash", "Existing", "Account"));
        }

        seeder().run(args());

        assertEquals(3, storedUsers.size());
        assertTrue(storedUsers.values().stream().allMatch(user ->
                user.getFirstName().equals("Existing") && user.getPasswordHash().equals("existing-password-hash")));
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(vehicleRepository, never()).saveAll(any());
        verify(fuelEntryRepository, never()).saveAll(any());
        verify(expenseRepository, never()).saveAll(any());
        verify(reminderRepository, never()).saveAll(any());
    }

    @Test
    void onlyRegistersSeederInTheDevelopmentProfile() {
        Profile profile = DemoDataSeeder.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertArrayEquals(new String[]{"dev & !prod"}, profile.value());
    }

    private void assertUser(String email, String rawPassword) {
        User user = storedUsers.get(email);
        assertNotNull(user);
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.isEnabled());
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
        assertFalse(user.getPasswordHash().equals(rawPassword));
    }

    private void assertScenarioCounts(
            String email,
            List<Vehicle> vehicles,
            List<FuelEntry> fuelEntries,
            List<Expense> expenses,
            List<Reminder> reminders,
            int vehicleCount,
            int fuelCount,
            int expenseCount,
            int reminderCount
    ) {
        assertEquals(vehicleCount, vehicles.stream().filter(v -> v.getOwner().getEmail().equals(email)).count());
        assertEquals(fuelCount, fuelEntries.stream().filter(e -> ownerEmail(e.getVehicle()).equals(email)).count());
        assertEquals(expenseCount, expenses.stream().filter(e -> ownerEmail(e.getVehicle()).equals(email)).count());
        assertEquals(reminderCount, reminders.stream().filter(r -> ownerEmail(r.getVehicle()).equals(email)).count());
    }

    private void assertChronology(List<Vehicle> vehicles, List<FuelEntry> fuelEntries, List<Expense> expenses) {
        for (Vehicle vehicle : vehicles) {
            List<FuelEntry> entries = fuelEntries.stream()
                    .filter(entry -> entry.getVehicle() == vehicle)
                    .sorted(java.util.Comparator.comparing(FuelEntry::getEntryDate))
                    .toList();
            int previousFuelOdometer = -1;
            TreeMap<LocalDate, int[]> dailyOdometers = new TreeMap<>();
            for (FuelEntry entry : entries) {
                assertFalse(entry.getEntryDate().isBefore(vehicle.getPurchaseDate()));
                assertTrue(entry.getOdometer() >= previousFuelOdometer);
                previousFuelOdometer = entry.getOdometer();
                includeOdometer(dailyOdometers, entry.getEntryDate(), entry.getOdometer());
            }
            for (Expense expense : expenses) {
                if (expense.getVehicle() == vehicle && expense.getOdometer() != null) {
                    assertFalse(expense.getExpenseDate().isBefore(vehicle.getPurchaseDate()));
                    includeOdometer(dailyOdometers, expense.getExpenseDate(), expense.getOdometer());
                }
            }

            int previousDayMaximum = -1;
            for (int[] dailyRange : dailyOdometers.values()) {
                assertTrue(dailyRange[0] >= previousDayMaximum);
                previousDayMaximum = dailyRange[1];
            }
        }
    }

    private void includeOdometer(TreeMap<LocalDate, int[]> dailyOdometers, LocalDate date, int odometer) {
        dailyOdometers.merge(date, new int[]{odometer, odometer}, (range, next) -> {
            range[0] = Math.min(range[0], next[0]);
            range[1] = Math.max(range[1], next[1]);
            return range;
        });
    }

    private List<Vehicle> capturedVehicles() {
        org.mockito.ArgumentCaptor<Iterable<Vehicle>> captor = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(vehicleRepository, atLeastOnce()).saveAll(captor.capture());
        return flatten(captor.getAllValues());
    }

    private List<FuelEntry> capturedFuelEntries() {
        org.mockito.ArgumentCaptor<Iterable<FuelEntry>> captor = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(fuelEntryRepository, atLeastOnce()).saveAll(captor.capture());
        return flatten(captor.getAllValues());
    }

    private List<Expense> capturedExpenses() {
        org.mockito.ArgumentCaptor<Iterable<Expense>> captor = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(expenseRepository, atLeastOnce()).saveAll(captor.capture());
        return flatten(captor.getAllValues());
    }

    private List<Reminder> capturedReminders() {
        org.mockito.ArgumentCaptor<Iterable<Reminder>> captor = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(reminderRepository, atLeastOnce()).saveAll(captor.capture());
        return flatten(captor.getAllValues());
    }

    private <T> List<T> flatten(List<Iterable<T>> batches) {
        return batches.stream().flatMap(batch -> {
            List<T> values = new ArrayList<>();
            batch.forEach(values::add);
            return values.stream();
        }).toList();
    }

    private String ownerEmail(Vehicle vehicle) {
        return vehicle.getOwner().getEmail();
    }

    private LocalDate today() {
        return LocalDate.now(clock());
    }

    private DemoDataSeeder seeder() {
        return new DemoDataSeeder(
                userRepository,
                vehicleRepository,
                fuelEntryRepository,
                expenseRepository,
                reminderRepository,
                passwordEncoder,
                clock()
        );
    }

    private Clock clock() {
        return Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneId.of("Europe/Warsaw"));
    }

    private ApplicationArguments args() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
