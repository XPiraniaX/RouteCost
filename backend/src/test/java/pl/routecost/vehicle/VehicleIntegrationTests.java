package pl.routecost.vehicle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.user.JwtService;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;

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
class VehicleIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void createsAndListsVehiclesWithPaginationAndFuelFilter() throws Exception {
        String token = createUserToken();
        createVehicle(token, "Toyota", "Corolla", "PETROL", 120_000);
        createVehicle(token, "Skoda", "Octavia", "DIESEL", 90_000);

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", bearer(token))
                        .param("page", "0")
                        .param("size", "1")
                        .param("fuelType", "PETROL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].make").value("Toyota"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void preventsOdometerRollbackAndHidesAnotherUsersVehicle() throws Exception {
        String ownerToken = createUserToken();
        String vehicleId = createVehicle(ownerToken, "Toyota", "Corolla", "PETROL", 120_000);

        mockMvc.perform(put("/api/vehicles/{vehicleId}", vehicleId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequest("Toyota", "Corolla", "PETROL", 119_000)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Odometer cannot be lower than the last recorded vehicle mileage."));

        String otherUserToken = createUserToken();
        mockMvc.perform(get("/api/vehicles/{vehicleId}", vehicleId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesAndDeletesOwnedVehicle() throws Exception {
        String token = createUserToken();
        String vehicleId = createVehicle(token, "Toyota", "Corolla", "PETROL", 120_000);

        mockMvc.perform(put("/api/vehicles/{vehicleId}", vehicleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequest("Toyota", "Corolla", "PETROL", 125_000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOdometer").value(125_000));

        mockMvc.perform(delete("/api/vehicles/{vehicleId}", vehicleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vehicles/{vehicleId}", vehicleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    private String createUserToken() {
        User owner = userRepository.saveAndFlush(new User(
                "vehicle-" + UUID.randomUUID() + "@routecost.example",
                "test-hash",
                "Vehicle",
                "Owner"
        ));
        return jwtService.createAuthResponse(owner).accessToken();
    }

    private String createVehicle(String token, String make, String model, String fuelType, int odometer)
            throws Exception {
        String response = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleRequest(make, model, fuelType, odometer)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher matcher = ID_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new AssertionError("Vehicle response did not contain an id");
        }
        return matcher.group(1);
    }

    private String vehicleRequest(String make, String model, String fuelType, int odometer) {
        return """
                {"make":"%s","model":"%s","productionYear":2018,"fuelType":"%s",
                 "currentOdometer":%d,"purchaseDate":"2022-06-15","purchasePrice":12500.00}
                """.formatted(make, model, fuelType, odometer);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
