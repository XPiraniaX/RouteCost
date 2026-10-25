package pl.routecost.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import pl.routecost.reminder.exception.InvalidReminderStatusTransitionException;
import pl.routecost.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status;

    @Column
    private Integer odometer;

    @Column(name = "activation_version", nullable = false)
    private int activationVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Reminder() {
        // Required by JPA.
    }

    public Reminder(
            Vehicle vehicle,
            ReminderType type,
            String title,
            String description,
            LocalDate dueDate,
            Integer odometer
    ) {
        this.vehicle = vehicle;
        updateDetails(type, title, description, dueDate, odometer);
        this.status = ReminderStatus.ACTIVE;
        this.activationVersion = 1;
    }

    public void updateDetails(
            ReminderType type,
            String title,
            String description,
            LocalDate dueDate,
            Integer odometer
    ) {
        this.type = type;
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.dueDate = dueDate;
        this.odometer = odometer;
    }

    public void changeStatus(ReminderStatus newStatus) {
        if (newStatus == status) {
            return;
        }
        if (status != ReminderStatus.ACTIVE || newStatus == ReminderStatus.ACTIVE) {
            throw new InvalidReminderStatusTransitionException();
        }
        status = newStatus;
    }

    public void reactivate() {
        if (status == ReminderStatus.ACTIVE) {
            throw new InvalidReminderStatusTransitionException();
        }
        status = ReminderStatus.ACTIVE;
        activationVersion++;
    }

    @PrePersist
    private void setCreationDefaults() {
        if (status == null) {
            status = ReminderStatus.ACTIVE;
        }
        if (activationVersion == 0) {
            activationVersion = 1;
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ReminderType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public int getActivationVersion() {
        return activationVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
