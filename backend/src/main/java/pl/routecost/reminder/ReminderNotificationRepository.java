package pl.routecost.reminder;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderNotificationRepository extends JpaRepository<ReminderNotification, Long> {

    boolean existsByReminder_IdAndActivationVersionAndKindAndDueDate(
            Long reminderId,
            int activationVersion,
            ReminderNotificationKind kind,
            LocalDate dueDate
    );

    long countByReminder_Id(Long reminderId);

    List<ReminderNotification> findAllByReminder_IdOrderByCreatedAtAsc(Long reminderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select notification from ReminderNotification notification
            where notification.status = :status
            order by notification.createdAt, notification.id
            """)
    List<ReminderNotification> findByStatusForUpdate(
            @Param("status") ReminderNotificationStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from ReminderNotification notification where notification.id = :id")
    Optional<ReminderNotification> findByIdForUpdate(@Param("id") Long id);
}
