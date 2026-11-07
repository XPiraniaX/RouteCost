package pl.routecost.reminder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReminderNotificationService {

    private final ReminderRepository reminderRepository;
    private final ReminderNotificationRepository notificationRepository;
    private final Clock clock;
    private final int upcomingDays;

    public ReminderNotificationService(
            ReminderRepository reminderRepository,
            ReminderNotificationRepository notificationRepository,
            Clock reminderClock,
            @Value("${routecost.reminders.upcoming-days:7}") int upcomingDays
    ) {
        this.reminderRepository = reminderRepository;
        this.notificationRepository = notificationRepository;
        this.clock = reminderClock;
        this.upcomingDays = upcomingDays;
    }

    @Transactional
    public int scanDueReminders() {
        LocalDate today = LocalDate.now(clock);
        int created = 0;
        List<Reminder> upcoming = reminderRepository.findActiveBetweenDatesForUpdate(
                ReminderStatus.ACTIVE,
                today,
                today.plusDays(upcomingDays)
        );
        for (Reminder reminder : upcoming) {
            created += createIfMissing(reminder, ReminderNotificationKind.UPCOMING);
        }

        List<Reminder> overdue = reminderRepository.findActiveBeforeDateForUpdate(ReminderStatus.ACTIVE, today);
        for (Reminder reminder : overdue) {
            created += createIfMissing(reminder, ReminderNotificationKind.OVERDUE);
        }
        return created;
    }

    private int createIfMissing(Reminder reminder, ReminderNotificationKind kind) {
        boolean exists = notificationRepository.existsByReminder_IdAndActivationVersionAndKindAndDueDate(
                reminder.getId(),
                reminder.getActivationVersion(),
                kind,
                reminder.getDueDate()
        );
        if (exists) {
            return 0;
        }
        notificationRepository.save(new ReminderNotification(reminder, kind, reminder.getDueDate()));
        return 1;
    }
}
