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
     * Cache name for the active-account-types list (PF-322). Unlike currencies, this resource has
     * real {@code @PostMapping}/{@code @DeleteMapping} write endpoints -- {@code @CacheEvict} on
     * both is the primary correctness mechanism, so a fresh write is always visible immediately
     * on the instance that served it. The 1-hour TTL here is a deliberately short backstop, not a
     * copy of currencies' 24h: Caffeine's cache is per-JVM-instance, so in a multi-instance
     * deployment a write on one instance can't evict another instance's copy -- the TTL bounds
     * how long that specific, narrower staleness risk can persist, independent of the eviction
     * path this ticket's own AC is primarily about.
     */
    public static final String ACCOUNT_TYPES_CACHE = "accountTypes";

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
        cacheManager.registerCustomCache(ACCOUNT_TYPES_CACHE,
                Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).recordStats().build());
        return cacheManager;
    }
}
