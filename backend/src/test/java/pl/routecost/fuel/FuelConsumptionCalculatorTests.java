package pl.routecost.fuel;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.routecost.fuel.dto.FuelStatisticsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuelConsumptionCalculatorTests {

    private final FuelConsumptionCalculator calculator = new FuelConsumptionCalculator();

    @Test
    void measuresFuelFromAfterStartingFullTankThroughNextFullTank() {
        FuelEntry start = entry(1L, 10_000, "30.000", "150.00", true, 1);
        FuelEntry partial = entry(2L, 10_200, "10.000", "50.00", false, 2);
        FuelEntry finish = entry(3L, 10_400, "20.000", "100.00", true, 3);

        FuelConsumptionCalculator.Calculation result = calculator.calculate(7L, List.of(start, partial, finish));

        assertEquals(new BigDecimal("7.50"), result.statistics().averageConsumptionLitersPer100Km());
        assertEquals(new BigDecimal("7.50"), result.consumptionFor(3L));
    }

    @Test
    void calculatesWeightedAverageAcrossValidIntervals() {
        List<FuelEntry> entries = List.of(
                entry(1L, 10_000, "25.000", "125.00", true, 1),
                entry(2L, 10_400, "30.000", "150.00", true, 2),
                entry(3L, 11_000, "60.000", "300.00", true, 3)
        );

        FuelStatisticsResponse result = calculator.calculate(7L, entries).statistics();

        assertEquals(new BigDecimal("9.00"), result.averageConsumptionLitersPer100Km());
        assertEquals(2, result.validConsumptionIntervals());
    }

    @Test
    void doesNotReturnConsumptionForZeroDistance() {
        List<FuelEntry> entries = List.of(
                entry(1L, 10_000, "30.000", "150.00", true, 1),
                entry(2L, 10_000, "20.000", "100.00", true, 2)
        );

        FuelConsumptionCalculator.Calculation result = calculator.calculate(7L, entries);

        assertNull(result.statistics().averageConsumptionLitersPer100Km());
        assertNull(result.consumptionFor(2L));
        assertEquals(0, result.statistics().validConsumptionIntervals());
    }

    @Test
    void doesNotReturnConsumptionWithoutTwoFullTanks() {
        List<FuelEntry> entries = List.of(
                entry(1L, 10_000, "30.000", "150.00", true, 1),
                entry(2L, 10_200, "10.000", "50.00", false, 2)
        );

        FuelStatisticsResponse result = calculator.calculate(7L, entries).statistics();

        assertNull(result.averageConsumptionLitersPer100Km());
        assertEquals(0, result.validConsumptionIntervals());
    }

    @Test
    void totalsAllFuelEntriesSeparatelyFromMeasuredConsumption() {
        List<FuelEntry> entries = List.of(
                entry(1L, 10_000, "30.000", "150.00", true, 1),
                entry(2L, 10_400, "20.000", "100.00", true, 2)
        );

        FuelStatisticsResponse result = calculator.calculate(7L, entries).statistics();

        assertEquals(new BigDecimal("50.000"), result.totalLiters());
        assertEquals(new BigDecimal("250.00"), result.totalCost());
        assertEquals(2, result.fuelEntryCount());
        assertEquals(new BigDecimal("5.00"), result.averageConsumptionLitersPer100Km());
    }

    private FuelEntry entry(
            Long id,
            int odometer,
            String liters,
            String cost,
            boolean fullTank,
            int day
    ) {
        FuelEntry entry = new FuelEntry(
                null,
                LocalDate.of(2025, 1, day),
                odometer,
                new BigDecimal(liters),
                new BigDecimal(cost),
                fullTank
        );
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }
}
