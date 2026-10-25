package pl.routecost.reminder;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ReminderNotificationPublisher {

    private final ReminderNotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public ReminderNotificationPublisher(
            ReminderNotificationRepository notificationRepository,
            RabbitTemplate rabbitTemplate,
            Clock reminderClock
    ) {
        this.notificationRepository = notificationRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = reminderClock;
    }

    @Transactional
    public int publishPending() {
        List<ReminderNotification> pending = notificationRepository.findByStatusForUpdate(
                ReminderNotificationStatus.PENDING,
                PageRequest.of(0, 100)
        );
        int published = 0;
        for (ReminderNotification notification : pending) {
            ReminderNotificationEvent event = new ReminderNotificationEvent(notification.getId());
            CorrelationData correlationData = new CorrelationData("reminder-notification-" + notification.getId());
            rabbitTemplate.convertAndSend(
                    "",
                    ReminderConfiguration.NOTIFICATION_QUEUE,
                    event.toQueuePayload(),
                    correlationData
            );
            try {
                CorrelationData.Confirm confirmation = correlationData.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirmation.isAck()) {
                    throw new IllegalStateException("RabbitMQ did not confirm a reminder notification message.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for RabbitMQ confirmation.", exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not publish reminder notification to RabbitMQ.", exception);
            }
            notification.markPublished(OffsetDateTime.now(clock));
            published++;
        }
        return published;
    }
}
