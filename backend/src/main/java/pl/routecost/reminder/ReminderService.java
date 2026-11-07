package pl.routecost.reminder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.reminder.dto.ReminderPageResponse;
import pl.routecost.reminder.dto.ReminderRequest;
import pl.routecost.reminder.dto.ReminderResponse;
import pl.routecost.reminder.exception.ReminderChronologyException;
import pl.routecost.reminder.exception.ReminderDateRangeException;
import pl.routecost.reminder.exception.ReminderNotFoundException;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;
import pl.routecost.vehicle.exception.VehicleNotFoundException;

import java.time.LocalDate;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final VehicleRepository vehicleRepository;

    public ReminderService(ReminderRepository reminderRepository, VehicleRepository vehicleRepository) {
        this.reminderRepository = reminderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public ReminderPageResponse findReminders(
            Long ownerId,
            Long vehicleId,
            ReminderStatus status,
            LocalDate dueFrom,
            LocalDate dueTo,
            Pageable pageable
    ) {
        findOwnedVehicle(ownerId, vehicleId);
        validateDateRange(dueFrom, dueTo);
        Specification<Reminder> specification = (root, query, builder) ->
                builder.equal(root.get("vehicle").get("id"), vehicleId);
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (dueFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
        }
        if (dueTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("dueDate"), dueTo));
        }
        Page<ReminderResponse> page = reminderRepository.findAll(specification, pageable)
                .map(ReminderResponse::from);
        return ReminderPageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ReminderResponse findReminder(Long ownerId, Long vehicleId, Long reminderId) {
        findOwnedVehicle(ownerId, vehicleId);
        return ReminderResponse.from(findReminder(vehicleId, reminderId));
    }

    @Transactional
    public ReminderResponse createReminder(Long ownerId, Long vehicleId, ReminderRequest request) {
        Vehicle vehicle = vehicleRepository.findOwnedVehicleForUpdate(vehicleId, ownerId)
                .orElseThrow(VehicleNotFoundException::new);
        validateDueDate(vehicle, request.dueDate());
        Reminder reminder = new Reminder(
                vehicle,
                request.type(),
                request.title(),
                request.description(),
                request.dueDate(),
                request.odometer()
        );
        return ReminderResponse.from(reminderRepository.saveAndFlush(reminder));
    }

    @Transactional
    public ReminderResponse updateReminder(
            Long ownerId,
            Long vehicleId,
            Long reminderId,
            ReminderRequest request
    ) {
        Reminder reminder = lockOwnedReminder(ownerId, vehicleId, reminderId);
        validateDueDate(reminder.getVehicle(), request.dueDate());
        reminder.updateDetails(
                request.type(),
                request.title(),
                request.description(),
                request.dueDate(),
                request.odometer()
        );
        return ReminderResponse.from(reminderRepository.saveAndFlush(reminder));
    }

    @Transactional
    public ReminderResponse changeStatus(
            Long ownerId,
            Long vehicleId,
            Long reminderId,
            ReminderStatus status
    ) {
        Reminder reminder = lockOwnedReminder(ownerId, vehicleId, reminderId);
        reminder.changeStatus(status);
        return ReminderResponse.from(reminderRepository.saveAndFlush(reminder));
    }

    @Transactional
    public ReminderResponse reactivateReminder(Long ownerId, Long vehicleId, Long reminderId) {
        Reminder reminder = lockOwnedReminder(ownerId, vehicleId, reminderId);
        reminder.reactivate();
        return ReminderResponse.from(reminderRepository.saveAndFlush(reminder));
    }

    @Transactional
    public void deleteReminder(Long ownerId, Long vehicleId, Long reminderId) {
        reminderRepository.delete(lockOwnedReminder(ownerId, vehicleId, reminderId));
    }

    private void validateDateRange(LocalDate dueFrom, LocalDate dueTo) {
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw new ReminderDateRangeException();
        }
    }

    private void validateDueDate(Vehicle vehicle, LocalDate dueDate) {
        if (dueDate.isBefore(vehicle.getPurchaseDate())) {
            throw new ReminderChronologyException();
        }
    }

    private Vehicle findOwnedVehicle(Long ownerId, Long vehicleId) {
        return vehicleRepository.findByIdAndOwner_Id(vehicleId, ownerId)
                .orElseThrow(VehicleNotFoundException::new);
    }

    private Reminder lockOwnedReminder(Long ownerId, Long vehicleId, Long reminderId) {
        return reminderRepository.findOwnedReminderForUpdate(reminderId, vehicleId, ownerId)
                .orElseThrow(ReminderNotFoundException::new);
    }

    private Reminder findReminder(Long vehicleId, Long reminderId) {
        return reminderRepository.findByIdAndVehicle_Id(reminderId, vehicleId)
                .orElseThrow(ReminderNotFoundException::new);
    }
}
