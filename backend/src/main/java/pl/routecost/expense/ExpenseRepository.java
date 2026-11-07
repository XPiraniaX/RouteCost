package pl.routecost.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.routecost.expense.dto.ExpenseCategoryTotal;
import pl.routecost.expense.dto.ExpenseCurrencyTotal;
import pl.routecost.expense.dto.ExpenseMonthlyTotal;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    long countByVehicle_Owner_Id(Long ownerId);

    long countByVehicle_Id(Long vehicleId);

    @Query("""
            select new pl.routecost.expense.dto.ExpenseCurrencyTotal(
                expense.currency, sum(expense.amount), count(expense)
            )
            from Expense expense
            where expense.vehicle.id = :vehicleId
            group by expense.currency
            order by expense.currency
            """)
    List<ExpenseCurrencyTotal> sumByCurrencyForVehicle(@Param("vehicleId") Long vehicleId);

    @Query("""
            select new pl.routecost.expense.dto.ExpenseCategoryTotal(
                expense.category, expense.currency, sum(expense.amount), count(expense)
            )
            from Expense expense
            where expense.vehicle.id = :vehicleId
            group by expense.category, expense.currency
            order by expense.category, expense.currency
            """)
    List<ExpenseCategoryTotal> sumByCategoryAndCurrencyForVehicle(@Param("vehicleId") Long vehicleId);

    @Query("""
            select new pl.routecost.expense.dto.ExpenseMonthlyTotal(
                year(expense.expenseDate), month(expense.expenseDate), expense.currency,
                sum(expense.amount), count(expense)
            )
            from Expense expense
            where expense.vehicle.id = :vehicleId
            group by year(expense.expenseDate), month(expense.expenseDate), expense.currency
            order by year(expense.expenseDate), month(expense.expenseDate), expense.currency
            """)
    List<ExpenseMonthlyTotal> sumByMonthAndCurrencyForVehicle(@Param("vehicleId") Long vehicleId);

    List<Expense> findAllByVehicle_IdOrderByExpenseDateAscIdAsc(Long vehicleId);

    Optional<Expense> findByIdAndVehicle_Id(Long id, Long vehicleId);
}
