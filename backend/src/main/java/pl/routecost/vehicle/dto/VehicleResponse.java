package pl.routecost.vehicle.dto;

import pl.routecost.vehicle.FuelType;
import pl.routecost.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record VehicleResponse(
        Long id,
        String make,
        String model,
        int productionYear,
        FuelType fuelType,
        int currentOdometer,
        LocalDate purchaseDate,
        BigDecimal purchasePrice,
        OffsetDateTime createdAt
) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getProductionYear(),
                vehicle.getFuelType(),
                vehicle.getCurrentOdometer(),
                vehicle.getPurchaseDate(),
                vehicle.getPurchasePrice(),
                vehicle.getCreatedAt()
        );
    }
}
