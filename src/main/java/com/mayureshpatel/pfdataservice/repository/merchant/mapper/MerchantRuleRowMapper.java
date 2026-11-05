package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.user.mapper.UserRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link MerchantRule}, following the same
 * prefix/{@code hasColumn}-guarded convention as
 * {@link com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRuleRowMapper}. The
 * embedded {@code merchant} and {@code user} are delegated to {@link MerchantRowMapper} and
 * {@link UserRowMapper}. The keyword set is not mapped here; the repository attaches it.
 */
@Component
public class MerchantRuleRowMapper extends JdbcMapperUtils implements RowMapper<MerchantRule> {

    @Override
    public MerchantRule mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * @param rs     the ResultSet containing the row data
     * @param prefix the prefix to use for column names
     * @return the mapped rule, or null when the id column is absent or null
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public static MerchantRule mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = normalizePrefix(prefix);
        Set<String> availableColumns = getAvailableColumns(rs);

        Long id = hasColumn(safePrefix + "id", availableColumns) ? getLongOrNull(rs, safePrefix + "id") : null;
        if (id == null) {
            return null;
        }

        MerchantRule.MerchantRuleBuilder builder = MerchantRule.builder().id(id);
        mapOptionalFields(builder, rs, safePrefix, availableColumns);
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }

    /**
     * Every column besides {@code id} and the audit trail, each independently optional.
     */
    private static void mapOptionalFields(MerchantRule.MerchantRuleBuilder builder, ResultSet rs, String safePrefix, Set<String> availableColumns) throws SQLException {
        if (availableColumns.contains(safePrefix + "match_type")) {
            String matchType = rs.getString(safePrefix + "match_type");
            builder.matchType(matchType != null ? MatchType.valueOf(matchType) : null);
        }
        if (availableColumns.contains(safePrefix + "priority")) {
            builder.priority(rs.getInt(safePrefix + "priority"));
        }
        if (availableColumns.contains(safePrefix + "merchant_id")) {
            builder.merchant(MerchantRowMapper.mapRow(rs, safePrefix + "merchant"));
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
