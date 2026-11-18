package pl.routecost.reminder;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

import java.time.Clock;

@Configuration
@EnableRabbit
@EnableScheduling
public class ReminderConfiguration {

    public static final String LEGACY_NOTIFICATION_QUEUE = "routecost.reminder.notifications";
    public static final String NOTIFICATION_QUEUE = LEGACY_NOTIFICATION_QUEUE + ".v2";
    public static final String NOTIFICATION_DEAD_LETTER_QUEUE = NOTIFICATION_QUEUE + ".dlq";

    @Bean
    Queue reminderNotificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true, false, false, Map.of(
                "x-dead-letter-exchange", "",
                "x-dead-letter-routing-key", NOTIFICATION_DEAD_LETTER_QUEUE
        ));
    }

    @Bean
    Queue reminderNotificationDeadLetterQueue() {
        return new Queue(NOTIFICATION_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    Queue legacyReminderNotificationQueue() {
        return new Queue(LEGACY_NOTIFICATION_QUEUE, true);
    }

    @Bean
    Clock reminderClock() {
        return Clock.systemDefaultZone();
    }
}
