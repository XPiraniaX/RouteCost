package pl.routecost.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.routecost.dashboard.dto.DashboardSummaryResponse;
import pl.routecost.dashboard.dto.VehicleCostSummaryResponse;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse findSummary(@AuthenticationPrincipal Jwt jwt) {
        return dashboardService.findSummary(userId(jwt));
    }

    @GetMapping("/vehicles/{vehicleId}/cost-summary")
    public VehicleCostSummaryResponse findVehicleCostSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId
    ) {
        return dashboardService.findVehicleCostSummary(userId(jwt), vehicleId);
    }

    private Long userId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        return userId.longValue();
    }
}
