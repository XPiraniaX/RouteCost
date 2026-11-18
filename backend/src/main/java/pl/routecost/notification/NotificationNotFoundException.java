package pl.routecost.notification;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() { super("Notification was not found."); }
}
