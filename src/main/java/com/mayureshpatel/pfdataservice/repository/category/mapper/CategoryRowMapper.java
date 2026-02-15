package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryRowMapper extends JdbcMapperUtils implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setColor(rs.getString("color"));
        category.setIcon(rs.getString("icon"));
        
        String type = rs.getString("type");
        if (type != null) {
            category.setType(CategoryType.valueOf(type));
        }

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            category.setUser(user);
        }

        Long parentId = getLongOrNull(rs, "parent_id");
        if (parentId != null) {
            Category parent = new Category();
            parent.setId(parentId);
            category.setParent(parent);
        }

        category.setCreatedAt(getLocalDateTime(rs, "created_at"));
        category.setUpdatedAt(getLocalDateTime(rs, "updated_at"));

        return category;
    }
}
