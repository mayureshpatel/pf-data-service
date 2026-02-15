package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.repository.transaction.model.Tag;
import com.mayureshpatel.pfdataservice.repository.user.model.User;
import com.mayureshpatel.pfdataservice.repository.RowMapperFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TagRowMapper extends RowMapperFactory implements RowMapper<Tag> {

    @Override
    public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
        Tag tag = new Tag();
        tag.setId(rs.getLong("id"));
        tag.setName(rs.getString("name"));
        tag.setColor(rs.getString("color"));

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            tag.setUser(user);
        }

        tag.setCreatedAt(getLocalDateTime(rs, "created_at"));
        tag.setUpdatedAt(getLocalDateTime(rs, "updated_at"));

        return tag;
    }
}
