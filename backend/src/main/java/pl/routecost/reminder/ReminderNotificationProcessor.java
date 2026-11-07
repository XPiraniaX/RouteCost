package pl.routecost.reminder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class ReminderNotificationProcessor {

    private final ReminderNotificationRepository notificationRepository;
    private final Clock clock;
    private final int upcomingDays;

    public ReminderNotificationProcessor(
            ReminderNotificationRepository notificationRepository,
            Clock reminderClock,
            @Value("${routecost.reminders.upcoming-days:7}") int upcomingDays
    ) {
        this.notificationRepository = notificationRepository;
        this.clock = reminderClock;
        this.upcomingDays = upcomingDays;
    }

    @Transactional
    public void process(ReminderNotificationEvent event) {
        ReminderNotification notification = notificationRepository.findByIdForUpdate(event.notificationId())
                .orElse(null);
        if (notification == null || notification.getStatus() != ReminderNotificationStatus.PUBLISHED) {
            return;
        }

        Reminder reminder = notification.getReminder();
        LocalDate today = LocalDate.now(clock);
        boolean currentAndRelevant = reminder.getStatus() == ReminderStatus.ACTIVE
                && reminder.getActivationVersion() == notification.getActivationVersion()
                && reminder.getDueDate().equals(notification.getDueDate())
                && isRelevant(notification, today);
        OffsetDateTime processedAt = OffsetDateTime.now(clock);
        if (currentAndRelevant) {
            notification.markProcessed(processedAt);
        } else {
            notification.markDiscarded(processedAt);
        }
    }

    private boolean isRelevant(ReminderNotification notification, LocalDate today) {
        if (notification.getKind() == ReminderNotificationKind.UPCOMING) {
            return !notification.getDueDate().isBefore(today)
                    && !notification.getDueDate().isAfter(today.plusDays(upcomingDays));
        }
        return notification.getDueDate().isBefore(today);
    }
}
