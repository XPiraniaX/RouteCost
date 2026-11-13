package pl.routecost.user;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.routecost.user.dto.AuthResponse;
import pl.routecost.user.dto.LoginRequest;
import pl.routecost.user.dto.RegisterRequest;
import pl.routecost.user.dto.UserProfileResponse;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String CSRF_HEADER = "X-RouteCost-Auth";
    private final UserAuthService userAuthService;
    private final String refreshCookieName;
    private final Duration refreshTokenLifetime;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public AuthController(
            UserAuthService userAuthService,
            @Value("${routecost.auth.refresh-cookie-name:routecost_refresh}") String refreshCookieName,
            @Value("${routecost.auth.refresh-token-expiration:P7D}") Duration refreshTokenLifetime,
            @Value("${routecost.auth.refresh-cookie-secure:false}") boolean refreshCookieSecure,
            @Value("${routecost.auth.refresh-cookie-same-site:Lax}") String refreshCookieSameSite
    ) {
        this.userAuthService = userAuthService;
        this.refreshCookieName = refreshCookieName;
        this.refreshTokenLifetime = refreshTokenLifetime;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        if ("None".equalsIgnoreCase(refreshCookieSameSite) && !refreshCookieSecure) {
            throw new IllegalStateException("SameSite=None refresh cookies require Secure=true");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        IssuedAuthSession session = userAuthService.register(request);
        return ResponseEntity.created(URI.create("/api/auth/me"))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.authResponse());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedAuthSession session = userAuthService.login(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.authResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(
                    name = "${routecost.auth.refresh-cookie-name:routecost_refresh}",
                    required = false
            ) String refreshToken,
            @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader
    ) {
        if (!"1".equals(csrfHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).cacheControl(CacheControl.noStore()).build();
        }
        Optional<IssuedAuthSession> refreshed = userAuthService.refresh(refreshToken);
        if (refreshed.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie())
                    .build();
        }
        IssuedAuthSession session = refreshed.get();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.authResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = "${routecost.auth.refresh-cookie-name:routecost_refresh}",
                    required = false
            ) String refreshToken,
            @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader
    ) {
        if (!"1".equals(csrfHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).cacheControl(CacheControl.noStore()).build();
        }
        userAuthService.logout(refreshToken);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie()).build();
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

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(refreshTokenLifetime)
                .build();
    }

    private String clearedRefreshCookie() {
        return refreshCookie("").mutate().maxAge(Duration.ZERO).build().toString();
    }
}
