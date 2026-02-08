package com.mayureshpatel.pfdataservice.jdbc.mapper;

import com.mayureshpatel.pfdataservice.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;

@Component
public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs , int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));

        Long lastUpdatedBy = rs.getLong("last_updated_by");
        if (!rs.wasNull()) {
            user.setLastUpdatedBy(lastUpdatedBy);
        }

        Timestamp lastUpdatedTimestamp =  rs.getTimestamp("last_updated_timestamp");
        if (lastUpdatedTimestamp != null) {
            user.setLastUpdatedTimestamp(lastUpdatedTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        Timestamp createdTimestamp = rs.getTimestamp("created_at");
        if (createdTimestamp != null) {
            user.setCreatedTimestamp(createdTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        Timestamp deletedTimestamp = rs.getTimestamp("deleted_at");
        if (deletedTimestamp != null) {
            user.setDeletedAt(deletedTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        return user;
    }
}
