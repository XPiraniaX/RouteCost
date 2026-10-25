package pl.routecost.expense;

import org.junit.jupiter.api.Test;
import pl.routecost.expense.dto.ExpenseStatisticsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseStatisticsCalculatorTests {

    private final ExpenseStatisticsCalculator calculator = new ExpenseStatisticsCalculator();

    @Test
    void aggregatesByCurrencyCategoryAndMonthWithoutCombiningCurrencies() {
        List<Expense> expenses = List.of(
                expense(ExpenseCategory.REPAIR, "100.00", "PLN", "2024-01-15"),
                expense(ExpenseCategory.PARTS, "50.00", "PLN", "2024-02-02"),
                expense(ExpenseCategory.INSURANCE, "20.00", "EUR", "2024-02-03"),
                expense(ExpenseCategory.REPAIR, "25.00", "PLN", "2024-02-10")
        );

        ExpenseStatisticsResponse result = calculator.calculate(9L, expenses);

        assertEquals(4, result.expenseCount());
        assertEquals("EUR", result.totalsByCurrency().getFirst().currency());
        assertEquals(new BigDecimal("20.00"), result.totalsByCurrency().getFirst().totalAmount());
        assertEquals("PLN", result.totalsByCurrency().get(1).currency());
        assertEquals(new BigDecimal("175.00"), result.totalsByCurrency().get(1).totalAmount());
        assertEquals(3, result.totalsByCategory().size());
        assertEquals(3, result.monthlyTotals().size());
        assertEquals(2024, result.monthlyTotals().get(0).year());
        assertEquals(2, result.monthlyTotals().get(1).month());
    }

    private Expense expense(ExpenseCategory category, String amount, String currency, String date) {
        return new Expense(
                null,
                LocalDate.parse(date),
                category,
                new BigDecimal(amount),
                currency,
                null,
                null
        );
    }
}
