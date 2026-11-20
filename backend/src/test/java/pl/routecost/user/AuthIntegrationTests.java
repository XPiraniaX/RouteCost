package pl.routecost.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.AbstractIntegrationTest;

import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests extends AbstractIntegrationTest {

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void registersAndAllowsAccessToCurrentUserEndpointWithIssuedToken() throws Exception {
        String email = uniqueEmail();
        String request = registrationRequest(email);

        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        var refreshCookie = registration.getResponse().getCookie("routecost_refresh");
        assertTrue(refreshCookie != null && refreshCookie.isHttpOnly());
        assertTrue(registration.getResponse().getHeader("Set-Cookie").contains("Path=/api/auth"));
        assertTrue(registration.getResponse().getHeader("Set-Cookie").contains("SameSite=Lax"));
        assertTrue(registration.getResponse().getHeader("Set-Cookie").contains("Max-Age=604800"));
        assertTrue(!registration.getResponse().getContentAsString().contains("refreshToken"));

        User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotEquals("Correct-Horse-Battery-12!", savedUser.getPasswordHash());
        assertTrue(
                passwordEncoder.matches("Correct-Horse-Battery-12!", savedUser.getPasswordHash())
        );

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + extractToken(registration.getResponse().getContentAsString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String request = registrationRequest(uniqueEmail());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email address already exists."));
    }

    @Test
    void loginRejectsWrongPasswordAndAcceptsCorrectPassword() throws Exception {
        String email = uniqueEmail();
        userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode("Correct-Horse-Battery-12!"),
                "Auth",
                "Test"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email, "Wrong-Password-12!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email, "Correct-Horse-Battery-12!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.containsString("HttpOnly")
                ));
    }

    @Test
    void validatesRegistrationInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","firstName":"","lastName":"Test"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void ignoresAttemptToChooseAdminRoleDuringPublicRegistration() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Correct-Horse-Battery-12!",
                                 "firstName":"Auth","lastName":"Test","role":"ADMIN"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"));

        org.junit.jupiter.api.Assertions.assertEquals(
                UserRole.USER, userRepository.findByEmailIgnoreCase(email).orElseThrow().getRole()
        );
    }

    @Test
    void adminUsersEndpointRequiresAdminRoleAndReturnsOnlySafeUserFields() throws Exception {
        User regularUser = userRepository.saveAndFlush(new User(
                uniqueEmail(), passwordEncoder.encode("Correct-Horse-Battery-12!"), "Regular", "User"
        ));
        User administrator = userRepository.saveAndFlush(new User(
                uniqueEmail(), passwordEncoder.encode("Correct-Horse-Battery-12!"), "System", "Admin", UserRole.ADMIN
        ));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + jwtService.createAuthResponse(regularUser).accessToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + jwtService.createAuthResponse(administrator).accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").exists())
                .andExpect(jsonPath("$.content[0].role").exists())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void rejectsPasswordThatExceedsBcryptByteLimit() throws Exception {
        String longUnicodePassword = "🔐".repeat(19);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequestWithPassword(uniqueEmail(), longUnicodePassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid password"));
    }

    @Test
    void protectsCurrentUserEndpointFromUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshesAnExpiredAccessTokenAndRotatesTheHttpOnlyCookie() throws Exception {
        String email = uniqueEmail();
        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(email)))
                .andExpect(status().isCreated())
                .andReturn();
        String oldRefreshToken = registration.getResponse().getCookie("routecost_refresh").getValue();
        String expiredAccessToken = expiredAccessToken(userRepository.findByEmailIgnoreCase(email).orElseThrow());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredAccessToken))
                .andExpect(status().isUnauthorized());

        var refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", oldRefreshToken))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String nextRefreshToken = refreshed.getResponse().getCookie("routecost_refresh").getValue();
        assertNotEquals(oldRefreshToken, nextRefreshToken);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + extractToken(refreshed.getResponse().getContentAsString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void rejectsInvalidAndExpiredRefreshTokensAndClearsTheirCookies() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", "not-a-valid-token"))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertTrue(result.getResponse().getHeader("Set-Cookie").contains("Max-Age=0")));

        User user = userRepository.saveAndFlush(new User(
                uniqueEmail(), passwordEncoder.encode("Correct-Horse-Battery-12!"), "Auth", "Test"
        ));
        String expired = "expired-refresh-token-" + UUID.randomUUID();
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user, refreshTokenService.hash(expired), UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.SECONDS), Instant.now().minus(8, ChronoUnit.DAYS)
        ));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", expired))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertTrue(result.getResponse().getHeader("Set-Cookie").contains("Max-Age=0")));
    }

    @Test
    void logoutRevokesRefreshSessionAndClearsCookie() throws Exception {
        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(uniqueEmail())))
                .andExpect(status().isCreated())
                .andReturn();
        String refreshToken = registration.getResponse().getCookie("routecost_refresh").getValue();

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", refreshToken))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertTrue(result.getResponse().getHeader("Set-Cookie").contains("Max-Age=0")));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", refreshToken))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replayingRotatedRefreshTokenRevokesItsTokenFamily() throws Exception {
        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(uniqueEmail())))
                .andExpect(status().isCreated())
                .andReturn();
        String first = registration.getResponse().getCookie("routecost_refresh").getValue();
        var rotation = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", first))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isOk())
                .andReturn();
        String second = rotation.getResponse().getCookie("routecost_refresh").getValue();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", first))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", second))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEndpointRequiresTheCsrfRequestHeader() throws Exception {
        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(uniqueEmail())))
                .andExpect(status().isCreated())
                .andReturn();
        String refreshToken = registration.getResponse().getCookie("routecost_refresh").getValue();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", refreshToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("routecost_refresh", refreshToken))
                        .header("X-RouteCost-Auth", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightAllowsCredentialedRefreshFromConfiguredFrontend() throws Exception {
        mockMvc.perform(options("/api/auth/refresh")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "x-routecost-auth"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("x-routecost-auth")));
    }

    private String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@routecost.example";
    }

    private String registrationRequest(String email) {
        return registrationRequestWithPassword(email, "Correct-Horse-Battery-12!");
    }

    private String registrationRequestWithPassword(String email, String password) {
        return """
                {"email":"%s","password":"%s","firstName":"Auth","lastName":"Test"}
                """.formatted(email, password);
    }

    private String loginRequest(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private String extractToken(String responseBody) {
        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new AssertionError("Authentication response did not contain an access token");
        }
        return matcher.group(1);
    }

    private String expiredAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("routecost")
                .issuedAt(now.minusSeconds(180))
                .expiresAt(now.minusSeconds(120))
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("roles", java.util.List.of(user.getRole().name()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims
        )).getTokenValue();
    }
}
