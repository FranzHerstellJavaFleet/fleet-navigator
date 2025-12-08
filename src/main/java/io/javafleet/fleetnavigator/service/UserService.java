package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.User;
import io.javafleet.fleetnavigator.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for user management and authentication.
 * Implements UserDetailsService for Spring Security integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Initialize default admin user on startup
     */
    @PostConstruct
    @Transactional
    public void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setDisplayName("Administrator");
            admin.setEmail("admin@localhost");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("👤 Default admin user created (username: admin, password: admin)");
            log.warn("⚠️  WICHTIG: Bitte ändern Sie das Admin-Passwort nach dem ersten Login!");
        } else {
            log.debug("Admin user already exists");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Get currently authenticated user
     */
    public Optional<User> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        } else if (principal instanceof String username) {
            return userRepository.findByUsername(username);
        }

        return Optional.empty();
    }

    /**
     * Get current user ID (for filtering data)
     */
    public Long getCurrentUserId() {
        return getCurrentUser().map(User::getId).orElse(null);
    }

    /**
     * Register a new user
     */
    @Transactional
    public User registerUser(String username, String password, String email, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Benutzername bereits vergeben: " + username);
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-Mail bereits registriert: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setDisplayName(displayName != null ? displayName : username);
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        log.info("👤 New user registered: {} (ID: {})", username, saved.getId());
        return saved;
    }

    /**
     * Update last login timestamp
     */
    @Transactional
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Falsches aktuelles Passwort");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("🔐 Password changed for user: {}", user.getUsername());
    }

    /**
     * Get all users (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Delete user (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Letzter Admin-User kann nicht gelöscht werden");
            }
        }

        userRepository.delete(user);
        log.info("🗑️ User deleted: {}", user.getUsername());
    }
}
