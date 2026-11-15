package pl.routecost.admin;

import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(@NotNull Boolean enabled) {
}
