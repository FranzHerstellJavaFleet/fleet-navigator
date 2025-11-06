package io.javafleet.fleetnavigator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration including CORS settings and SPA routing
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:2025", "http://localhost:3000", "http://localhost:5173", "http://localhost:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Forward all non-API routes to index.html for Vue Router to handle
     * This enables SPA routing to work with direct URL access
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward all routes that don't start with /api to index.html
        registry.addViewController("/agents/**").setViewName("forward:/index.html");
    }
}
