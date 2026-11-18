package pl.routecost.reminder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.notification.UserNotification;
import pl.routecost.notification.UserNotificationRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class ReminderNotificationProcessor {

    private final ReminderNotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final Clock clock;
    private final int upcomingDays;

    public ReminderNotificationProcessor(
            ReminderNotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            Clock reminderClock,
            @Value("${routecost.reminders.upcoming-days:7}") int upcomingDays
    ) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
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
            String message = notification.getKind() == ReminderNotificationKind.OVERDUE
                    ? "Termin przypomnienia minął " + notification.getDueDate() + "."
                    : "Zbliża się termin: " + notification.getDueDate() + ".";
            userNotificationRepository.save(new UserNotification(
                    notification,
                    reminder.getVehicle().getOwner(),
                    reminder.getVehicle(),
                    reminder,
                    reminder.getTitle(),
                    message,
                    processedAt
            ));
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
