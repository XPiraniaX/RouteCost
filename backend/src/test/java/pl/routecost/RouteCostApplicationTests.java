package pl.routecost;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.user.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "routecost.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class RouteCostApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void persistsUserWithDefaultsAndFindsEmailIgnoringCase() {
        User savedUser = userRepository.saveAndFlush(
                new User(" Test.User@Example.com ", "bcrypt-hash", "Test", "User")
        );

        assertEquals("test.user@example.com", savedUser.getEmail());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertTrue(savedUser.isEnabled());
        assertNotNull(savedUser.getCreatedAt());
        assertTrue(userRepository.existsByEmailIgnoreCase("TEST.USER@example.com"));
        assertEquals(savedUser.getId(), userRepository.findByEmailIgnoreCase("TEST.USER@example.com")
                .orElseThrow()
                .getId());
    }
}
