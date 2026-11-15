package pl.routecost.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.routecost.expense.ExpenseRepository;
import pl.routecost.fuel.FuelEntryRepository;
import pl.routecost.reminder.ReminderRepository;
import pl.routecost.user.RefreshTokenRepository;
import pl.routecost.user.User;
import pl.routecost.user.UserRepository;
import pl.routecost.user.UserRole;
import pl.routecost.vehicle.VehicleRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final ExpenseRepository expenseRepository;
    private final ReminderRepository reminderRepository;

    public AdminService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            VehicleRepository vehicleRepository,
            FuelEntryRepository fuelEntryRepository,
            ExpenseRepository expenseRepository,
            ReminderRepository reminderRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelEntryRepository = fuelEntryRepository;
        this.expenseRepository = expenseRepository;
        this.reminderRepository = reminderRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> findUsers(String search, UserRole role, Boolean enabled, Pageable pageable) {
        List<Specification<User>> filters = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            filters.add((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("email")), pattern),
                    builder.like(builder.lower(root.get("firstName")), pattern),
                    builder.like(builder.lower(root.get("lastName")), pattern)
            ));
        }
        if (role != null) {
            filters.add((root, query, builder) -> builder.equal(root.get("role"), role));
        }
        if (enabled != null) {
            filters.add((root, query, builder) -> builder.equal(root.get("enabled"), enabled));
        }

        Specification<User> specification = Specification.allOf(filters);
        return userRepository.findAll(specification, pageable).map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse changeRole(Long actorId, Long targetId, UserRole role) {
        List<User> activeAdmins = lockActiveAdmins();
        User target = findTargetForUpdate(targetId);
        if (actorId.equals(targetId) && role != UserRole.ADMIN) {
            throw new AdminConflictException("Nie można odebrać sobie roli administratora.");
        }
        if (target.isEnabled() && target.getRole() == UserRole.ADMIN && role != UserRole.ADMIN
                && activeAdmins.size() <= 1) {
            throw new AdminConflictException("System musi mieć co najmniej jednego aktywnego administratora.");
        }
        target.changeRole(role);
        return AdminUserResponse.from(target);
    }

    @Transactional
    public AdminUserResponse changeStatus(Long actorId, Long targetId, boolean enabled) {
        List<User> activeAdmins = lockActiveAdmins();
        User target = findTargetForUpdate(targetId);
        if (actorId.equals(targetId) && !enabled) {
            throw new AdminConflictException("Nie można dezaktywować własnego konta.");
        }
        if (!enabled && target.isEnabled() && target.getRole() == UserRole.ADMIN && activeAdmins.size() <= 1) {
            throw new AdminConflictException("System musi mieć co najmniej jednego aktywnego administratora.");
        }
        target.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.revokeAllActiveForUser(targetId, Instant.now());
        }
        return AdminUserResponse.from(target);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        long totalUsers = userRepository.count();
        return new AdminDashboardResponse(
                totalUsers,
                userRepository.countByEnabled(true),
                userRepository.countByEnabled(false),
                userRepository.countByRole(UserRole.ADMIN),
                vehicleRepository.count(),
                fuelEntryRepository.count(),
                expenseRepository.count(),
                reminderRepository.count()
        );
    }

    private List<User> lockActiveAdmins() {
        return userRepository.findActiveByRoleForUpdate(UserRole.ADMIN);
    }

    private User findTargetForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AdminUserNotFoundException(userId));
    }
}
