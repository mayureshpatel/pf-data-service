package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class CategoryRowMapper extends JdbcMapperUtils implements RowMapper<Category> {

    @Override
    public Category mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps the parent category from the result set.
     *
     * @param parentId The ID of the parent category.
     * @param rs The result set containing parent category data.
     * @param prefix The column prefix for parent category data.
     * @param availableColumns The set of available columns in the result set.
     * @return The mapped parent category or null if parentId is 0.
     * @throws SQLException If an error occurs while accessing the result set.
     */
    private static Category mapParent(long parentId, ResultSet rs, String prefix, Set<String> availableColumns) throws SQLException {
        if (parentId == 0) {
            return null;
        }

        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Category.CategoryBuilder parentBuilder = Category.builder();
        parentBuilder.id(parentId);

        if (hasColumn(safePrefix + "name", availableColumns)) {
            parentBuilder.name(rs.getString(safePrefix + "name"));
        }
        if (hasColumn(safePrefix + "type", availableColumns)) {
            parentBuilder.type(rs.getString(safePrefix + "type"));
        }
        if (hasColumn(safePrefix + "color", availableColumns)) {
            parentBuilder.color(rs.getString(safePrefix + "color"));
        }
        if (hasColumn(safePrefix + "icon", availableColumns)) {
            parentBuilder.icon(rs.getString(safePrefix + "icon"));
        }
        return parentBuilder.build();
    }

    /**
     * Maps a ResultSet row to a Category object with prefix support.
     *
     * @param rs     the ResultSet containing the row data
     * @param prefix the prefix to use for column names
     * @return the mapped Category object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public static Category mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        Category.CategoryBuilder builder = Category.builder();
        if (hasColumn(safePrefix + "id", availableColumns)) {
            builder.id(rs.getLong(safePrefix + "id"));
        } else {
            return null;
        }

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(rs.getLong(safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + "name", availableColumns)) {
            builder.name(rs.getString(safePrefix + "name"));
        }
        if (hasColumn( safePrefix + "parent_id", availableColumns)) {
            long parentId = rs.getLong(safePrefix + "parent_id");
            builder.parentId(parentId);

            if (parentId != 0) {
                builder.parent(mapParent(parentId, rs, safePrefix + "parent_", availableColumns));
            }
        }
        if (hasColumn(safePrefix + "color", availableColumns)) {
            builder.color(rs.getString(safePrefix + "color"));
        }
        if (hasColumn(safePrefix + "icon", availableColumns)) {
            builder.icon(rs.getString(safePrefix + "icon"));
        }
        if (hasColumn(safePrefix + "type", availableColumns)) {
            builder.type(rs.getString(safePrefix + "type"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));
        return builder.build();
    }
}
