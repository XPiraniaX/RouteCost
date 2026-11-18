package pl.routecost.reminder;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ReminderNotificationConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final ReminderNotificationProcessor processor;
    private final RabbitTemplate rabbitTemplate;

    public ReminderNotificationConsumer(ReminderNotificationProcessor processor, RabbitTemplate rabbitTemplate) {
        this.processor = processor;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = ReminderConfiguration.NOTIFICATION_QUEUE)
    public void consume(String payload) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                processor.process(ReminderNotificationEvent.fromQueuePayload(payload));
                return;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (attempt == MAX_ATTEMPTS) break;
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AmqpRejectAndDontRequeueException("Reminder notification processing was interrupted.", interrupted);
                }
            }
        }
        throw new AmqpRejectAndDontRequeueException("Reminder notification processing failed after bounded retries.", lastFailure);
    }

    @RabbitListener(queues = ReminderConfiguration.LEGACY_NOTIFICATION_QUEUE)
    public void migrateLegacy(String payload) {
        Exception failure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                publishConfirmed(ReminderConfiguration.NOTIFICATION_QUEUE, payload,
                        "legacy-reminder-notification-" + payload);
                return;
            } catch (AmqpRejectAndDontRequeueException exception) {
                throw exception;
            } catch (Exception exception) {
                failure = exception;
                if (attempt < MAX_ATTEMPTS) pause(attempt);
            }
        }
        try {
            publishConfirmed(ReminderConfiguration.NOTIFICATION_DEAD_LETTER_QUEUE, payload,
                    "legacy-reminder-notification-dlq-" + payload);
        } catch (Exception deadLetterFailure) {
            throw new AmqpRejectAndDontRequeueException("Could not transfer legacy notification after bounded retries.", deadLetterFailure);
        }
    }

    private void publishConfirmed(String queue, String payload, String correlationId) throws Exception {
        CorrelationData confirmation = new CorrelationData(correlationId);
        rabbitTemplate.convertAndSend("", queue, payload, confirmation);
        CorrelationData.Confirm result;
        try {
            result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AmqpRejectAndDontRequeueException("Notification transfer was interrupted.", interrupted);
        }
        if (!result.isAck()) throw new IllegalStateException("RabbitMQ did not confirm the notification transfer.");
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(250L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AmqpRejectAndDontRequeueException("Legacy notification migration was interrupted.", interrupted);
        }
    }
}
