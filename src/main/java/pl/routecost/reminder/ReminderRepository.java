package pl.routecost.reminder;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long>, JpaSpecificationExecutor<Reminder> {

    Page<Reminder> findAll(Specification<Reminder> specification, Pageable pageable);

    Optional<Reminder> findByIdAndVehicle_Id(Long id, Long vehicleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reminder from Reminder reminder
            where reminder.id = :reminderId
              and reminder.vehicle.id = :vehicleId
              and reminder.vehicle.owner.id = :ownerId
            """)
    Optional<Reminder> findOwnedReminderForUpdate(
            @Param("reminderId") Long reminderId,
            @Param("vehicleId") Long vehicleId,
            @Param("ownerId") Long ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reminder from Reminder reminder
            where reminder.status = :status
              and reminder.dueDate between :dateFrom and :dateTo
            order by reminder.id
            """)
    List<Reminder> findActiveBetweenDatesForUpdate(
            @Param("status") ReminderStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reminder from Reminder reminder
            where reminder.status = :status
              and reminder.dueDate < :dateBefore
            order by reminder.id
            """)
    List<Reminder> findActiveBeforeDateForUpdate(
            @Param("status") ReminderStatus status,
            @Param("dateBefore") LocalDate dateBefore
    );
}
