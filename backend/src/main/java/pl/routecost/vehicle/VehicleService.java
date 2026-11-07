package pl.routecost.vehicle;

import pl.routecost.vehicle.dto.VehiclePageResponse;
import pl.routecost.vehicle.dto.VehicleRequest;
import pl.routecost.vehicle.dto.VehicleResponse;
import pl.routecost.vehicle.exception.OdometerRollbackException;
import pl.routecost.vehicle.exception.VehicleNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public VehiclePageResponse findVehicles(Long ownerId, FuelType fuelType, Pageable pageable) {
        Page<Vehicle> vehicles = fuelType == null
                ? vehicleRepository.findAllByOwner_Id(ownerId, pageable)
                : vehicleRepository.findAllByOwner_IdAndFuelType(ownerId, fuelType, pageable);

        return VehiclePageResponse.from(vehicles.map(VehicleResponse::from));
    }

    @Transactional(readOnly = true)
    public VehicleResponse findVehicle(Long ownerId, Long vehicleId) {
        return VehicleResponse.from(findOwnedVehicle(ownerId, vehicleId));
    }

    @Transactional
    public VehicleResponse createVehicle(Long ownerId, VehicleRequest request) {
        User owner = userRepository.findById(ownerId).orElseThrow(VehicleNotFoundException::new);
        Vehicle vehicle = new Vehicle(
                owner,
                request.make(),
                request.model(),
                request.productionYear(),
                request.fuelType(),
                request.currentOdometer(),
                request.purchaseDate(),
                request.purchasePrice()
        );
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse updateVehicle(Long ownerId, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = findOwnedVehicle(ownerId, vehicleId);
        if (request.currentOdometer() < vehicle.getCurrentOdometer()) {
            throw new OdometerRollbackException();
        }

        vehicle.updateDetails(
                request.make(),
                request.model(),
                request.productionYear(),
                request.fuelType(),
                request.currentOdometer(),
                request.purchaseDate(),
                request.purchasePrice()
        );
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long ownerId, Long vehicleId) {
        Vehicle vehicle = findOwnedVehicle(ownerId, vehicleId);
        vehicleRepository.delete(vehicle);
    }

    private Vehicle findOwnedVehicle(Long ownerId, Long vehicleId) {
        return vehicleRepository.findByIdAndOwner_Id(vehicleId, ownerId)
                .orElseThrow(VehicleNotFoundException::new);
    }
}
