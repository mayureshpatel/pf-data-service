package com.mayureshpatel.pfdataservice.repository.user.mapper;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
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
        user.setAudit(new TableAudit());
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));

        String lastUpdatedBy = rs.getString("last_updated_by");
        if (lastUpdatedBy != null) {
            if (user.getAudit().getUpdatedBy() == null) {
                user.getAudit().setUpdatedBy(new User());
            }
            user.getAudit().getUpdatedBy().setUsername(lastUpdatedBy);
        }

        Timestamp lastUpdatedTimestamp =  rs.getTimestamp("last_updated_timestamp");
        if (lastUpdatedTimestamp != null) {
            user.getAudit().setUpdatedAt(lastUpdatedTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        Timestamp createdTimestamp = rs.getTimestamp("created_at");
        if (createdTimestamp != null) {
            user.getAudit().setCreatedAt(createdTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        Timestamp deletedTimestamp = rs.getTimestamp("deleted_at");
        if (deletedTimestamp != null) {
            user.getAudit().setDeletedAt(deletedTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        return user;
    }
}
