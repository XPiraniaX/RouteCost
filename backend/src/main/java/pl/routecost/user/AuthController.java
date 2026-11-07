package pl.routecost.user;

import jakarta.validation.Valid;
import pl.routecost.user.dto.AuthResponse;
import pl.routecost.user.dto.LoginRequest;
import pl.routecost.user.dto.RegisterRequest;
import pl.routecost.user.dto.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAuthService userAuthService;

    public AuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userAuthService.register(request);
        return ResponseEntity.created(URI.create("/api/auth/me")).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userAuthService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        List<String> roles = jwt.getClaimAsStringList("roles");

        return new UserProfileResponse(
                userId.longValue(),
                jwt.getSubject(),
                jwt.getClaimAsString("firstName"),
                jwt.getClaimAsString("lastName"),
                UserRole.valueOf(roles.getFirst())
        );
    }
}
