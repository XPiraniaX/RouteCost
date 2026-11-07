package pl.routecost.reminder;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReminderSchedulerTests {

    @Test
    void scheduledOperationsDelegateScanningAndPublishing() {
        ReminderNotificationService notificationService = mock(ReminderNotificationService.class);
        ReminderNotificationPublisher publisher = mock(ReminderNotificationPublisher.class);
        ReminderScheduler scheduler = new ReminderScheduler(notificationService, publisher);

        scheduler.scanDueReminders();
        scheduler.publishPendingNotifications();

        verify(notificationService).scanDueReminders();
        verify(publisher).publishPending();
    }

    @Test
    void bothScheduledOperationsHaveConfigurableFixedDelays() throws NoSuchMethodException {
        Method scan = ReminderScheduler.class.getMethod("scanDueReminders");
        Method publish = ReminderScheduler.class.getMethod("publishPendingNotifications");

        Scheduled scanSchedule = scan.getAnnotation(Scheduled.class);
        Scheduled publishSchedule = publish.getAnnotation(Scheduled.class);

        assertNotNull(scanSchedule);
        assertNotNull(publishSchedule);
        assertEquals("${routecost.reminders.scan-delay-ms:60000}", scanSchedule.fixedDelayString());
        assertEquals("${routecost.reminders.publish-delay-ms:5000}", publishSchedule.fixedDelayString());
    }
}
