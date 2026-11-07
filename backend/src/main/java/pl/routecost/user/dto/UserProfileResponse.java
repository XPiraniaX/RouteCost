package pl.routecost.user.dto;

import pl.routecost.user.UserRole;

public record UserProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {
}
