package pl.routecost.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import pl.routecost.reminder.Reminder;
import pl.routecost.reminder.ReminderNotification;
import pl.routecost.user.User;
import pl.routecost.vehicle.Vehicle;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_notifications", uniqueConstraints = @UniqueConstraint(
        name = "uq_user_notification_source", columnNames = "reminder_notification_id"
))
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_notification_id", nullable = false)
    private ReminderNotification source;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    protected UserNotification() {
        // Required by JPA.
    }

    public UserNotification(ReminderNotification source, User user, Vehicle vehicle, Reminder reminder,
                            String title, String message, OffsetDateTime createdAt) {
        this.source = source;
        this.user = user;
        this.vehicle = vehicle;
        this.reminder = reminder;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }

    public void markRead(OffsetDateTime at) {
        if (readAt == null) readAt = at;
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public ReminderNotification getSource() { return source; }
    public User getUser() { return user; }
    public Vehicle getVehicle() { return vehicle; }
    public Reminder getReminder() { return reminder; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getReadAt() { return readAt; }
}
