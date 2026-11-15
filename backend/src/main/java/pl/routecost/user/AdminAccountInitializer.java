package pl.routecost.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${routecost.admin.email:}") String email,
            @Value("${routecost.admin.password:}") String password,
            @Value("${routecost.admin.first-name:}") String firstName,
            @Value("${routecost.admin.last-name:}") String lastName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean configured = !email.isBlank() || !password.isBlank()
                || !firstName.isBlank() || !lastName.isBlank();
        if (!configured) {
            return;
        }
        if (email.isBlank() || password.isBlank() || firstName.isBlank() || lastName.isBlank()) {
            throw new IllegalStateException(
                    "Initial admin requires INITIAL_ADMIN_EMAIL, INITIAL_ADMIN_PASSWORD, "
                            + "INITIAL_ADMIN_FIRST_NAME and INITIAL_ADMIN_LAST_NAME"
            );
        }
        if (password.getBytes(StandardCharsets.UTF_8).length < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Initial admin password must be between 8 and 72 UTF-8 bytes");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var existing = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            if (existing.get().getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                        "Configured initial admin email belongs to a non-admin account; refusing role promotion"
                );
            }
            return;
        }

        userRepository.saveAndFlush(new User(
                normalizedEmail,
                passwordEncoder.encode(password),
                firstName.trim(),
                lastName.trim(),
                UserRole.ADMIN
        ));
    }
}
