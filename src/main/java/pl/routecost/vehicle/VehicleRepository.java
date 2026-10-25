package pl.routecost.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findAllByOwner_Id(Long ownerId, Pageable pageable);

    Page<Vehicle> findAllByOwner_IdAndFuelType(Long ownerId, FuelType fuelType, Pageable pageable);

    Optional<Vehicle> findByIdAndOwner_Id(Long id, Long ownerId);
}
