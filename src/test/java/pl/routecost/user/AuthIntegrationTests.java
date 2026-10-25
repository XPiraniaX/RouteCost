package pl.routecost.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests {

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
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
}
