package pl.routecost.user;

import pl.routecost.user.dto.LoginRequest;
import pl.routecost.user.dto.RegisterRequest;
import pl.routecost.user.exception.EmailAlreadyRegisteredException;
import pl.routecost.user.exception.InvalidCredentialsException;
import pl.routecost.user.exception.PasswordTooLongException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public UserAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public IssuedAuthSession register(RegisterRequest request) {
        validatePasswordLength(request.password());
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim()
        );

        final User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // The unique database constraint also protects against simultaneous registrations.
            throw new EmailAlreadyRegisteredException();
        }
        return createSession(savedUser);
    }

    @Transactional
    public IssuedAuthSession login(LoginRequest request) {
        validatePasswordLength(request.password());
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(User::isEnabled)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return createSession(user);
    }

    @Transactional
    public Optional<IssuedAuthSession> refresh(String refreshToken) {
        return refreshTokenService.rotate(refreshToken)
                .map(grant -> new IssuedAuthSession(jwtService.createAuthResponse(grant.user()), grant.rawToken()));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeFamily(refreshToken);
    }

    private IssuedAuthSession createSession(User user) {
        return new IssuedAuthSession(jwtService.createAuthResponse(user), refreshTokenService.issue(user).rawToken());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordTooLongException();
        }
    }
}
