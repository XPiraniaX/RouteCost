package pl.routecost.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    List<Expense> findAllByVehicle_IdOrderByExpenseDateAscIdAsc(Long vehicleId);

    Optional<Expense> findByIdAndVehicle_Id(Long id, Long vehicleId);
}
