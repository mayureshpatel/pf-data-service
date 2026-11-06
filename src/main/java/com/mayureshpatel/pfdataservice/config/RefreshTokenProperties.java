package com.mayureshpatel.pfdataservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Refresh-token lifetimes and cookie settings bound from {@code app.session.refresh.*} (PF-899). A remember-me session
 * may idle for 14 days and live at most 60; a browser-session-only one idles 8 hours and lives at
 * most 24. The access-token lifetime stays {@code spring.security.jwt.expiration}.
 *
 * @param rememberMeIdle      idle window for a remember-me session
 * @param rememberMeAbsolute  maximum age of a remember-me session
 * @param sessionOnlyIdle     idle window for a session-only session
 * @param sessionOnlyAbsolute maximum age of a session-only session
 * @param cookieName          name of the refresh cookie
 * @param cookieSecure        whether the cookie is {@code Secure} (true in production, false for http dev)
 * @param cookieSameSite      the cookie {@code SameSite} attribute (Strict in production, Lax for local dev)
 */
@ConfigurationProperties(prefix = "app.session.refresh")
public record RefreshTokenProperties(
        @DefaultValue("14d") Duration rememberMeIdle,
        @DefaultValue("60d") Duration rememberMeAbsolute,
        @DefaultValue("8h") Duration sessionOnlyIdle,
        @DefaultValue("24h") Duration sessionOnlyAbsolute,
        @DefaultValue("pf_refresh") String cookieName,
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("Strict") String cookieSameSite
) {
}
