package pl.routecost.vehicle;

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
import pl.routecost.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String make;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "production_year", nullable = false)
    private int productionYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Column(name = "current_odometer", nullable = false)
    private int currentOdometer;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Vehicle() {
        // Required by JPA.
    }

    public Vehicle(
            User owner,
            String make,
            String model,
            int productionYear,
            FuelType fuelType,
            int currentOdometer,
            LocalDate purchaseDate,
            BigDecimal purchasePrice
    ) {
        this.owner = owner;
        updateDetails(make, model, productionYear, fuelType, currentOdometer, purchaseDate, purchasePrice);
    }

    public void updateDetails(
            String make,
            String model,
            int productionYear,
            FuelType fuelType,
            int currentOdometer,
            LocalDate purchaseDate,
            BigDecimal purchasePrice
    ) {
        this.make = make.trim();
        this.model = model.trim();
        this.productionYear = productionYear;
        this.fuelType = fuelType;
        this.currentOdometer = currentOdometer;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
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

    public User getOwner() {
        return owner;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getProductionYear() {
        return productionYear;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public int getCurrentOdometer() {
        return currentOdometer;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
