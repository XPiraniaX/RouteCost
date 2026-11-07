package pl.routecost.reminder;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableRabbit
@EnableScheduling
public class ReminderConfiguration {

    public static final String NOTIFICATION_QUEUE = "routecost.reminder.notifications";

    @Bean
    Queue reminderNotificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    Clock reminderClock() {
        return Clock.systemDefaultZone();
    }
}
