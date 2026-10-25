package pl.routecost.reminder;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReminderNotificationConsumer {

    private final ReminderNotificationProcessor processor;

    public ReminderNotificationConsumer(ReminderNotificationProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = ReminderConfiguration.NOTIFICATION_QUEUE)
    public void consume(String payload) {
        processor.process(ReminderNotificationEvent.fromQueuePayload(payload));
    }
}
