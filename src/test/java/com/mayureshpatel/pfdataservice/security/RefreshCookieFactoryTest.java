package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.config.RefreshTokenProperties;
import com.mayureshpatel.pfdataservice.service.IssuedRefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies {@link RefreshCookieFactory} attributes for the production (Secure, Strict) and local-dev (not Secure, Lax) configurations. */
@DisplayName("RefreshCookieFactory Tests")
class RefreshCookieFactoryTest {

    private static RefreshCookieFactory factory(boolean secure, String sameSite) {
        return new RefreshCookieFactory(new RefreshTokenProperties(Duration.ofDays(14), Duration.ofDays(60),
                Duration.ofHours(8), Duration.ofHours(24), "pf_refresh", secure, sameSite));
    }

    @Test
    @DisplayName("should build a Secure, Strict, HttpOnly cookie with Max-Age for remember-me")
    void productionRememberMe() {
        // act
        String cookie = factory(true, "Strict").create(new IssuedRefreshToken("raw", true, 100L)).toString();

        // assert & verify
        assertThat(cookie).startsWith("pf_refresh=raw")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=100");
    }

    @Test
    @DisplayName("should build a non-Secure Lax session cookie without Max-Age for local development")
    void localSessionOnly() {
        // act
        String cookie = factory(false, "Lax").create(new IssuedRefreshToken("raw", false, null)).toString();

        // assert & verify
        assertThat(cookie).contains("HttpOnly", "SameSite=Lax").doesNotContain("Secure", "Max-Age");
    }

    @Test
    @DisplayName("should build an empty, immediately expired cookie on the same path when clearing")
    void clear() {
        // act
        ResponseCookie cookie = factory(true, "Strict").clear();

        // assert & verify
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
