package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.repository.category.model.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.user.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;

@Component
public class CategoryRuleRowMapper implements RowMapper<CategoryRule> {

    @Override
    public CategoryRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        CategoryRule categoryRule = new CategoryRule();
        categoryRule.setId(rs.getLong("id"));
        categoryRule.setKeyword(rs.getString("keyword"));
        categoryRule.setCategoryName(rs.getString("category_name"));
        categoryRule.setPriority(rs.getInt("priority"));

        // set user object
        User user = new User();
        user.setId(rs.getLong("user_id"));

        categoryRule.setUser(user);

        // set audit columns
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            categoryRule.setCreatedAt(createdAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            categoryRule.setUpdatedAt(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        return categoryRule;
    }
}
