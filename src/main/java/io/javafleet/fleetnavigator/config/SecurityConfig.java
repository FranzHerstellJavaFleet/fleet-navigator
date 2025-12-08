package io.javafleet.fleetnavigator.config;

import io.javafleet.fleetnavigator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security configuration for Fleet Navigator.
 * Implements session-based authentication with user isolation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Lazy
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection with cookie (for SPA)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/api/auth/login",        // Login (form submission)
                    "/api/auth/register",     // Registration
                    "/api/auth/logout",       // Logout
                    "/api/auth/check",        // Auth check (GET)
                    "/api/fleet-mate/ws/**",  // WebSocket
                    "/api/chat/**",           // Chat API (all operations including delete)
                    "/api/fleetcode/**",      // FleetCode API
                    "/api/fleet-mate/**",     // Fleet Mate commands (ping, execute, etc.)
                    "/api/llm/providers/**",  // LLM Provider Management API
                    "/api/experts/**",        // Experten-System (CRUD + Avatar)
                    "/api/settings/**",       // Settings API
                    "/api/search/**",         // Search Settings API
                    "/api/files/**",          // File Upload API
                    "/api/projects/**",       // Project API (includes chat assignment)
                    "/api/system-prompts/**", // System Prompt API
                    "/api/models/**",         // Model API (includes setting default)
                    "/api/model-store/**"     // Model Store API (downloads)
                )
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/auth/**",           // Login, Register, Logout
                    "/api/system/version",    // Version check for cache
                    "/api/system/setup-status", // Setup wizard check
                    "/api/system/ai-startup-status", // AI startup status for loading overlay
                    "/api/llm/providers/llama-server/health", // llama-server health check
                    "/api/fleet-mate/ws/**",  // WebSocket (has own auth)
                    "/login",                 // Login page
                    "/register",              // Register page
                    "/",                      // Root
                    "/index.html",
                    "/assets/**",             // Static assets
                    "/favicon.ico",
                    "/*.js",
                    "/*.css"
                ).permitAll()

                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // Frontend routes - let Vue Router handle (but require auth)
                .anyRequest().authenticated()
            )

            // Form login configuration
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/api/auth/login")
                .successHandler(authenticationSuccessHandler())
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    // Generische Fehlermeldung - keine Details preisgeben
                    response.getWriter().write("{\"error\":\"Benutzername oder Passwort falsch\"}");
                })
                .permitAll()
            )

            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Erfolgreich abgemeldet\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Session management
            .sessionManagement(session -> session
                .maximumSessions(3)  // Max 3 sessions per user
                .expiredUrl("/login?expired")
            )

            // Exception handling - redirect to login for unauthenticated API requests
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestUri = request.getRequestURI();
                    if (requestUri.startsWith("/api/")) {
                        // API request - return 401 JSON
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Nicht authentifiziert\",\"loginRequired\":true}");
                    } else {
                        // Page request - redirect to login
                        response.sendRedirect("/login");
                    }
                })
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            // Update last login
            userService.updateLastLogin(authentication.getName());

            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Erfolgreich angemeldet\",\"username\":\"" +
                    authentication.getName() + "\"}");
        };
    }
}
