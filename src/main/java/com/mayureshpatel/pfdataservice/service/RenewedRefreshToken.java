package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.user.User;

/**
 * The result of successfully renewing a refresh token (PF-899): the token's user and what the
 * controller needs to re-send the cookie with a slid expiry.
 *
 * @param user          the user the token belongs to, freshly loaded
 * @param rememberMe    whether the session is a remember-me one
 * @param maxAgeSeconds the new cookie {@code Max-Age} for a remember-me session, or {@code null} for a session-only one
 */
public record RenewedRefreshToken(User user, boolean rememberMe, Long maxAgeSeconds) {
}
