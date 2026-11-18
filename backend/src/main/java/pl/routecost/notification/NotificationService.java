package pl.routecost.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class NotificationService {
    private final UserNotificationRepository repository;
    private final Clock clock;

    public NotificationService(UserNotificationRepository repository, Clock reminderClock) {
        this.repository = repository;
        this.clock = reminderClock;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse findAll(Long userId, int page, int size) {
        return NotificationPageResponse.from(repository.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUser_IdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        UserNotification notification = repository.findByIdAndUserIdForUpdate(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(OffsetDateTime.now(clock));
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, OffsetDateTime.now(clock));
    }
}
