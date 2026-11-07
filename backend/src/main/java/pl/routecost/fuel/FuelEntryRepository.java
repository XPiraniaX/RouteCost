package pl.routecost.fuel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.routecost.dashboard.dto.MonthlyFuelCost;

import java.util.List;
import java.util.Optional;

public interface FuelEntryRepository extends JpaRepository<FuelEntry, Long> {

    long countByVehicle_Owner_Id(Long ownerId);

    long countByVehicle_Id(Long vehicleId);

    @Query("""
            select new pl.routecost.dashboard.dto.MonthlyFuelCost(
                year(entry.entryDate), month(entry.entryDate), sum(entry.totalCost), count(entry)
            )
            from FuelEntry entry
            where entry.vehicle.id = :vehicleId
            group by year(entry.entryDate), month(entry.entryDate)
            order by year(entry.entryDate), month(entry.entryDate)
            """)
    List<MonthlyFuelCost> findMonthlyCostsByVehicleId(@Param("vehicleId") Long vehicleId);

    Page<FuelEntry> findAllByVehicle_Id(Long vehicleId, Pageable pageable);

    List<FuelEntry> findAllByVehicle_IdOrderByEntryDateAscIdAsc(Long vehicleId);

    Optional<FuelEntry> findByIdAndVehicle_Id(Long id, Long vehicleId);
}
