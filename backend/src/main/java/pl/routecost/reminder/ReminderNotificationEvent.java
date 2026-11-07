package pl.routecost.reminder;

public record ReminderNotificationEvent(Long notificationId) {

    public String toQueuePayload() {
        return notificationId.toString();
    }

    public static ReminderNotificationEvent fromQueuePayload(String payload) {
        return new ReminderNotificationEvent(Long.valueOf(payload));
    }
}
