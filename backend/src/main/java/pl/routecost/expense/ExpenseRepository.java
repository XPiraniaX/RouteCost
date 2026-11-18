package pl.routecost.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.routecost.expense.dto.ExpenseCategoryTotal;
import pl.routecost.expense.dto.ExpenseMonthlyTotal;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    long countByVehicle_Owner_Id(Long ownerId);

    @Query("select sum(expense.amount) from Expense expense where expense.vehicle.owner.id = :ownerId")
    BigDecimal sumAmountByOwnerId(@Param("ownerId") Long ownerId);

    @Query("select sum(expense.amount) from Expense expense where expense.vehicle.owner.id = :ownerId and expense.expenseDate >= :fromDate")
    BigDecimal sumAmountByOwnerIdFromDate(
            @Param("ownerId") Long ownerId,
            @Param("fromDate") LocalDate fromDate
    );

    long countByVehicle_Id(Long vehicleId);

    @Query("select coalesce(sum(expense.amount), 0) from Expense expense where expense.vehicle.id = :vehicleId")
    BigDecimal sumAmountByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query("""
            select new pl.routecost.expense.dto.ExpenseCategoryTotal(
                expense.category, sum(expense.amount), count(expense)
            )
            from Expense expense
            where expense.vehicle.id = :vehicleId
            group by expense.category
            order by expense.category
            """)
    List<ExpenseCategoryTotal> sumByCategoryForVehicle(@Param("vehicleId") Long vehicleId);

    @Query("""
            select new pl.routecost.expense.dto.ExpenseMonthlyTotal(
                year(expense.expenseDate), month(expense.expenseDate),
                sum(expense.amount), count(expense)
            )
            from Expense expense
            where expense.vehicle.id = :vehicleId
            group by year(expense.expenseDate), month(expense.expenseDate)
            order by year(expense.expenseDate), month(expense.expenseDate)
            """)
    List<ExpenseMonthlyTotal> sumByMonthForVehicle(@Param("vehicleId") Long vehicleId);

    List<Expense> findAllByVehicle_IdOrderByExpenseDateAscIdAsc(Long vehicleId);

    Optional<Expense> findFirstByVehicle_IdOrderByExpenseDateAscIdAsc(Long vehicleId);

    Optional<Expense> findByIdAndVehicle_Id(Long id, Long vehicleId);
}
