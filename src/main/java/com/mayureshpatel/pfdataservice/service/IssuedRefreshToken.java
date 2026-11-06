package com.mayureshpatel.pfdataservice.service;

/**
 * The result of issuing a refresh token (PF-899): the RAW token, which exists only here and in the
 * user's cookie, and what the cookie needs to be set correctly.
 *
 * @param rawToken      the URL-safe base64 token to put in the cookie; never stored or logged
 * @param rememberMe    whether this is a remember-me session
 * @param maxAgeSeconds the cookie {@code Max-Age} for a remember-me session (time until its absolute
 *                      expiry), or {@code null} for a session-only one, whose cookie has no {@code Max-Age}
 */
public record IssuedRefreshToken(String rawToken, boolean rememberMe, Long maxAgeSeconds) {

    @Override
    public String toString() {
        return "IssuedRefreshToken[rememberMe=" + rememberMe + ", maxAgeSeconds=" + maxAgeSeconds + "]";
    }
}
