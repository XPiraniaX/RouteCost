package pl.routecost.reminder.dto;

import pl.routecost.reminder.Reminder;
import pl.routecost.reminder.ReminderStatus;
import pl.routecost.reminder.ReminderType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ReminderResponse(
        Long id,
        Long vehicleId,
        ReminderType type,
        String title,
        String description,
        LocalDate dueDate,
        ReminderStatus status,
        Integer odometer,
        int activationVersion,
        OffsetDateTime createdAt
) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getVehicle().getId(),
                reminder.getType(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getDueDate(),
                reminder.getStatus(),
                reminder.getOdometer(),
                reminder.getActivationVersion(),
                reminder.getCreatedAt()
        );
    }
}
