package pl.routecost.admin;

public class AdminUserNotFoundException extends RuntimeException {
    public AdminUserNotFoundException(Long userId) {
        super("User was not found: " + userId);
    }
}
