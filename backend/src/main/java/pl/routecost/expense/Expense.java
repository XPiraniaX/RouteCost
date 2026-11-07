package pl.routecost.expense;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.routecost.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(length = 1000)
    private String description;

    @Column
    private Integer odometer;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Expense() {
        // Required by JPA.
    }

    public Expense(
            Vehicle vehicle,
            LocalDate expenseDate,
            ExpenseCategory category,
            BigDecimal amount,
            String currency,
            String description,
            Integer odometer
    ) {
        this.vehicle = vehicle;
        updateDetails(expenseDate, category, amount, currency, description, odometer);
    }

    public void updateDetails(
            LocalDate expenseDate,
            ExpenseCategory category,
            BigDecimal amount,
            String currency,
            String description,
            Integer odometer
    ) {
        this.expenseDate = expenseDate;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.odometer = odometer;
    }

    @PrePersist
    private void setCreatedAt() {
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

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
