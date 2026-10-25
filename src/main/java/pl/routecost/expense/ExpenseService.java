package pl.routecost.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.expense.dto.ExpensePageResponse;
import pl.routecost.expense.dto.ExpenseRequest;
import pl.routecost.expense.dto.ExpenseResponse;
import pl.routecost.expense.dto.ExpenseStatisticsResponse;
import pl.routecost.expense.exception.ExpenseChronologyException;
import pl.routecost.expense.exception.ExpenseDateRangeException;
import pl.routecost.expense.exception.ExpenseNotFoundException;
import pl.routecost.expense.exception.InvalidExpenseCurrencyException;
import pl.routecost.fuel.FuelEntry;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.vehicle.Vehicle;
import pl.routecost.vehicle.VehicleRepository;
import pl.routecost.vehicle.exception.VehicleNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final ExpenseStatisticsCalculator statisticsCalculator;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            VehicleRepository vehicleRepository,
            FuelEntryRepository fuelEntryRepository,
            ExpenseStatisticsCalculator statisticsCalculator
    ) {
        this.expenseRepository = expenseRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelEntryRepository = fuelEntryRepository;
        this.statisticsCalculator = statisticsCalculator;
    }

    @Transactional(readOnly = true)
    public ExpensePageResponse findExpenses(
            Long ownerId,
            Long vehicleId,
            ExpenseCategory category,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        findOwnedVehicle(ownerId, vehicleId);
        validateDateRange(dateFrom, dateTo);
        Specification<Expense> specification = (root, query, builder) ->
                builder.equal(root.get("vehicle").get("id"), vehicleId);
        if (category != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("category"), category));
        }
        if (dateFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("expenseDate"), dateFrom));
        }
        if (dateTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("expenseDate"), dateTo));
        }
        Page<ExpenseResponse> expenses = expenseRepository.findAll(specification, pageable)
                .map(ExpenseResponse::from);
        return ExpensePageResponse.from(expenses);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findExpense(Long ownerId, Long vehicleId, Long expenseId) {
        findOwnedVehicle(ownerId, vehicleId);
        return ExpenseResponse.from(findExpense(vehicleId, expenseId));
    }

    @Transactional
    public ExpenseResponse createExpense(Long ownerId, Long vehicleId, ExpenseRequest request) {
        Vehicle vehicle = lockOwnedVehicle(ownerId, vehicleId);
        validateExpenseDate(vehicle, request.expenseDate());
        validateCurrency(request.currency());

        Expense expense = new Expense(
                vehicle,
                request.expenseDate(),
                request.category(),
                request.amount(),
                request.currency(),
                request.description(),
                request.odometer()
        );
        List<OdometerPoint> timeline = odometerTimeline(vehicleId, null);
        addExpenseOdometer(timeline, expense);
        validateOdometerTimeline(timeline);

        return ExpenseResponse.from(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(Long ownerId, Long vehicleId, Long expenseId, ExpenseRequest request) {
        Vehicle vehicle = lockOwnedVehicle(ownerId, vehicleId);
        Expense expense = findExpense(vehicleId, expenseId);
        validateExpenseDate(vehicle, request.expenseDate());
        validateCurrency(request.currency());

        List<OdometerPoint> timeline = odometerTimeline(vehicleId, expenseId);
        addExpenseOdometer(timeline, request.expenseDate(), request.odometer());
        validateOdometerTimeline(timeline);

        expense.updateDetails(
                request.expenseDate(),
                request.category(),
                request.amount(),
                request.currency(),
                request.description(),
                request.odometer()
        );
        return ExpenseResponse.from(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void deleteExpense(Long ownerId, Long vehicleId, Long expenseId) {
        lockOwnedVehicle(ownerId, vehicleId);
        Expense expense = findExpense(vehicleId, expenseId);
        validateOdometerTimeline(odometerTimeline(vehicleId, expenseId));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseStatisticsResponse findExpenseStatistics(Long ownerId, Long vehicleId) {
        findOwnedVehicle(ownerId, vehicleId);
        List<Expense> expenses = expenseRepository.findAllByVehicle_IdOrderByExpenseDateAscIdAsc(vehicleId);
        return statisticsCalculator.calculate(vehicleId, expenses);
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ExpenseDateRangeException();
        }
    }

    private void validateExpenseDate(Vehicle vehicle, LocalDate expenseDate) {
        if (expenseDate.isBefore(vehicle.getPurchaseDate())) {
            throw new ExpenseChronologyException();
        }
    }

    private void validateCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException exception) {
            throw new InvalidExpenseCurrencyException();
        }
    }

    private List<OdometerPoint> odometerTimeline(Long vehicleId, Long excludedExpenseId) {
        List<OdometerPoint> timeline = new ArrayList<>();
        for (FuelEntry entry : fuelEntryRepository.findAllByVehicle_IdOrderByEntryDateAscIdAsc(vehicleId)) {
            timeline.add(new OdometerPoint(entry.getEntryDate(), entry.getOdometer()));
        }
        for (Expense expense : expenseRepository.findAllByVehicle_IdOrderByExpenseDateAscIdAsc(vehicleId)) {
            if (!expense.getId().equals(excludedExpenseId) && expense.getOdometer() != null) {
                timeline.add(new OdometerPoint(expense.getExpenseDate(), expense.getOdometer()));
            }
        }
        return timeline;
    }

    private void addExpenseOdometer(List<OdometerPoint> timeline, Expense expense) {
        addExpenseOdometer(timeline, expense.getExpenseDate(), expense.getOdometer());
    }

    private void addExpenseOdometer(List<OdometerPoint> timeline, LocalDate expenseDate, Integer odometer) {
        if (odometer != null) {
            timeline.add(new OdometerPoint(expenseDate, odometer));
        }
    }

    private void validateOdometerTimeline(List<OdometerPoint> timeline) {
        Map<LocalDate, OdometerRange> dailyRanges = new TreeMap<>();
        for (OdometerPoint point : timeline) {
            dailyRanges.computeIfAbsent(point.date(), ignored -> new OdometerRange()).include(point.odometer());
        }

        Integer previousDayMaximum = null;
        for (OdometerRange range : dailyRanges.values()) {
            if (previousDayMaximum != null && range.minimum() < previousDayMaximum) {
                throw new ExpenseChronologyException();
            }
            previousDayMaximum = range.maximum();
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

    private Expense findExpense(Long vehicleId, Long expenseId) {
        return expenseRepository.findByIdAndVehicle_Id(expenseId, vehicleId)
                .orElseThrow(ExpenseNotFoundException::new);
    }

    private record OdometerPoint(LocalDate date, int odometer) {
    }

    private static final class OdometerRange {
        private Integer minimum;
        private Integer maximum;

        private void include(int odometer) {
            minimum = minimum == null ? odometer : Math.min(minimum, odometer);
            maximum = maximum == null ? odometer : Math.max(maximum, odometer);
        }

        private int minimum() {
            return minimum;
        }

        private int maximum() {
            return maximum;
        }
    }
}
