package pl.routecost.notification;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    long countBySource_Id(Long sourceId);

    Page<UserNotification> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long countByUser_IdAndReadAtIsNull(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from UserNotification notification where notification.id = :id and notification.user.id = :userId")
    Optional<UserNotification> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("update UserNotification notification set notification.readAt = :readAt where notification.user.id = :userId and notification.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") OffsetDateTime readAt);
}
