package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

@Component
public class CategoryRowMapper implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));

        long parentId = rs.getLong("parent_id");
        if (!rs.wasNull()) {
            Category parentCategory = new Category();
            parentCategory.setId(parentId);
            if (hasColumn(rs, "parent_name")) {
                parentCategory.setName(rs.getString("parent_name"));
                String parentType = rs.getString("parent_type");
                if (parentType != null) {
                    parentCategory.setType(CategoryType.valueOf(parentType));
                }
                Iconography parentIconography = new Iconography();
                parentIconography.setColor(rs.getString("parent_color"));
                parentIconography.setIcon(rs.getString("parent_icon"));
                parentCategory.setIconography(parentIconography);
            }
            category.setParent(parentCategory);
        }

        category.setIconography(new Iconography());
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

        category.setAudit(new TimestampAudit());
        category.getAudit().setCreatedAt(JdbcMapperUtils.getOffsetDateTime(rs, "created_at"));
        category.getAudit().setUpdatedAt(JdbcMapperUtils.getOffsetDateTime(rs, "updated_at"));

        return category;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(meta.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
}
