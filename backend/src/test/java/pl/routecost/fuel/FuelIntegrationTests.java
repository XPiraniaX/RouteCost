package pl.routecost.fuel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.user.JwtService;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
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
class FuelIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");
    private final Map<String, Long> ownerIdsByToken = new HashMap<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void createsListsAndCalculatesFuelStatisticsWithPagination() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token), 10_400);

        createEntry(token, vehicleId, "2024-01-01", 10_000, "30.000", "150.00", true);
        createEntry(token, vehicleId, "2024-02-01", 10_200, "10.000", "50.00", false);
        String lastEntryId = createEntry(token, vehicleId, "2024-03-01", 10_400, "20.000", "100.00", true);

        mockMvc.perform(get("/api/vehicles/{vehicleId}/fuel-entries", vehicleId)
                        .header("Authorization", bearer(token))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, lastEntryId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumptionFromPreviousFullTank").value(7.5))
                .andExpect(jsonPath("$.pricePerLiter").value(5.0));

        mockMvc.perform(get("/api/vehicles/{vehicleId}/fuel-statistics", vehicleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageConsumptionLitersPer100Km").value(7.5))
                .andExpect(jsonPath("$.totalLiters").value(60))
                .andExpect(jsonPath("$.totalCost").value(300))
                .andExpect(jsonPath("$.validConsumptionIntervals").value(1));
    }

    @Test
    void rejectsRollbackWhenCreatingOrEditingAnEarlierEntry() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token), 11_000);
        String earlierEntryId = createEntry(token, vehicleId, "2024-01-01", 10_000, "30.000", "150.00", true);
        String laterEntryId = createEntry(token, vehicleId, "2024-03-01", 11_000, "25.000", "125.00", true);

        mockMvc.perform(post("/api/vehicles/{vehicleId}/fuel-entries", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-04-01", 10_500, "15.000", "75.00", false)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, earlierEntryId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-01-01", 12_000, "30.000", "150.00", true)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, laterEntryId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-03-01", 9_000, "25.000", "125.00", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesPositiveFuelAmountAndCost() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token), 10_000);

        mockMvc.perform(post("/api/vehicles/{vehicleId}/fuel-entries", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-01-01", 10_000, "0", "0.00", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));

        mockMvc.perform(post("/api/vehicles/{vehicleId}/fuel-entries", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2999-01-01", -1, "1.000", "5.00", true)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vehicles/{vehicleId}/fuel-entries", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2022-06-14", 10_000, "1.000", "5.00", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesFuelEntriesFromAnotherUserAndSupportsUpdateAndDelete() throws Exception {
        String ownerToken = createUserToken();
        Long vehicleId = createVehicle(ownerId(ownerToken), 10_000);
        String entryId = createEntry(ownerToken, vehicleId, "2024-01-01", 10_000, "30.000", "150.00", true);
        String otherUserToken = createUserToken();

        mockMvc.perform(get("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, entryId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, entryId)
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-01-01", 10_000, "31.000", "155.00", true)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, entryId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("2024-01-01", 10_000, "31.000", "155.00", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liters").value(31));

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, entryId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, entryId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAnEarlierFullTankRecalculatesConsumptionFromRemainingHistory() throws Exception {
        String token = createUserToken();
        Long vehicleId = createVehicle(ownerId(token), 10_400);
        String firstEntryId = createEntry(token, vehicleId, "2024-01-01", 10_000, "30.000", "150.00", true);
        createEntry(token, vehicleId, "2024-02-01", 10_400, "20.000", "100.00", true);

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/fuel-entries/{entryId}", vehicleId, firstEntryId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vehicles/{vehicleId}/fuel-statistics", vehicleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageConsumptionLitersPer100Km").doesNotExist())
                .andExpect(jsonPath("$.totalLiters").value(20))
                .andExpect(jsonPath("$.totalCost").value(100));
    }

    private Long createVehicle(Long ownerId, int odometer) {
        User owner = userRepository.findById(ownerId).orElseThrow();
        Vehicle vehicle = new Vehicle(
                owner,
                "Toyota",
                "Corolla",
                2018,
                FuelType.PETROL,
                odometer,
                LocalDate.of(2022, 6, 15),
                new BigDecimal("12500.00")
        );
        return vehicleRepository.saveAndFlush(vehicle).getId();
    }

    private String createEntry(
            String token,
            Long vehicleId,
            String date,
            int odometer,
            String liters,
            String cost,
            boolean fullTank
    ) throws Exception {
        String response = mockMvc.perform(post("/api/vehicles/{vehicleId}/fuel-entries", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(date, odometer, liters, cost, fullTank)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher matcher = ID_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new AssertionError("Fuel entry response did not contain an id");
        }
        return matcher.group(1);
    }

    private String createUserToken() {
        User owner = userRepository.saveAndFlush(new User(
                "fuel-" + UUID.randomUUID() + "@routecost.example",
                "test-hash",
                "Fuel",
                "Owner"
        ));
        String token = jwtService.createAuthResponse(owner).accessToken();
        ownerIdsByToken.put(token, owner.getId());
        return token;
    }

    private Long ownerId(String token) {
        return ownerIdsByToken.get(token);
    }

    private String request(String date, int odometer, String liters, String cost, boolean fullTank) {
        return """
                {"entryDate":"%s","odometer":%d,"liters":%s,"totalCost":%s,"fullTank":%s}
                """.formatted(date, odometer, liters, cost, fullTank);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
