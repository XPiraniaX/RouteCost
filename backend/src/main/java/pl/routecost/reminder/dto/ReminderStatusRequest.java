package pl.routecost.reminder.dto;

import jakarta.validation.constraints.NotNull;
import pl.routecost.reminder.ReminderStatus;

public record ReminderStatusRequest(@NotNull ReminderStatus status) {
}
