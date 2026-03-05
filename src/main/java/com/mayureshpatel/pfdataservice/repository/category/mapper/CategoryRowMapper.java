package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CategoryRowMapper extends JdbcMapperUtils implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Category.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .name(rs.getString("name"))
                .type(rs.getString("type"))
                .parentId(getLongOrNull(rs, "parent_id"))
                .parent(mapParent(rs))
                .color(rs.getString("color"))
                .icon(rs.getString("icon"))
                .audit(getAuditColumns(rs))
                .build();
    }

    private Category mapParent(ResultSet rs) throws SQLException {
        Long parentId = getLongOrNull(rs, "parent_id");
        if (parentId == null) return null;

        // Check if parent columns exist in result set (for joined queries)
        if (hasColumn(rs, "parent_name")) {
            return Category.builder()
                    .id(parentId)
                    .name(rs.getString("parent_name"))
                    .type(rs.getString("parent_type"))
                    .color(rs.getString("parent_color"))
                    .icon(rs.getString("parent_icon"))
                    .build();
        }
        return null;
    }
}
