package pl.routecost.user;

import pl.routecost.user.dto.AuthResponse;

public record IssuedAuthSession(AuthResponse authResponse, String refreshToken) {
}
