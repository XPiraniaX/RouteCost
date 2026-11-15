package pl.routecost.admin;

import jakarta.validation.constraints.NotNull;
import pl.routecost.user.UserRole;

public record ChangeUserRoleRequest(@NotNull UserRole role) {
}
