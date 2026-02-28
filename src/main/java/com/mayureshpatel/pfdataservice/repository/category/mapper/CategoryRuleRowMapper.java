package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.category.Category;
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
        categoryRule.setPriority(rs.getInt("priority"));

        Category category = new Category();
        category.setId(rs.getLong("category_id"));
        category.setName(rs.getString("category_name"));

        Iconography categoryIconography = new Iconography();
        categoryIconography.setColor(rs.getString("category_color"));
        categoryIconography.setIcon(rs.getString("category_icon"));

        category.setIconography(categoryIconography);
        categoryRule.setCategory(category);

        // set user object
        User user = new User();
        user.setId(rs.getLong("user_id"));

        categoryRule.setUser(user);

        // set audit columns
        categoryRule.setAudit(new TimestampAudit());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            categoryRule.getAudit().setCreatedAt(createdAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            categoryRule.getAudit().setUpdatedAt(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        return categoryRule;
    }
}
