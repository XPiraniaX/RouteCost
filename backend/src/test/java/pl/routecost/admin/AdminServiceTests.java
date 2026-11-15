package pl.routecost.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.routecost.expense.ExpenseRepository;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.reminder.ReminderRepository;
import pl.routecost.user.RefreshTokenRepository;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.user.UserRole;
import pl.routecost.vehicle.VehicleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final AdminService adminService = new AdminService(
            userRepository,
            refreshTokenRepository,
            mock(VehicleRepository.class),
            mock(FuelEntryRepository.class),
            mock(ExpenseRepository.class),
            mock(ReminderRepository.class)
    );

    private User lastActiveAdmin;

    @BeforeEach
    void setUp() {
        lastActiveAdmin = new User("last-admin@example.test", "hash", "Last", "Admin", UserRole.ADMIN);
        when(userRepository.findActiveByRoleForUpdate(UserRole.ADMIN)).thenReturn(List.of(lastActiveAdmin));
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(lastActiveAdmin));
    }

    @Test
    void refusesDemotingTheLastActiveAdmin() {
        assertThrows(AdminConflictException.class,
                () -> adminService.changeRole(1L, 99L, UserRole.USER));
        assertEquals(UserRole.ADMIN, lastActiveAdmin.getRole());
        verify(userRepository).findActiveByRoleForUpdate(UserRole.ADMIN);
    }

    @Test
    void refusesDeactivatingTheLastActiveAdmin() {
        assertThrows(AdminConflictException.class,
                () -> adminService.changeStatus(1L, 99L, false));
        assertEquals(true, lastActiveAdmin.isEnabled());
        verify(userRepository).findActiveByRoleForUpdate(UserRole.ADMIN);
    }
}
