package pl.routecost.expense;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.routecost.expense.dto.ExpensePageResponse;
import pl.routecost.expense.dto.ExpenseRequest;
import pl.routecost.expense.dto.ExpenseResponse;
import pl.routecost.expense.dto.ExpenseStatisticsResponse;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/expenses")
    public ExpensePageResponse findExpenses(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @PageableDefault(size = 20, sort = {"expenseDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return expenseService.findExpenses(userId(jwt), vehicleId, category, dateFrom, dateTo, pageable);
    }

    @GetMapping("/expenses/{expenseId}")
    public ExpenseResponse findExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long expenseId
    ) {
        return expenseService.findExpense(userId(jwt), vehicleId, expenseId);
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @Valid @RequestBody ExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.createExpense(userId(jwt), vehicleId, request);
        URI location = URI.create("/api/vehicles/" + vehicleId + "/expenses/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/expenses/{expenseId}")
    public ExpenseResponse updateExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseRequest request
    ) {
        return expenseService.updateExpense(userId(jwt), vehicleId, expenseId, request);
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @PathVariable Long expenseId
    ) {
        expenseService.deleteExpense(userId(jwt), vehicleId, expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expense-statistics")
    public ExpenseStatisticsResponse findExpenseStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId
    ) {
        return expenseService.findExpenseStatistics(userId(jwt), vehicleId);
    }

    private Long userId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        return userId.longValue();
    }
}
