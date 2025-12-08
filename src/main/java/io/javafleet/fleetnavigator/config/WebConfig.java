package io.javafleet.fleetnavigator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * Web configuration including CORS settings, SPA routing, and cache control
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
     * Configure cache control for static resources
     * - index.html: no-cache (always revalidate to detect new versions)
     * - JS/CSS with hash: long cache (Vite adds hash, safe to cache)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // index.html - always revalidate (no-cache, but still allows conditional caching)
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());

        // JS, CSS, assets with content hash - cache for 1 year (Vite adds hash to filenames)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));

        // Other static resources - short cache
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
    }

    /**
     * Forward all non-API routes to index.html for Vue Router to handle
     * This enables SPA routing to work with direct URL access
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward all routes that don't start with /api to index.html
        registry.addViewController("/agents/**").setViewName("forward:/index.html");
        registry.addViewController("/experts").setViewName("forward:/index.html");
        registry.addViewController("/experts/**").setViewName("forward:/index.html");
    }
}
