package pl.routecost.user;

public record RefreshTokenGrant(User user, String rawToken) {
}
