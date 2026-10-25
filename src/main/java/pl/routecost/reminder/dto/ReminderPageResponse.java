package pl.routecost.reminder.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReminderPageResponse(
        List<ReminderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ReminderPageResponse from(Page<ReminderResponse> page) {
        return new ReminderPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
