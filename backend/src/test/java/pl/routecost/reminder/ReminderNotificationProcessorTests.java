package pl.routecost.reminder;

import org.junit.jupiter.api.Test;
import pl.routecost.notification.UserNotificationRepository;
import pl.routecost.user.User;
import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReminderNotificationProcessorTests {

    @Test
    void doesNotMarkEventProcessedWhenSavingUserNotificationFails() {
        ReminderNotificationRepository events = mock(ReminderNotificationRepository.class);
        UserNotificationRepository notifications = mock(UserNotificationRepository.class);
        Reminder reminder = new Reminder(
                new Vehicle(new User("owner@example.test", "hash", "Owner", "Test"), "Toyota", "Corolla", 2020,
                        FuelType.PETROL, 10_000, LocalDate.now().minusYears(1), new BigDecimal("1000.00")),
                ReminderType.SERVICE, "Service", null, LocalDate.now().plusDays(1), null);
        ReminderNotification event = new ReminderNotification(reminder, ReminderNotificationKind.UPCOMING, reminder.getDueDate());
        event.markPublished(java.time.OffsetDateTime.now());
        when(events.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(event));
        when(notifications.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        ReminderNotificationProcessor processor = new ReminderNotificationProcessor(events, notifications,
                Clock.systemDefaultZone(), 7);

        assertThrows(IllegalStateException.class, () -> processor.process(new ReminderNotificationEvent(1L)));
        assertEquals(ReminderNotificationStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void discardsInactiveEventWithoutCreatingUserNotification() {
        ReminderNotificationRepository events = mock(ReminderNotificationRepository.class);
        UserNotificationRepository notifications = mock(UserNotificationRepository.class);
        Reminder reminder = new Reminder(
                new Vehicle(new User("owner@example.test", "hash", "Owner", "Test"), "Toyota", "Corolla", 2020,
                        FuelType.PETROL, 10_000, LocalDate.now().minusYears(1), new BigDecimal("1000.00")),
                ReminderType.SERVICE, "Service", null, LocalDate.now().plusDays(1), null);
        reminder.changeStatus(ReminderStatus.CANCELLED);
        ReminderNotification event = new ReminderNotification(reminder, ReminderNotificationKind.UPCOMING, reminder.getDueDate());
        event.markPublished(java.time.OffsetDateTime.now());
        when(events.findByIdForUpdate(2L)).thenReturn(java.util.Optional.of(event));
        ReminderNotificationProcessor processor = new ReminderNotificationProcessor(events, notifications,
                Clock.systemDefaultZone(), 7);

        processor.process(new ReminderNotificationEvent(2L));

        assertEquals(ReminderNotificationStatus.DISCARDED, event.getStatus());
        verifyNoInteractions(notifications);
    }
}
