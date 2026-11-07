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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "reminder_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reminder_notification_generation",
                columnNames = {"reminder_id", "activation_version", "notification_kind", "due_date"}
        )
)
public class ReminderNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @Column(name = "activation_version", nullable = false)
    private int activationVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_kind", nullable = false, length = 20)
    private ReminderNotificationKind kind;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderNotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    protected ReminderNotification() {
        // Required by JPA.
    }

    public ReminderNotification(Reminder reminder, ReminderNotificationKind kind, LocalDate dueDate) {
        this.reminder = reminder;
        this.activationVersion = reminder.getActivationVersion();
        this.kind = kind;
        this.dueDate = dueDate;
        this.status = ReminderNotificationStatus.PENDING;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        if (status != ReminderNotificationStatus.PENDING) {
            return;
        }
        status = ReminderNotificationStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public void markProcessed(OffsetDateTime processedAt) {
        if (status != ReminderNotificationStatus.PUBLISHED) {
            return;
        }
        status = ReminderNotificationStatus.PROCESSED;
        this.processedAt = processedAt;
    }

    public void markDiscarded(OffsetDateTime processedAt) {
        if (status != ReminderNotificationStatus.PUBLISHED) {
            return;
        }
        status = ReminderNotificationStatus.DISCARDED;
        this.processedAt = processedAt;
    }

    @PrePersist
    private void setCreationDefaults() {
        if (status == null) {
            status = ReminderNotificationStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public int getActivationVersion() {
        return activationVersion;
    }

    public ReminderNotificationKind getKind() {
        return kind;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public ReminderNotificationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
