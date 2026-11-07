package pl.routecost.expense.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ExpensePageResponse(
        List<ExpenseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ExpensePageResponse from(Page<ExpenseResponse> page) {
        return new ExpensePageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
