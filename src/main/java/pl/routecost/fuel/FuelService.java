package pl.routecost.fuel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.fuel.dto.FuelEntryPageResponse;
import pl.routecost.fuel.dto.FuelEntryRequest;
import pl.routecost.fuel.dto.FuelEntryResponse;
import pl.routecost.fuel.dto.FuelStatisticsResponse;
import pl.routecost.fuel.exception.FuelEntryChronologyException;
import pl.routecost.fuel.exception.FuelEntryNotFoundException;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;
import pl.routecost.vehicle.exception.VehicleNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FuelService {

    private final FuelEntryRepository fuelEntryRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelConsumptionCalculator calculator;

    public FuelService(
            FuelEntryRepository fuelEntryRepository,
            VehicleRepository vehicleRepository,
            FuelConsumptionCalculator calculator
    ) {
        this.fuelEntryRepository = fuelEntryRepository;
        this.vehicleRepository = vehicleRepository;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public FuelEntryPageResponse findFuelEntries(Long ownerId, Long vehicleId, Pageable pageable) {
        findOwnedVehicle(ownerId, vehicleId);
        Page<FuelEntry> page = fuelEntryRepository.findAllByVehicle_Id(vehicleId, pageable);
        FuelConsumptionCalculator.Calculation calculation = calculate(vehicleId);
        Page<FuelEntryResponse> responsePage = page.map(entry -> toResponse(entry, calculation));
        return FuelEntryPageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public FuelEntryResponse findFuelEntry(Long ownerId, Long vehicleId, Long entryId) {
        findOwnedVehicle(ownerId, vehicleId);
        FuelEntry entry = findEntry(vehicleId, entryId);
        return toResponse(entry, calculate(vehicleId));
    }

    @Transactional
    public FuelEntryResponse createFuelEntry(Long ownerId, Long vehicleId, FuelEntryRequest request) {
        Vehicle vehicle = lockOwnedVehicle(ownerId, vehicleId);
        validateEntryDate(vehicle, request.entryDate());

        FuelEntry entry = new FuelEntry(
                vehicle,
                request.entryDate(),
                request.odometer(),
                request.liters(),
                request.totalCost(),
                request.fullTank()
        );
        List<TimelineEntry> timeline = timeline(vehicleId);
        timeline.add(TimelineEntry.from(entry));
        validateTimeline(timeline);

        FuelEntry saved = fuelEntryRepository.saveAndFlush(entry);
        return toResponse(saved, calculate(vehicleId));
    }

    @Transactional
    public FuelEntryResponse updateFuelEntry(
            Long ownerId,
            Long vehicleId,
            Long entryId,
            FuelEntryRequest request
    ) {
        Vehicle vehicle = lockOwnedVehicle(ownerId, vehicleId);
        FuelEntry entry = findEntry(vehicleId, entryId);
        validateEntryDate(vehicle, request.entryDate());

        List<TimelineEntry> timeline = timeline(vehicleId).stream()
                .filter(item -> !item.id().equals(entryId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        timeline.add(new TimelineEntry(entryId, request.entryDate(), request.odometer()));
        validateTimeline(timeline);

        entry.updateDetails(
                request.entryDate(),
                request.odometer(),
                request.liters(),
                request.totalCost(),
                request.fullTank()
        );
        FuelEntry updated = fuelEntryRepository.saveAndFlush(entry);
        return toResponse(updated, calculate(vehicleId));
    }

    @Transactional
    public void deleteFuelEntry(Long ownerId, Long vehicleId, Long entryId) {
        lockOwnedVehicle(ownerId, vehicleId);
        FuelEntry entry = findEntry(vehicleId, entryId);

        List<TimelineEntry> remainingTimeline = timeline(vehicleId).stream()
                .filter(item -> !item.id().equals(entryId))
                .toList();
        validateTimeline(remainingTimeline);
        fuelEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public FuelStatisticsResponse findFuelStatistics(Long ownerId, Long vehicleId) {
        findOwnedVehicle(ownerId, vehicleId);
        return calculate(vehicleId).statistics();
    }

    private FuelEntryResponse toResponse(FuelEntry entry, FuelConsumptionCalculator.Calculation calculation) {
        return FuelEntryResponse.from(entry, calculation.consumptionFor(entry.getId()));
    }

    private FuelConsumptionCalculator.Calculation calculate(Long vehicleId) {
        return calculator.calculate(
                vehicleId,
                fuelEntryRepository.findAllByVehicle_IdOrderByEntryDateAscIdAsc(vehicleId)
        );
    }

    private List<TimelineEntry> timeline(Long vehicleId) {
        return fuelEntryRepository.findAllByVehicle_IdOrderByEntryDateAscIdAsc(vehicleId)
                .stream()
                .map(TimelineEntry::from)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void validateEntryDate(Vehicle vehicle, LocalDate entryDate) {
        if (entryDate.isBefore(vehicle.getPurchaseDate())) {
            throw new FuelEntryChronologyException();
        }
    }

    private void validateTimeline(List<TimelineEntry> timeline) {
        timeline = new ArrayList<>(timeline);
        timeline.sort(Comparator.comparing(TimelineEntry::entryDate)
                .thenComparing(TimelineEntry::id, Comparator.nullsLast(Comparator.naturalOrder())));
        for (int index = 1; index < timeline.size(); index++) {
            if (timeline.get(index).odometer() < timeline.get(index - 1).odometer()) {
                throw new FuelEntryChronologyException();
            }
        }
    }

    private Vehicle findOwnedVehicle(Long ownerId, Long vehicleId) {
        return vehicleRepository.findByIdAndOwner_Id(vehicleId, ownerId)
                .orElseThrow(VehicleNotFoundException::new);
    }

    private Vehicle lockOwnedVehicle(Long ownerId, Long vehicleId) {
        return vehicleRepository.findOwnedVehicleForUpdate(vehicleId, ownerId)
                .orElseThrow(VehicleNotFoundException::new);
    }

    private FuelEntry findEntry(Long vehicleId, Long entryId) {
        return fuelEntryRepository.findByIdAndVehicle_Id(entryId, vehicleId)
                .orElseThrow(FuelEntryNotFoundException::new);
    }

    private record TimelineEntry(Long id, LocalDate entryDate, int odometer) {
        private static TimelineEntry from(FuelEntry entry) {
            return new TimelineEntry(entry.getId(), entry.getEntryDate(), entry.getOdometer());
        }
    }
}
