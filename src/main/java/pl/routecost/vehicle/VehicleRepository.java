package pl.routecost.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findAllByOwner_Id(Long ownerId, Pageable pageable);

    Page<Vehicle> findAllByOwner_IdAndFuelType(Long ownerId, FuelType fuelType, Pageable pageable);

    Optional<Vehicle> findByIdAndOwner_Id(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vehicle from Vehicle vehicle where vehicle.id = :vehicleId and vehicle.owner.id = :ownerId")
    Optional<Vehicle> findOwnedVehicleForUpdate(
            @Param("vehicleId") Long vehicleId,
            @Param("ownerId") Long ownerId
    );
}
