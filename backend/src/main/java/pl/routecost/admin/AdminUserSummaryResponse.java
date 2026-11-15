package pl.routecost.admin;

import pl.routecost.user.User;
import pl.routecost.user.UserRole;

import java.time.OffsetDateTime;

public record AdminUserSummaryResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        OffsetDateTime createdAt
) {
    public static AdminUserSummaryResponse from(User user) {
        return new AdminUserSummaryResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.getCreatedAt()
        );
    }
}
