package pl.routecost.expense;

import org.springframework.stereotype.Component;
import pl.routecost.expense.dto.ExpenseCategoryTotal;
import pl.routecost.expense.dto.ExpenseCurrencyTotal;
import pl.routecost.expense.dto.ExpenseMonthlyTotal;
import pl.routecost.expense.dto.ExpenseStatisticsResponse;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ExpenseStatisticsCalculator {

    public ExpenseStatisticsResponse calculate(Long vehicleId, List<Expense> expenses) {
        Map<String, Aggregate> totalsByCurrency = new TreeMap<>();
        Map<CategoryCurrency, Aggregate> totalsByCategory = new TreeMap<>(
                Comparator.comparing(CategoryCurrency::category).thenComparing(CategoryCurrency::currency)
        );
        Map<MonthCurrency, Aggregate> monthlyTotals = new TreeMap<>(
                Comparator.comparing(MonthCurrency::month).thenComparing(MonthCurrency::currency)
        );

        for (Expense expense : expenses) {
            String currency = expense.getCurrency();
            BigDecimal amount = expense.getAmount();
            totalsByCurrency.computeIfAbsent(currency, ignored -> new Aggregate()).add(amount);
            totalsByCategory.computeIfAbsent(
                    new CategoryCurrency(expense.getCategory(), currency), ignored -> new Aggregate()
            ).add(amount);
            monthlyTotals.computeIfAbsent(
                    new MonthCurrency(YearMonth.from(expense.getExpenseDate()), currency), ignored -> new Aggregate()
            ).add(amount);
        }

        List<ExpenseCurrencyTotal> currencyTotals = totalsByCurrency.entrySet().stream()
                .map(entry -> new ExpenseCurrencyTotal(
                        entry.getKey(), entry.getValue().amount(), entry.getValue().count()
                ))
                .toList();
        List<ExpenseCategoryTotal> categoryTotals = totalsByCategory.entrySet().stream()
                .map(entry -> new ExpenseCategoryTotal(
                        entry.getKey().category(),
                        entry.getKey().currency(),
                        entry.getValue().amount(),
                        entry.getValue().count()
                ))
                .toList();
        List<ExpenseMonthlyTotal> months = monthlyTotals.entrySet().stream()
                .map(entry -> new ExpenseMonthlyTotal(
                        entry.getKey().month().getYear(),
                        entry.getKey().month().getMonthValue(),
                        entry.getKey().currency(),
                        entry.getValue().amount(),
                        entry.getValue().count()
                ))
                .toList();

        return new ExpenseStatisticsResponse(vehicleId, expenses.size(), currencyTotals, categoryTotals, months);
    }

    private record CategoryCurrency(ExpenseCategory category, String currency) {
    }

    private record MonthCurrency(YearMonth month, String currency) {
    }

    private static final class Aggregate {
        private BigDecimal amount = BigDecimal.ZERO;
        private long count;

        private void add(BigDecimal value) {
            amount = amount.add(value);
            count++;
        }

        private BigDecimal amount() {
            return amount;
        }

        private long count() {
            return count;
        }
    }
}
