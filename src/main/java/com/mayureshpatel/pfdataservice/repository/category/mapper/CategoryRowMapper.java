package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link Category}, following the same
 * prefix/{@code hasColumn}-guarded convention as
 * {@link com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper}. A
 * subcategory's parent is mapped one level deep via {@link #mapParent}, matching
 * {@code Category}'s own one-level-of-nesting design.
 */
@Component
public class CategoryRowMapper extends JdbcMapperUtils implements RowMapper<Category> {

    private static final String COL_NAME = "name";
    private static final String COL_TYPE = "type";
    private static final String COL_COLOR = "color";
    private static final String COL_ICON = "icon";

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

        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Category.CategoryBuilder parentBuilder = Category.builder();
        parentBuilder.id(parentId);

        if (hasColumn(safePrefix + COL_NAME, availableColumns)) {
            parentBuilder.name(rs.getString(safePrefix + COL_NAME));
        }
        if (hasColumn(safePrefix + COL_TYPE, availableColumns)) {
            parentBuilder.type(rs.getString(safePrefix + COL_TYPE));
        }
        if (hasColumn(safePrefix + COL_COLOR, availableColumns)) {
            parentBuilder.color(rs.getString(safePrefix + COL_COLOR));
        }
        if (hasColumn(safePrefix + COL_ICON, availableColumns)) {
            parentBuilder.icon(rs.getString(safePrefix + COL_ICON));
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
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        Category.CategoryBuilder builder = Category.builder();
        if (hasColumn(safePrefix + "id", availableColumns)) {
            Long id = getLongOrNull(rs, safePrefix + "id");
            if (id == null) {
                return null;
            }
            builder.id(id);
        } else {
            return null;
        }

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(getLongOrNull(rs, safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + COL_NAME, availableColumns)) {
            builder.name(rs.getString(safePrefix + COL_NAME));
        }
        if (hasColumn( safePrefix + "parent_id", availableColumns)) {
            long parentId = rs.getLong(safePrefix + "parent_id");
            builder.parentId(parentId);

            if (parentId != 0) {
                builder.parent(mapParent(parentId, rs, safePrefix + "category_parent", availableColumns));
            }
        }
        if (hasColumn(safePrefix + COL_COLOR, availableColumns)) {
            builder.color(rs.getString(safePrefix + COL_COLOR));
        }
        if (hasColumn(safePrefix + COL_ICON, availableColumns)) {
            builder.icon(rs.getString(safePrefix + COL_ICON));
        }
        if (hasColumn(safePrefix + COL_TYPE, availableColumns)) {
            builder.type(rs.getString(safePrefix + COL_TYPE));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
