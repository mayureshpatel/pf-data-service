package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.CategoryDto;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryRowMapper implements RowMapper<CategoryDto> {

    @Override
    public CategoryDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        CategoryDto category = new CategoryDto();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));

        category.setIconography(new Iconography());
        category.getIconography().setColor(rs.getString("color"));
        category.getIconography().setIcon(rs.getString("icon"));

        try {
            String type = rs.getString("type");
            if (type != null) {
                category.setType(CategoryType.valueOf(type));
            }
        } catch (Exception e) {
            category.setType(null);
        }

        Long userId = JdbcMapperUtils.getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            category.setUser(user);
        }

        category.setAudit(new TableAudit());
        category.getAudit().setCreatedAt(JdbcMapperUtils.getOffsetDateTime(rs, "created_at"));
        category.getAudit().setUpdatedAt(JdbcMapperUtils.getOffsetDateTime(rs, "updated_at"));

        return category;
    }
}
