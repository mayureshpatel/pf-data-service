package com.mayureshpatel.pfdataservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter for authentication endpoints.
 * Limits requests per IP address to prevent brute-force attacks.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public RateLimitingFilter() {
        // Periodically clean up old buckets to prevent memory leak
        cleanupExecutor.scheduleAtFixedRate(this::cleanupBuckets, 1, 1, TimeUnit.HOURS);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate limit authentication and registration endpoints
        if (path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIp(request);
            TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket());

            if (!bucket.tryConsume()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many authentication attempts. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void cleanupBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccessed > TimeUnit.MINUTES.toMillis(5));
    }

    private static class TokenBucket {
        private final AtomicInteger tokens = new AtomicInteger(MAX_REQUESTS_PER_MINUTE);
        private long lastRefill = System.currentTimeMillis();
        private long lastAccessed = System.currentTimeMillis();

        public synchronized boolean tryConsume() {
            refill();
            lastAccessed = System.currentTimeMillis();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefill > TimeUnit.MINUTES.toMillis(1)) {
                tokens.set(MAX_REQUESTS_PER_MINUTE);
                lastRefill = now;
            }
        }
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdown();
        super.destroy();
    }
}
