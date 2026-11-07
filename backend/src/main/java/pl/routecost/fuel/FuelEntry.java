package pl.routecost.fuel;

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
import pl.routecost.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fuel_entries")
public class FuelEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private int odometer;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal liters;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "full_tank", nullable = false)
    private boolean fullTank;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected FuelEntry() {
        // Required by JPA.
    }

    public FuelEntry(
            Vehicle vehicle,
            LocalDate entryDate,
            int odometer,
            BigDecimal liters,
            BigDecimal totalCost,
            boolean fullTank
    ) {
        this.vehicle = vehicle;
        updateDetails(entryDate, odometer, liters, totalCost, fullTank);
    }

    public void updateDetails(
            LocalDate entryDate,
            int odometer,
            BigDecimal liters,
            BigDecimal totalCost,
            boolean fullTank
    ) {
        this.entryDate = entryDate;
        this.odometer = odometer;
        this.liters = liters;
        this.totalCost = totalCost;
        this.fullTank = fullTank;
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

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public int getOdometer() {
        return odometer;
    }

    public BigDecimal getLiters() {
        return liters;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public boolean isFullTank() {
        return fullTank;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
