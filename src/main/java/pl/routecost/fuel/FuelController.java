package pl.routecost.fuel;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.routecost.fuel.dto.FuelEntryPageResponse;
import pl.routecost.fuel.dto.FuelEntryRequest;
import pl.routecost.fuel.dto.FuelEntryResponse;
import pl.routecost.fuel.dto.FuelStatisticsResponse;

import java.net.URI;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}")
public class FuelController {

    private final FuelService fuelService;

    public FuelController(FuelService fuelService) {
        this.fuelService = fuelService;
    }

    @GetMapping("/fuel-entries")
    public FuelEntryPageResponse findFuelEntries(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PageableDefault(size = 20, sort = {"entryDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return fuelService.findFuelEntries(userId(jwt), vehicleId, pageable);
    }

    @GetMapping("/fuel-entries/{entryId}")
    public FuelEntryResponse findFuelEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long entryId
    ) {
        return fuelService.findFuelEntry(userId(jwt), vehicleId, entryId);
    }

    @PostMapping("/fuel-entries")
    public ResponseEntity<FuelEntryResponse> createFuelEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @Valid @RequestBody FuelEntryRequest request
    ) {
        FuelEntryResponse response = fuelService.createFuelEntry(userId(jwt), vehicleId, request);
        URI location = URI.create("/api/vehicles/" + vehicleId + "/fuel-entries/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/fuel-entries/{entryId}")
    public FuelEntryResponse updateFuelEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long entryId,
            @Valid @RequestBody FuelEntryRequest request
    ) {
        return fuelService.updateFuelEntry(userId(jwt), vehicleId, entryId, request);
    }

    @DeleteMapping("/fuel-entries/{entryId}")
    public ResponseEntity<Void> deleteFuelEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long entryId
    ) {
        fuelService.deleteFuelEntry(userId(jwt), vehicleId, entryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fuel-statistics")
    public FuelStatisticsResponse findFuelStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId
    ) {
        return fuelService.findFuelStatistics(userId(jwt), vehicleId);
    }

    private Long userId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        return userId.longValue();
    }
}
