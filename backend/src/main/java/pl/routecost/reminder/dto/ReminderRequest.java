package pl.routecost.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pl.routecost.reminder.ReminderType;

import java.time.LocalDate;

public record ReminderRequest(
        @NotNull ReminderType type,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull LocalDate dueDate,
        @PositiveOrZero Integer odometer
) {
}
