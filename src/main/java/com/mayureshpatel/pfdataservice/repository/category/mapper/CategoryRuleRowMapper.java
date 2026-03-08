package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.user.mapper.UserRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class CategoryRuleRowMapper extends JdbcMapperUtils implements RowMapper<CategoryRule> {

    @Override
    public CategoryRule mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    public static CategoryRule mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        CategoryRule.CategoryRuleBuilder builder = CategoryRule.builder();
        if (hasColumn(safePrefix + "id", availableColumns)) {
            Long id = getLongOrNull(rs, safePrefix + "id");
            if (id == null) {
                return null;
            }
            builder.id(id);
        } else {
            return null;
        }

        if (availableColumns.contains(safePrefix + "keyword")) {
            builder.keyword(rs.getString(safePrefix + "keyword"));
        }
        if (availableColumns.contains(safePrefix + "priority")) {
            builder.priority(rs.getInt(safePrefix + "priority"));
        }
        if (availableColumns.contains(safePrefix + "category_id")) {
            builder.category(CategoryRowMapper.mapRow(rs, safePrefix + "category"));
        }
        if (availableColumns.contains(safePrefix + "user_id")) {
            builder.user(UserRowMapper.mapRow(rs, safePrefix + "user"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
