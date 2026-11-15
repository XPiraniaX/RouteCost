package pl.routecost.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.expense.Expense;
import pl.routecost.expense.ExpenseCategory;
import pl.routecost.expense.ExpenseRepository;
import pl.routecost.fuel.FuelEntry;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.reminder.Reminder;
import pl.routecost.reminder.ReminderRepository;
import pl.routecost.user.JwtService;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.user.UserRole;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class AdminIntegrationTests {

    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private FuelEntryRepository fuelEntryRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void userCannotUseAdminApiButAdminGetsPaginatedFilteredUsers() throws Exception {
        User administrator = saveUser(UserRole.ADMIN, true, "Admin", "Operator");
        String adminToken = token(administrator);
        User target = saveUser(UserRole.USER, true, "Filtered", "Person");
        saveUser(UserRole.USER, false, "Filtered", "Disabled");
        saveUser(UserRole.ADMIN, true, "Other", "Admin");
        String userToken = token(saveUser(UserRole.USER, true, "Regular", "User"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("search", "filtered")
                        .param("role", "USER")
                        .param("enabled", "true")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(target.getId()))
                .andExpect(jsonPath("$.content[0].enabled").value(true))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void pageMetadataAndSearchSupportMoreThanOnePage() throws Exception {
        User admin = saveUser(UserRole.ADMIN, true, "Page", "Admin");
        String search = "multi-page-" + UUID.randomUUID();
        saveUser(UserRole.USER, true, "Page", search + "-one@example.test");
        saveUser(UserRole.USER, true, "Page", search + "-two@example.test");
        saveUser(UserRole.USER, true, "Page", search + "-three@example.test");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(token(admin)))
                        .param("search", search)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void roleChangeTakesEffectForAnAlreadyIssuedAccessToken() throws Exception {
        User administrator = saveUser(UserRole.ADMIN, true, "Admin", "One");
        User secondAdministrator = saveUser(UserRole.ADMIN, true, "Admin", "Two");
        User target = saveUser(UserRole.USER, true, "Role", "Target");
        String targetOldToken = token(target);
        String adminToken = token(administrator);

        mockMvc.perform(patch("/api/admin/users/{id}/role", target.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(targetOldToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(targetOldToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(patch("/api/admin/users/{id}/role", target.getId())
                        .header("Authorization", bearer(token(secondAdministrator)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(targetOldToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(targetOldToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void deactivationBlocksExistingAccessAndRevokesEveryRefreshToken() throws Exception {
        User administrator = saveUser(UserRole.ADMIN, true, "Admin", "Operator");
        String email = "deactivate-" + UUID.randomUUID() + "@routecost.example";
        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Correct-Horse-Battery-12!",
                                 "firstName":"Disabled","lastName":"User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        User target = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        String targetToken = extractToken(registration.getResponse().getContentAsString());
        String firstRefresh = registration.getResponse().getCookie("routecost_refresh").getValue();

        var secondLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Correct-Horse-Battery-12!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        String secondRefresh = secondLogin.getResponse().getCookie("routecost_refresh").getValue();

        mockMvc.perform(patch("/api/admin/users/{id}/status", target.getId())
                        .header("Authorization", bearer(token(administrator)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(targetToken)))
                .andExpect(status().isUnauthorized());
        assertEquals(0L, jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where user_id = ? and revoked_at is null",
                Long.class, target.getId()
        ));
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", firstRefresh))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", secondRefresh))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preventsSelfChangesAndKeepsAtLeastOneActiveAdministrator() throws Exception {
        User onlyAdministrator = saveUser(UserRole.ADMIN, true, "Sole", "Admin");
        String adminToken = token(onlyAdministrator);

        mockMvc.perform(patch("/api/admin/users/{id}/status", onlyAdministrator.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/admin/users/{id}/role", onlyAdministrator.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void dashboardCountsEntitiesWithoutReturningPrivateRows() throws Exception {
        long usersBefore = userRepository.count();
        long activeBefore = userRepository.countByEnabled(true);
        long inactiveBefore = userRepository.countByEnabled(false);
        long adminsBefore = userRepository.countByRole(UserRole.ADMIN);
        long vehiclesBefore = vehicleRepository.count();
        long fuelBefore = fuelEntryRepository.count();
        long expensesBefore = expenseRepository.count();
        long remindersBefore = reminderRepository.count();

        User administrator = saveUser(UserRole.ADMIN, true, "Dashboard", "Admin");
        User owner = saveUser(UserRole.USER, false, "Private", "Owner");
        Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(
                owner, "Toyota", "Corolla", 2020, FuelType.PETROL, 20_000,
                LocalDate.of(2020, 1, 1), new BigDecimal("10000.00")
        ));
        fuelEntryRepository.saveAndFlush(new FuelEntry(
                vehicle, LocalDate.of(2021, 1, 1), 10_000,
                new BigDecimal("40.000"), new BigDecimal("250.00"), true
        ));
        expenseRepository.saveAndFlush(new Expense(
                vehicle, LocalDate.of(2021, 2, 1), ExpenseCategory.SERVICE,
                new BigDecimal("150.00"), "PLN", "Private description", 10_500
        ));
        reminderRepository.saveAndFlush(new Reminder(
                vehicle, pl.routecost.reminder.ReminderType.SERVICE,
                "Private reminder", "Private details", LocalDate.of(2022, 1, 1), 15_000
        ));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(token(administrator))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(usersBefore + 2))
                .andExpect(jsonPath("$.activeUsers").value(activeBefore + 1))
                .andExpect(jsonPath("$.inactiveUsers").value(inactiveBefore + 1))
                .andExpect(jsonPath("$.adminUsers").value(adminsBefore + 1))
                .andExpect(jsonPath("$.totalVehicles").value(vehiclesBefore + 1))
                .andExpect(jsonPath("$.totalFuelEntries").value(fuelBefore + 1))
                .andExpect(jsonPath("$.totalExpenses").value(expensesBefore + 1))
                .andExpect(jsonPath("$.totalReminders").value(remindersBefore + 1))
                .andExpect(jsonPath("$.privateDescription").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void adminCannotModifyAnotherUsersVehicle() throws Exception {
        User administrator = saveUser(UserRole.ADMIN, true, "Admin", "Operator");
        User owner = saveUser(UserRole.USER, true, "Vehicle", "Owner");
        Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(
                owner, "Toyota", "Corolla", 2020, FuelType.PETROL, 20_000,
                LocalDate.of(2020, 1, 1), new BigDecimal("10000.00")
        ));

        mockMvc.perform(put("/api/vehicles/{id}", vehicle.getId())
                        .header("Authorization", bearer(token(administrator)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"make":"Toyota","model":"Camry","productionYear":2020,
                                 "fuelType":"PETROL","currentOdometer":21000,
                                 "purchaseDate":"2020-01-01","purchasePrice":10000}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/vehicles/{id}", vehicle.getId())
                        .header("Authorization", bearer(token(administrator))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/vehicles/{id}/fuel-entries", vehicle.getId())
                        .header("Authorization", bearer(token(administrator))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/vehicles/{id}/expenses", vehicle.getId())
                        .header("Authorization", bearer(token(administrator))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/vehicles/{id}/reminders", vehicle.getId())
                        .header("Authorization", bearer(token(administrator))))
                .andExpect(status().isNotFound());
    }

    private User saveUser(UserRole role, boolean enabled, String firstName, String lastName) {
        User user = new User(uniqueEmail(), "test-hash", firstName, lastName, role);
        user.setEnabled(enabled);
        return userRepository.saveAndFlush(user);
    }

    private String token(User user) {
        return jwtService.createAuthResponse(user).accessToken();
    }

    private String uniqueEmail() {
        return "admin-test-" + UUID.randomUUID() + "@routecost.example";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String extractToken(String body) {
        Matcher matcher = ACCESS_TOKEN.matcher(body);
        if (!matcher.find()) throw new AssertionError("Auth response has no access token");
        return matcher.group(1);
    }
}
