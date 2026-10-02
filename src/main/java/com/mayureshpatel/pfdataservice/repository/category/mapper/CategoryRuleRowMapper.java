package com.mayureshpatel.pfdataservice.repository.category.mapper;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.user.mapper.UserRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link CategoryRule}, following the same
 * prefix/{@code hasColumn}-guarded convention as
 * {@link com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper}. The
 * embedded {@code category} and {@code user} are delegated to {@link CategoryRowMapper} and
 * {@link UserRowMapper} respectively.
 */
@Component
public class CategoryRuleRowMapper extends JdbcMapperUtils implements RowMapper<CategoryRule> {

    @Override
    public CategoryRule mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * @param rs     the ResultSet containing the row data
     * @param prefix the prefix to use for column names
     * @return the mapped CategoryRule object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
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

        if (availableColumns.contains(safePrefix + "match_type")) {
            String matchType = rs.getString(safePrefix + "match_type");
            builder.matchType(matchType != null ? MatchType.valueOf(matchType) : null);
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
        if (availableColumns.contains(safePrefix + "min_amount")) {
            builder.minAmount(rs.getBigDecimal(safePrefix + "min_amount"));
        }
        if (availableColumns.contains(safePrefix + "max_amount")) {
            builder.maxAmount(rs.getBigDecimal(safePrefix + "max_amount"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
