package com.mayureshpatel.pfdataservice.repository.user.mapper;

import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class UserRowMapper extends JdbcMapperUtils implements RowMapper<User> {

    @Override
    public User mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to a User object with a custom prefix.
     *
     * @param rs     ResultSet containing the row data
     * @param prefix Prefix to use for column names
     * @return User object populated with data from the ResultSet
     * @throws SQLException if there is an error accessing the ResultSet
     */
    public static User mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        User.UserBuilder builder = User.builder();
        if (hasColumn(safePrefix + "id", availableColumns)) {
            Long id = getLongOrNull(rs, safePrefix + "id");
            if (id == null) {
                return null;
            }
            builder.id(id);
        } else {
            return null;
        }

        if (hasColumn(safePrefix + "username", availableColumns)) {
            builder.username(rs.getString(safePrefix + "username"));
        }
        if (hasColumn(safePrefix + "password_hash", availableColumns)) {
            builder.passwordHash(rs.getString(safePrefix + "password_hash"));
        }
        if (hasColumn(safePrefix + "email", availableColumns)) {
            builder.email(rs.getString(safePrefix + "email"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
