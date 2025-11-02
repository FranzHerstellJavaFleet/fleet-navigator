package io.javafleet.fleetnavigator.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine for high-performance caching.
 * Inspired by local-llm-demo-full project.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with TTL and size limits.
     *
     * Cache Names:
     * - "modelResponses": Caches LLM responses to avoid redundant calls
     * - "modelSelection": Caches model selection decisions
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "modelResponses",
            "modelSelection"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500) // Max 500 entries
            .expireAfterWrite(30, TimeUnit.MINUTES) // Cache for 30 minutes
            .recordStats()); // Enable statistics for monitoring

        return cacheManager;
    }
}
