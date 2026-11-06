package com.mayureshpatel.pfdataservice.domain.user;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * The server-side record of one session's refresh token (PF-899). Holds only the SHA-256 hash of the
 * token -- the raw token exists solely in the user's HttpOnly cookie -- plus the expiry bookkeeping
 * that implements the idle timeout (slides on use) and the absolute maximum session age.
 */
@Getter
@Builder(toBuilder = true)
@ToString(exclude = "tokenHash")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshToken {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private String tokenHash;
    private boolean rememberMe;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime idleExpiresAt;
    private OffsetDateTime absoluteExpiresAt;
    private OffsetDateTime revokedAt;
}
