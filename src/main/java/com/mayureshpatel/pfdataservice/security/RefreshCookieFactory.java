package com.mayureshpatel.pfdataservice.security;

import com.mayureshpatel.pfdataservice.config.RefreshTokenProperties;
import com.mayureshpatel.pfdataservice.service.IssuedRefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the refresh cookie (PF-899). It is always {@code HttpOnly} with {@code Path=/api/v1/auth}, so page
 * scripts cannot read it and the browser sends it only to the auth endpoints; {@code Secure} and
 * {@code SameSite} come from configuration and {@code Max-Age} is set only for remember-me sessions (a
 * session cookie otherwise, which ends when the browser closes).
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    /** the only path the browser sends the cookie to. */
    public static final String COOKIE_PATH = "/api/v1/auth";

    private final RefreshTokenProperties properties;

    /**
     * @param issued the refresh token to put in the cookie
     * @return the cookie carrying the raw token
     */
    public ResponseCookie create(IssuedRefreshToken issued) {
        ResponseCookie.ResponseCookieBuilder builder = base(issued.rawToken());
        if (issued.maxAgeSeconds() != null) {
            builder.maxAge(Duration.ofSeconds(issued.maxAgeSeconds()));
        }
        return builder.build();
    }

    /**
     * @return an immediately-expired cookie that makes the browser delete the refresh cookie
     */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(COOKIE_PATH);
    }
}
