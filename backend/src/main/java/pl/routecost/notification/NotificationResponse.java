package pl.routecost.notification;

import java.time.OffsetDateTime;

public record NotificationResponse(Long id, String title, String message, Long vehicleId, String vehicleName,
                                   Long reminderId, OffsetDateTime createdAt, boolean read, OffsetDateTime readAt) {
    public static NotificationResponse from(UserNotification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getVehicle().getId(), notification.getVehicle().getMake() + " " + notification.getVehicle().getModel(),
                notification.getReminder().getId(), notification.getCreatedAt(), notification.getReadAt() != null,
                notification.getReadAt());
    }
}
