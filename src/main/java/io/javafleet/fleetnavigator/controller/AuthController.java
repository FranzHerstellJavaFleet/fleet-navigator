package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.model.User;
import io.javafleet.fleetnavigator.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for authentication operations.
 * Handles login, logout, registration, and user info.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    /**
     * GET /api/auth/me - Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        return userService.getCurrentUser()
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "role", user.getRole().name(),
                        "authenticated", true
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "authenticated", false
                )));
    }

    /**
     * POST /api/auth/register - Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Validation
            if (request.username == null || request.username.length() < 3) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Benutzername muss mindestens 3 Zeichen haben"
                ));
            }
            if (request.password == null || request.password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Passwort muss mindestens 8 Zeichen haben"
                ));
            }

            User user = userService.registerUser(
                    request.username,
                    request.password,
                    request.email,
                    request.displayName
            );

            log.info("New user registered: {}", user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "Registrierung erfolgreich",
                    "username", user.getUsername()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Registrierung fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/auth/change-password - Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            Long userId = userService.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Nicht angemeldet"
                ));
            }

            userService.changePassword(userId, request.oldPassword, request.newPassword);

            return ResponseEntity.ok(Map.of(
                    "message", "Passwort erfolgreich geändert"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/auth/check - Check if user is authenticated (for frontend)
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());

        if (isAuthenticated) {
            return userService.getCurrentUser()
                    .map(user -> ResponseEntity.ok(Map.of(
                            "authenticated", true,
                            "username", user.getUsername(),
                            "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                            "role", user.getRole().name()
                    )))
                    .orElse(ResponseEntity.ok(Map.of("authenticated", false)));
        }

        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    // ===== Request DTOs =====

    public record RegisterRequest(
            String username,
            String password,
            String email,
            String displayName
    ) {}

    public record ChangePasswordRequest(
            String oldPassword,
            String newPassword
    ) {}
}
