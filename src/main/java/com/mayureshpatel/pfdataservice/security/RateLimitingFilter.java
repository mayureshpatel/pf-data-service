package com.mayureshpatel.pfdataservice.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter for authentication endpoints.
 * Limits requests per IP address, per endpoint, to prevent brute-force attacks and registration
 * abuse -- each endpoint under {@code /api/v1/auth/} gets its own independent bucket per IP, so
 * traffic against one (e.g. login attempts) can't consume another's (e.g. registration) budget.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int MAX_REGISTER_REQUESTS_PER_MINUTE = 5;
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // only rate limit authentication and registration endpoints
        if (path.startsWith("/api/v1/auth/")) {
            String bucketKey = getClientIp(request) + ":" + path;
            int limit = REGISTER_PATH.equals(path) ? MAX_REGISTER_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
            TokenBucket bucket = buckets.get(bucketKey, k -> new TokenBucket(limit));

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

    /**
     * A per-key request counter that starts full and decrements on each consume. Refill is a
     * single reset back to {@code maxRequests}, applied the next time a full minute has elapsed
     * since the last refill -- not a smooth, continuously-draining refill.
     */
    private static class TokenBucket {
        private final int maxRequests;
        private final AtomicInteger tokens;
        private long lastRefill = System.currentTimeMillis();

        TokenBucket(int maxRequests) {
            this.maxRequests = maxRequests;
            this.tokens = new AtomicInteger(maxRequests);
        }

        /**
         * Refills the bucket if a minute has elapsed, then attempts to take one token.
         *
         * @return true if a token was available and consumed; false if the bucket is empty and
         *     the caller should be rate-limited
         */
        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefill > TimeUnit.MINUTES.toMillis(1)) {
                tokens.set(maxRequests);
                lastRefill = now;
            }
        }
    }
}
