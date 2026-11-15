package pl.routecost.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.ArgumentCaptor;

class AdminAccountInitializerTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void doesNothingWhenInitialAdminIsNotConfigured() {
        AdminAccountInitializer initializer = initializer("", "", "", "");

        assertDoesNotThrow(() -> initializer.run(new DefaultApplicationArguments(new String[0])));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsAdminWithEncodedPasswordWhenFullyConfigured() {
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Strong-Admin-Password-12!")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminAccountInitializer initializer = initializer(
                " Admin@Example.com ", "Strong-Admin-Password-12!", " System ", " Admin "
        );

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(passwordEncoder).encode("Strong-Admin-Password-12!");
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertEquals(UserRole.ADMIN, savedUser.getValue().getRole());
        assertEquals("admin@example.com", savedUser.getValue().getEmail());
        assertEquals("bcrypt-hash", savedUser.getValue().getPasswordHash());
    }

    @Test
    void refusesPartialConfigurationAndNeverPromotesExistingUser() {
        assertThrows(IllegalStateException.class,
                () -> initializer("admin@example.com", "", "System", "Admin")
                        .run(new DefaultApplicationArguments(new String[0])));

        User regularUser = new User("admin@example.com", "hash", "Regular", "User");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(regularUser));
        assertThrows(IllegalStateException.class,
                () -> initializer("admin@example.com", "Strong-Admin-Password-12!", "System", "Admin")
                        .run(new DefaultApplicationArguments(new String[0])));
        verify(userRepository, never()).saveAndFlush(any());
    }

    private AdminAccountInitializer initializer(String email, String password, String firstName, String lastName) {
        return new AdminAccountInitializer(userRepository, passwordEncoder, email, password, firstName, lastName);
    }
}
