package com.mayureshpatel.pfdataservice.repository.user.mapper;

import com.mayureshpatel.pfdataservice.domain.user.RefreshToken;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps a {@code refresh_tokens} row directly to a {@link RefreshToken}; the table has no audit columns. */
@Component
public class RefreshTokenRowMapper extends JdbcMapperUtils implements RowMapper<RefreshToken> {

    @Override
    public RefreshToken mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return RefreshToken.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .tokenHash(rs.getString("token_hash"))
                .rememberMe(rs.getBoolean("remember_me"))
                .createdAt(getOffsetDateTime(rs, "created_at"))
                .lastUsedAt(getOffsetDateTime(rs, "last_used_at"))
                .idleExpiresAt(getOffsetDateTime(rs, "idle_expires_at"))
                .absoluteExpiresAt(getOffsetDateTime(rs, "absolute_expires_at"))
                .revokedAt(getOffsetDateTime(rs, "revoked_at"))
                .build();
    }
}
