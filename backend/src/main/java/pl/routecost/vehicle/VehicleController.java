package pl.routecost.vehicle;

import jakarta.validation.Valid;
import pl.routecost.vehicle.dto.VehiclePageResponse;
import pl.routecost.vehicle.dto.VehicleRequest;
import pl.routecost.vehicle.dto.VehicleResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public VehiclePageResponse findVehicles(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) FuelType fuelType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return vehicleService.findVehicles(userId(jwt), fuelType, pageable);
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse findVehicle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long vehicleId) {
        return vehicleService.findVehicle(userId(jwt), vehicleId);
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VehicleRequest request
    ) {
        VehicleResponse response = vehicleService.createVehicle(userId(jwt), request);
        return ResponseEntity.created(URI.create("/api/vehicles/" + response.id())).body(response);
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleRequest request
    ) {
        return vehicleService.updateVehicle(userId(jwt), vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId
    ) {
        vehicleService.deleteVehicle(userId(jwt), vehicleId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        return userId.longValue();
    }
}
