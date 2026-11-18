package pl.routecost.notification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public NotificationPageResponse findAll(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.findAll(userId(jwt), page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCountResponse(service.unreadCount(userId(jwt)));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
        return service.markRead(userId(jwt), notificationId);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) { return ((Number) jwt.getClaim("userId")).longValue(); }

    public record UnreadCountResponse(long count) { }
}
