package pl.routecost.reminder;

import org.junit.jupiter.api.Test;
import pl.routecost.reminder.exception.InvalidReminderStatusTransitionException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReminderStatusTests {

    @Test
    void activeReminderCanBeCompletedOrCancelled() {
        Reminder completed = reminder();
        Reminder cancelled = reminder();

        completed.changeStatus(ReminderStatus.COMPLETED);
        cancelled.changeStatus(ReminderStatus.CANCELLED);

        assertEquals(ReminderStatus.COMPLETED, completed.getStatus());
        assertEquals(ReminderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void terminalReminderRequiresExplicitReactivationAndStartsNewActivationVersion() {
        Reminder reminder = reminder();
        reminder.changeStatus(ReminderStatus.COMPLETED);

        assertThrows(InvalidReminderStatusTransitionException.class,
                () -> reminder.changeStatus(ReminderStatus.ACTIVE));

        reminder.reactivate();

        assertEquals(ReminderStatus.ACTIVE, reminder.getStatus());
        assertEquals(2, reminder.getActivationVersion());
    }

    @Test
    void cannotReactivateAnAlreadyActiveReminderOrSwitchBetweenTerminalStates() {
        Reminder reminder = reminder();
        assertThrows(InvalidReminderStatusTransitionException.class, reminder::reactivate);

        reminder.changeStatus(ReminderStatus.CANCELLED);
        assertThrows(InvalidReminderStatusTransitionException.class,
                () -> reminder.changeStatus(ReminderStatus.COMPLETED));
    }

    private Reminder reminder() {
        return new Reminder(null, ReminderType.SERVICE, "Service", null, LocalDate.now().plusDays(10), null);
    }
}
