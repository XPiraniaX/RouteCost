package pl.routecost.expense;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.fuel.FuelEntry;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.user.JwtService;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class ExpenseIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");
    private final Map<String, Long> ownerIdsByToken = new HashMap<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void createsFiltersAndSummarizesExpensesSeparatelyFromFuel() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token));
        createExpense(token, vehicleId, "2024-01-15", "REPAIR", "100.00", "PLN", null);
        createExpense(token, vehicleId, "2024-02-02", "PARTS", "50.00", "PLN", null);
        createExpense(token, vehicleId, "2024-02-03", "INSURANCE", "20.00", "EUR", null);
        fuelEntryRepository.saveAndFlush(new FuelEntry(
                vehicleRepository.findById(vehicleId).orElseThrow(),
                LocalDate.of(2024, 2, 5),
                11_000,
                new BigDecimal("40.000"),
                new BigDecimal("200.00"),
                true
        ));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .param("category", "PARTS")
                        .param("dateFrom", "2024-02-01")
                        .param("dateTo", "2024-02-29")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].category").value("PARTS"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/expense-statistics", vehicleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseCount").value(3))
                .andExpect(jsonPath("$.totalsByCurrency.length()").value(2))
                .andExpect(jsonPath("$.totalsByCurrency[0].currency").value("EUR"))
                .andExpect(jsonPath("$.totalsByCurrency[0].totalAmount").value(20))
                .andExpect(jsonPath("$.totalsByCurrency[1].currency").value("PLN"))
                .andExpect(jsonPath("$.totalsByCurrency[1].totalAmount").value(150))
                .andExpect(jsonPath("$.totalsByCategory.length()").value(3))
                .andExpect(jsonPath("$.monthlyTotals.length()").value(3));
    }

    @Test
    void validatesAmountDateCurrencyAndMileageChronology() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token));
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();
        fuelEntryRepository.saveAndFlush(new FuelEntry(
                vehicle,
                LocalDate.of(2024, 1, 10),
                10_000,
                new BigDecimal("30.000"),
                new BigDecimal("150.00"),
                true
        ));
        fuelEntryRepository.saveAndFlush(new FuelEntry(
                vehicle,
                LocalDate.of(2024, 3, 10),
                12_000,
                new BigDecimal("30.000"),
                new BigDecimal("150.00"),
                true
        ));

        mockMvc.perform(post("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-02-01", "REPAIR", "0.00", "PLN", null, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-02-01", "REPAIR", "25.00", "pln", null, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2999-02-01", "REPAIR", "25.00", "PLN", null, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-02-01", "REPAIR", "25.00", "PLN", 9_000, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-02-01", "REPAIR", "25.00", "PLN", 11_000, null)))
                .andExpect(status().isCreated());
    }

    @Test
    void restrictsOtherUsersAndSupportsExpenseCrud() throws Exception {
        String ownerToken = createUserToken();
        Long vehicleId = createVehicle(ownerId(ownerToken));
        String expenseId = createExpense(
                ownerToken, vehicleId, "2024-02-01", "SERVICE", "125.00", "PLN", 11_000
        );
        String otherUserToken = createUserToken();

        mockMvc.perform(get("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/vehicles/{vehicleId}/expenses/{expenseId}", vehicleId, expenseId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/expenses/{expenseId}", vehicleId, expenseId)
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-02-01", "SERVICE", "150.00", "PLN", 11_000, null)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/expenses/{expenseId}", vehicleId, expenseId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-02-01", "SERVICE", "150.00", "PLN", 11_000, "Changed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.description").value("Changed"));

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/expenses/{expenseId}", vehicleId, expenseId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/expenses/{expenseId}", vehicleId, expenseId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vehicles/{vehicleId}/expenses/{expenseId}", vehicleId, expenseId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    private Long createVehicle(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElseThrow();
        Vehicle vehicle = new Vehicle(
                owner,
                "Toyota",
                "Corolla",
                2018,
                FuelType.PETROL,
                12_000,
                LocalDate.of(2022, 6, 15),
                new BigDecimal("12500.00")
        );
        return vehicleRepository.saveAndFlush(vehicle).getId();
    }

    private String createExpense(
            String token,
            Long vehicleId,
            String date,
            String category,
            String amount,
            String currency,
            Integer odometer
    ) throws Exception {
        String response = mockMvc.perform(post("/api/vehicles/{vehicleId}/expenses", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(date, category, amount, currency, odometer, null)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher matcher = ID_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new AssertionError("Expense response did not contain an id");
        }
        return matcher.group(1);
    }

    private String createUserToken() {
        User owner = userRepository.saveAndFlush(new User(
                "expense-" + UUID.randomUUID() + "@routecost.example",
                "test-hash",
                "Expense",
                "Owner"
        ));
        String token = jwtService.createAuthResponse(owner).accessToken();
        ownerIdsByToken.put(token, owner.getId());
        return token;
    }

    private Long ownerId(String token) {
        return ownerIdsByToken.get(token);
    }

    private String request(
            String date,
            String category,
            String amount,
            String currency,
            Integer odometer,
            String description
    ) {
        String odometerJson = odometer == null ? "null" : odometer.toString();
        String descriptionJson = description == null ? "null" : "\"" + description + "\"";
        return """
                {"expenseDate":"%s","category":"%s","amount":%s,"currency":"%s",
                 "description":%s,"odometer":%s}
                """.formatted(date, category, amount, currency, descriptionJson, odometerJson);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
