package pl.routecost.reminder;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private final ReminderNotificationService notificationService;
    private final ReminderNotificationPublisher publisher;

    public ReminderScheduler(
            ReminderNotificationService notificationService,
            ReminderNotificationPublisher publisher
    ) {
        this.notificationService = notificationService;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${routecost.reminders.scan-delay-ms:60000}")
    public void scanDueReminders() {
        notificationService.scanDueReminders();
    }

    @Scheduled(fixedDelayString = "${routecost.reminders.publish-delay-ms:5000}")
    public void publishPendingNotifications() {
        publisher.publishPending();
    }
}
