package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryRowMapper implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.getIconography().setColor(rs.getString("color"));
        category.getIconography().setIcon(rs.getString("icon"));

        String type = rs.getString("type");
        if (type != null) {
            category.setType(CategoryType.valueOf(type));
        }

        Long userId = JdbcMapperUtils.getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            category.setUser(user);
        }

        category.getAudit().setCreatedAt(JdbcMapperUtils.getOffsetDateTime(rs, "created_at"));
        category.getAudit().setUpdatedAt(JdbcMapperUtils.getOffsetDateTime(rs, "updated_at"));

        return category;
    }
}
