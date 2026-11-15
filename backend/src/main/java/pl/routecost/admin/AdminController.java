package pl.routecost.admin;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.routecost.user.UserRole;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public AdminUserPageResponse findUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return AdminUserPageResponse.from(adminService.findUsers(search, role, enabled, pageable));
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserResponse changeRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request
    ) {
        return adminService.changeRole(authenticatedUserId(jwt), userId, request.role());
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserResponse changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        return adminService.changeStatus(authenticatedUserId(jwt), userId, request.enabled());
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    private Long authenticatedUserId(Jwt jwt) {
        return ((Number) jwt.getClaim("userId")).longValue();
    }
}
