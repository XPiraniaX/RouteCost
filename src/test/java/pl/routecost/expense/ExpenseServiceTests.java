package pl.routecost.expense;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import pl.routecost.expense.exception.ExpenseDateRangeException;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;
import pl.routecost.vehicle.FuelType;
import pl.routecost.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpenseServiceTests {

    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final FuelEntryRepository fuelEntryRepository = mock(FuelEntryRepository.class);
    private final ExpenseStatisticsCalculator statisticsCalculator = new ExpenseStatisticsCalculator();
    private final ExpenseService service = new ExpenseService(
            expenseRepository,
            vehicleRepository,
            fuelEntryRepository,
            statisticsCalculator
    );

    @Test
    void rejectsDateRangeWithStartAfterEndBeforeQueryingExpenses() {
        Vehicle vehicle = new Vehicle(
                mock(User.class),
                "Toyota",
                "Corolla",
                2018,
                FuelType.PETROL,
                10_000,
                LocalDate.of(2022, 1, 1),
                new BigDecimal("10000.00")
        );
        when(vehicleRepository.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(vehicle));

        assertThrows(ExpenseDateRangeException.class, () -> service.findExpenses(
                1L,
                2L,
                null,
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 1, 31),
                Pageable.unpaged()
        ));
    }
}
