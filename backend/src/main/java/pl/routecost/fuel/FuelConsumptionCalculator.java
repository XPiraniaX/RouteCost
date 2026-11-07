package pl.routecost.fuel;

import org.springframework.stereotype.Component;
import pl.routecost.fuel.dto.FuelStatisticsResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FuelConsumptionCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int RESULT_SCALE = 2;

    public Calculation calculate(Long vehicleId, List<FuelEntry> sourceEntries) {
        List<FuelEntry> entries = new ArrayList<>(sourceEntries);
        entries.sort(Comparator.comparing(FuelEntry::getEntryDate).thenComparing(FuelEntry::getId));

        BigDecimal totalLiters = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (FuelEntry entry : entries) {
            totalLiters = totalLiters.add(entry.getLiters());
            totalCost = totalCost.add(entry.getTotalCost());
        }

        Map<Long, BigDecimal> consumptionByEntryId = new HashMap<>();
        BigDecimal measuredLiters = BigDecimal.ZERO;
        long measuredKilometers = 0;
        int validIntervals = 0;
        int previousFullTankIndex = -1;

        for (int currentIndex = 0; currentIndex < entries.size(); currentIndex++) {
            FuelEntry current = entries.get(currentIndex);
            if (!current.isFullTank()) {
                continue;
            }

            if (previousFullTankIndex >= 0) {
                FuelEntry previousFullTank = entries.get(previousFullTankIndex);
                int distance = current.getOdometer() - previousFullTank.getOdometer();
                if (distance > 0) {
                    BigDecimal intervalLiters = BigDecimal.ZERO;
                    for (int index = previousFullTankIndex + 1; index <= currentIndex; index++) {
                        intervalLiters = intervalLiters.add(entries.get(index).getLiters());
                    }
                    BigDecimal consumption = intervalLiters.multiply(ONE_HUNDRED)
                            .divide(BigDecimal.valueOf(distance), RESULT_SCALE, RoundingMode.HALF_UP);
                    consumptionByEntryId.put(current.getId(), consumption);
                    measuredLiters = measuredLiters.add(intervalLiters);
                    measuredKilometers += distance;
                    validIntervals++;
                }
            }
            previousFullTankIndex = currentIndex;
        }

        BigDecimal average = measuredKilometers == 0
                ? null
                : measuredLiters.multiply(ONE_HUNDRED)
                    .divide(BigDecimal.valueOf(measuredKilometers), RESULT_SCALE, RoundingMode.HALF_UP);
        FuelStatisticsResponse statistics = new FuelStatisticsResponse(
                vehicleId,
                average,
                totalLiters,
                totalCost,
                entries.size(),
                validIntervals
        );
        return new Calculation(statistics, Map.copyOf(consumptionByEntryId));
    }

    public record Calculation(FuelStatisticsResponse statistics, Map<Long, BigDecimal> consumptionByEntryId) {
        public BigDecimal consumptionFor(Long entryId) {
            return consumptionByEntryId.get(entryId);
        }
    }
}
