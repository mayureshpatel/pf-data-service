package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryRuleRowMapper extends JdbcMapperUtils implements RowMapper<CategoryRule> {

    @Override
    public CategoryRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        // user
        User user = User.builder()
                .id(rs.getLong("user_id"))
                .build();

        // category
        Category category = Category.builder()
                .id(rs.getLong("category_id"))
                .userId(rs.getLong("user_id"))
                .name(rs.getString("category_name"))
                .color(rs.getString("category_color"))
                .icon(rs.getString("category_icon"))
                .build();

        return CategoryRule.builder()
                .id(rs.getLong("id"))
                .keyword(rs.getString("keyword"))
                .priority(rs.getInt("priority"))
                .category(category)
                .user(user)
                .audit(getAuditColumns(rs))
                .build();
    }
}
