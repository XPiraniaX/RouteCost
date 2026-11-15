package pl.routecost.admin;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminUserPageResponse from(Page<AdminUserResponse> page) {
        return new AdminUserPageResponse(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }
}
