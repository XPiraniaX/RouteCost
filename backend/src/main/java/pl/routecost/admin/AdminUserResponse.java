package pl.routecost.admin;

import pl.routecost.user.User;
import pl.routecost.user.UserRole;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean enabled,
        OffsetDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.isEnabled(), user.getCreatedAt()
        );
    }
}
