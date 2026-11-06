package com.mayureshpatel.pfdataservice.repository.user;

import com.mayureshpatel.pfdataservice.domain.user.RefreshToken;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.user.mapper.RefreshTokenRowMapper;
import com.mayureshpatel.pfdataservice.repository.user.query.RefreshTokenQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-backed persistence for {@link RefreshToken} (PF-899). Timestamps are always passed in by the
 * caller (the service's injected clock) rather than taken from the database, so expiry rules stay
 * testable with a fixed clock.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String PARAM_NOW = "now";
    private static final String PARAM_TOKEN_HASH = "tokenHash";
    private static final String PARAM_IDLE_EXPIRES_AT = "idleExpiresAt";

    private final JdbcClient jdbcClient;
    private final RefreshTokenRowMapper rowMapper;

    /**
     * Stores a new token; {@code created_at} and {@code last_used_at} are both set to {@code now}.
     *
     * @param token the token to store (its id, timestamps other than expiries, and revoked time are ignored)
     * @param now   the creation time
     * @return the generated id
     */
    public long insert(RefreshToken token, OffsetDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(RefreshTokenQueries.INSERT)
                .param(SqlParams.USER_ID, token.getUserId())
                .param(PARAM_TOKEN_HASH, token.getTokenHash())
                .param("rememberMe", token.isRememberMe())
                .param(PARAM_NOW, now)
                .param(PARAM_IDLE_EXPIRES_AT, token.getIdleExpiresAt())
                .param("absoluteExpiresAt", token.getAbsoluteExpiresAt())
                .update(keyHolder, "id");

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null || keys.get("id") == null) {
            throw new IllegalStateException("refresh token insert returned no id");
        }
        return ((Number) keys.get("id")).longValue();
    }

    /**
     * @param tokenHash the hex SHA-256 of the raw token
     * @return the token row, including revoked and expired ones, if the hash is known
     */
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jdbcClient.sql(RefreshTokenQueries.FIND_BY_TOKEN_HASH)
                .param(PARAM_TOKEN_HASH, tokenHash)
                .query(rowMapper)
                .optional();
    }

    /**
     * Records a use of the token and slides its idle expiry.
     *
     * @param id            the token id
     * @param now           the time of use
     * @param idleExpiresAt the new idle expiry
     * @return the number of rows updated (0 or 1)
     */
    public int touch(long id, OffsetDateTime now, OffsetDateTime idleExpiresAt) {
        return jdbcClient.sql(RefreshTokenQueries.TOUCH)
                .param("id", id)
                .param(PARAM_NOW, now)
                .param(PARAM_IDLE_EXPIRES_AT, idleExpiresAt)
                .update();
    }

    /**
     * Revokes one token if it is not already revoked (idempotent; the first revoke time is kept).
     *
     * @param tokenHash the hex SHA-256 of the raw token
     * @param now       the revoke time
     * @return the number of rows revoked (0 or 1)
     */
    public int revokeByTokenHash(String tokenHash, OffsetDateTime now) {
        return jdbcClient.sql(RefreshTokenQueries.REVOKE_BY_TOKEN_HASH)
                .param(PARAM_TOKEN_HASH, tokenHash)
                .param(PARAM_NOW, now)
                .update();
    }

    /**
     * Revokes every not-yet-revoked token of a user.
     *
     * @param userId the user id
     * @param now    the revoke time
     * @return the number of rows revoked
     */
    public int revokeAllByUserId(Long userId, OffsetDateTime now) {
        return jdbcClient.sql(RefreshTokenQueries.REVOKE_ALL_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_NOW, now)
                .update();
    }

    /**
     * Deletes tokens that were revoked, or whose idle or absolute expiry passed, before the cutoff.
     *
     * @param cutoff the moment before which a revoke or expiry must have happened
     * @return the number of rows deleted
     */
    public int deleteExpiredOrRevokedBefore(OffsetDateTime cutoff) {
        return jdbcClient.sql(RefreshTokenQueries.DELETE_EXPIRED_OR_REVOKED_BEFORE)
                .param("cutoff", cutoff)
                .update();
    }
}
