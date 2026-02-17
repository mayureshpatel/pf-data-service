package com.mayureshpatel.pfdataservice.repository.tag.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TagRowMapper extends JdbcMapperUtils implements RowMapper<Tag> {

    @Override
    public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
        Tag tag = new Tag();
        tag.setId(rs.getLong("id"));
        tag.setName(rs.getString("name"));
        tag.getIconography().setColor(rs.getString("color"));

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            tag.setUser(user);
        }

        tag.getAudit().setCreatedAt(getOffsetDateTime(rs, "created_at"));
        tag.getAudit().setUpdatedAt(getOffsetDateTime(rs, "updated_at"));

        return tag;
    }
}
