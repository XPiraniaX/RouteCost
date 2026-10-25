package pl.routecost.vehicle.dto;

import org.springframework.data.domain.Page;
import pl.routecost.vehicle.Vehicle;

import java.util.List;

public record VehiclePageResponse(
        List<VehicleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static VehiclePageResponse from(Page<VehicleResponse> page) {
        return new VehiclePageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
