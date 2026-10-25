package pl.routecost.fuel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuelEntryRepository extends JpaRepository<FuelEntry, Long> {

    Page<FuelEntry> findAllByVehicle_Id(Long vehicleId, Pageable pageable);

    List<FuelEntry> findAllByVehicle_IdOrderByEntryDateAscIdAsc(Long vehicleId);

    Optional<FuelEntry> findByIdAndVehicle_Id(Long id, Long vehicleId);
}
