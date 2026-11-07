package pl.routecost.fuel.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record FuelEntryPageResponse(
        List<FuelEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static FuelEntryPageResponse from(Page<FuelEntryResponse> page) {
        return new FuelEntryPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
