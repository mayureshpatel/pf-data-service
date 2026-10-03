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
        String safePrefix = normalizePrefix(prefix);
        Set<String> availableColumns = getAvailableColumns(rs);

        Long id = requireId(rs, safePrefix, availableColumns);
        if (id == null) {
            return null;
        }

        CategoryRule.CategoryRuleBuilder builder = CategoryRule.builder().id(id);
        mapOptionalFields(builder, rs, safePrefix, availableColumns);
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }

    /**
     * The one required column (PF-809: extracted from {@link #mapRow(ResultSet, String)}) --
     * {@code null} whether the column is simply absent from the query or present but itself
     * {@code null}, since either way there's no rule to map.
     */
    private static Long requireId(ResultSet rs, String safePrefix, Set<String> availableColumns) throws SQLException {
        return hasColumn(safePrefix + "id", availableColumns) ? getLongOrNull(rs, safePrefix + "id") : null;
    }

    /**
     * Every column besides {@code id} and the audit trail, each independently optional
     * (PF-809: extracted from {@link #mapRow(ResultSet, String)}).
     */
    private static void mapOptionalFields(CategoryRule.CategoryRuleBuilder builder, ResultSet rs, String safePrefix, Set<String> availableColumns) throws SQLException {
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
    }
}
