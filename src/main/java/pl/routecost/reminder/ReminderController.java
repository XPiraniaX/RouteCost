package pl.routecost.reminder;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.routecost.reminder.dto.ReminderPageResponse;
import pl.routecost.reminder.dto.ReminderRequest;
import pl.routecost.reminder.dto.ReminderResponse;
import pl.routecost.reminder.dto.ReminderStatusRequest;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public ReminderPageResponse findReminders(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) LocalDate dueFrom,
            @RequestParam(required = false) LocalDate dueTo,
            @PageableDefault(size = 20, sort = {"dueDate", "id"}, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return reminderService.findReminders(userId(jwt), vehicleId, status, dueFrom, dueTo, pageable);
    }

    @GetMapping("/{reminderId}")
    public ReminderResponse findReminder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long reminderId
    ) {
        return reminderService.findReminder(userId(jwt), vehicleId, reminderId);
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> createReminder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @Valid @RequestBody ReminderRequest request
    ) {
        ReminderResponse response = reminderService.createReminder(userId(jwt), vehicleId, request);
        URI location = URI.create("/api/vehicles/" + vehicleId + "/reminders/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{reminderId}")
    public ReminderResponse updateReminder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long reminderId,
            @Valid @RequestBody ReminderRequest request
    ) {
        return reminderService.updateReminder(userId(jwt), vehicleId, reminderId, request);
    }

    @PatchMapping("/{reminderId}/status")
    public ReminderResponse changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long reminderId,
            @Valid @RequestBody ReminderStatusRequest request
    ) {
        return reminderService.changeStatus(userId(jwt), vehicleId, reminderId, request.status());
    }

    @PostMapping("/{reminderId}/reactivate")
    public ReminderResponse reactivateReminder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long reminderId
    ) {
        return reminderService.reactivateReminder(userId(jwt), vehicleId, reminderId);
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long reminderId
    ) {
        reminderService.deleteReminder(userId(jwt), vehicleId, reminderId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        return userId.longValue();
    }
}
