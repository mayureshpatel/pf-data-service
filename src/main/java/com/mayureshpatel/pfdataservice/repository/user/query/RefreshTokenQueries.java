package com.mayureshpatel.pfdataservice.repository.user.query;

import lombok.NoArgsConstructor;

/**
 * SQL query constants for {@code RefreshTokenRepository}. Every write that targets "live" tokens
 * guards on {@code revoked_at is null} so revoking stays idempotent and keeps the first revoke time.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RefreshTokenQueries {

    // language=SQL
    public static final String INSERT = """
            insert into refresh_tokens (user_id, token_hash, remember_me, created_at, last_used_at,
                                        idle_expires_at, absolute_expires_at)
            values (:userId, :tokenHash, :rememberMe, :now, :now, :idleExpiresAt, :absoluteExpiresAt)
            """;

    // language=SQL
    public static final String FIND_BY_TOKEN_HASH = """
            select *
            from refresh_tokens
            where token_hash = :tokenHash
            """;

    // language=SQL
    public static final String TOUCH = """
            update refresh_tokens
            set last_used_at = :now,
                idle_expires_at = :idleExpiresAt
            where id = :id
            """;

    // language=SQL
    public static final String REVOKE_BY_TOKEN_HASH = """
            update refresh_tokens
            set revoked_at = :now
            where token_hash = :tokenHash
              and revoked_at is null
            """;

    // language=SQL
    public static final String REVOKE_ALL_BY_USER_ID = """
            update refresh_tokens
            set revoked_at = :now
            where user_id = :userId
              and revoked_at is null
            """;

    // language=SQL
    public static final String DELETE_EXPIRED_OR_REVOKED_BEFORE = """
            delete from refresh_tokens
            where revoked_at < :cutoff
               or idle_expires_at < :cutoff
               or absolute_expires_at < :cutoff
            """;
}
