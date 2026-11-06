package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.config.RefreshTokenProperties;
import com.mayureshpatel.pfdataservice.domain.user.RefreshToken;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.user.RefreshTokenRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Issues, renews and revokes the refresh tokens that keep an active user signed in (PF-899). The raw
 * token is a random 256-bit value handed out once; only its SHA-256 hash is stored. Tokens are NOT
 * rotated: the same raw token stays valid, its idle expiry slides on each renewal (never past the
 * absolute expiry), and it ends on idle or absolute expiry or on sign-out. Time comes from the
 * injected {@link Clock}. The cookie {@code Max-Age} of a remember-me session is the time to its idle
 * expiry, so it slides with each renewal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration PURGE_GRACE = Duration.ofDays(1);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a session for a user: generates a random token, stores its hash with the expiries for
     * the chosen mode, and returns the raw token for the cookie.
     *
     * @param user       the signed-in user
     * @param rememberMe whether to use the long remember-me windows
     * @return the raw token and cookie metadata
     */
    public IssuedRefreshToken issue(User user, boolean rememberMe) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        OffsetDateTime now = now();
        OffsetDateTime absolute = now.plus(rememberMe ? properties.rememberMeAbsolute() : properties.sessionOnlyAbsolute());
        OffsetDateTime idle = slidIdleExpiry(now, rememberMe, absolute);

        refreshTokenRepository.insert(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .rememberMe(rememberMe)
                .idleExpiresAt(idle)
                .absoluteExpiresAt(absolute)
                .build(), now);

        return new IssuedRefreshToken(rawToken, rememberMe, cookieMaxAge(now, rememberMe, idle));
    }

    /**
     * Validates a presented token and, when valid, slides its idle expiry (no rotation).
     *
     * @param rawToken the raw token from the cookie; may be null or blank
     * @return the token's user plus the cookie facts (remember-me flag and a fresh sliding {@code Max-Age}), or empty when the token is unknown, revoked, idle- or absolute-expired,
     *         or its user no longer exists
     */
    public Optional<RenewedRefreshToken> renew(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        OffsetDateTime now = now();
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash(rawToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = found.get();
        boolean live = token.getRevokedAt() == null
                && now.isBefore(token.getIdleExpiresAt())
                && now.isBefore(token.getAbsoluteExpiresAt());
        if (!live) {
            log.debug("refresh token {} rejected (revoked or expired)", token.getId());
            return Optional.empty();
        }

        Optional<User> user = userRepository.findById(token.getUserId());
        if (user.isEmpty()) {
            return Optional.empty();
        }

        OffsetDateTime newIdle = slidIdleExpiry(now, token.isRememberMe(), token.getAbsoluteExpiresAt());
        refreshTokenRepository.touch(token.getId(), now, newIdle);
        return Optional.of(new RenewedRefreshToken(user.get(), token.isRememberMe(),
                cookieMaxAge(now, token.isRememberMe(), newIdle)));
    }

    /**
     * Ends a session on the server. Idempotent: an unknown, blank or already-revoked token is a no-op.
     *
     * @param rawToken the raw token from the cookie; may be null or blank
     */
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.revokeByTokenHash(hash(rawToken), now());
    }

    /**
     * Deletes tokens that expired or were revoked more than a day ago; runs daily on a configurable cron
     * ({@code app.session.refresh.purge-cron}, default 03:30).
     *
     * @return the number of rows deleted
     */
    @Scheduled(cron = "${app.session.refresh.purge-cron:0 30 3 * * *}")
    public int purgeExpired() {
        int deleted = refreshTokenRepository.deleteExpiredOrRevokedBefore(now().minus(PURGE_GRACE));
        log.info("purged {} expired or revoked refresh tokens", deleted);
        return deleted;
    }

    // the cookie lives as long as the server would keep an idle session, so the browser drops it together with the row
    private Long cookieMaxAge(OffsetDateTime now, boolean rememberMe, OffsetDateTime idleExpiry) {
        return rememberMe ? Duration.between(now, idleExpiry).toSeconds() : null;
    }

    private OffsetDateTime slidIdleExpiry(OffsetDateTime now, boolean rememberMe, OffsetDateTime absolute) {
        OffsetDateTime idle = now.plus(rememberMe ? properties.rememberMeIdle() : properties.sessionOnlyIdle());
        return idle.isAfter(absolute) ? absolute : idle;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    /**
     * @param rawToken the raw token
     * @return its lowercase hex SHA-256
     */
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
