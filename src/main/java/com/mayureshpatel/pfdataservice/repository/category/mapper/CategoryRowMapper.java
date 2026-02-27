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

        long parentId = rs.getLong("parent_id");
        if (!rs.wasNull()) {
            CategoryDto parentCategory = new CategoryDto();
            parentCategory.setId(parentId);
            try {
                parentCategory.setName(rs.getString("parent_name"));
                String parentType = rs.getString("parent_type");
                if (parentType != null) {
                    parentCategory.setType(CategoryType.valueOf(parentType));
                }
                Iconography parentIconography = new Iconography();
                parentIconography.setColor(rs.getString("parent_color"));
                parentIconography.setIcon(rs.getString("parent_icon"));
                parentCategory.setIconography(parentIconography);
            } catch (SQLException ignored) {
                // p_* columns not present in queries without the parent join
            }
            category.setParent(parentCategory);
        }

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
