package com.mayureshpatel.pfdataservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Spring's caching abstraction, backed by the Caffeine dependency already on
 * the classpath (previously unused outside {@code RateLimitingFilter}).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache name for the active-currencies list (PF-321). No eviction is registered for this
     * cache -- CurrencyController exposes no create/update/delete endpoint, confirmed directly
     * rather than assumed, so the only staleness risk is a future schema/seed change, which the
     * 24-hour TTL bounds.
     */
    public static final String CURRENCIES_CACHE = "currencies";

    /**
     * Configures the Caffeine-backed {@link CacheManager}. Each named cache gets its own TTL,
     * registered individually rather than one blanket policy, since different cached resources
     * have different staleness tolerances. {@code recordStats()} is on for every registered
     * cache: this app already exposes Micrometer/actuator cache metrics for whatever
     * CacheManager is registered, so real hit/miss numbers cost nothing extra to make visible.
     *
     * @return the configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(CURRENCIES_CACHE,
                Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).recordStats().build());
        return cacheManager;
    }
}
