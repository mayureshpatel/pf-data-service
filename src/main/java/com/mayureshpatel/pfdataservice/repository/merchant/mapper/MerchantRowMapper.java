package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link Merchant}, following the same
 * prefix/{@code hasColumn}-guarded convention as
 * {@link com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper}.
 */
@Component
public class MerchantRowMapper extends JdbcMapperUtils implements RowMapper<Merchant> {

    @Override
    public Merchant mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to a Merchant object with optional prefix for column names.
     * Handles null values gracefully and uses builder pattern for construction.
     *
     * @param rs     ResultSet containing row data
     * @param prefix Optional prefix for column names
     * @return Merchant object constructed from ResultSet row
     * @throws SQLException if there is an error accessing ResultSet
     */
    public static Merchant mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = normalizePrefix(prefix);
        Set<String> availableColumns = getAvailableColumns(rs);

        Merchant.MerchantBuilder builder = Merchant.builder();
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
        if (hasColumn(safePrefix + "name", availableColumns)) {
            builder.name(rs.getString(safePrefix + "name"));
        }
        mapLocationColumns(builder, rs, safePrefix, availableColumns);
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }

    /**
     * Maps the optional location columns and the list query's {@code link_count}, each only when
     * present in the result set -- split out of {@link #mapRow(ResultSet, String)} to keep that
     * method under the project's complexity threshold.
     *
     * @param builder          the merchant being built
     * @param rs               ResultSet containing row data
     * @param safePrefix       the normalized column prefix
     * @param availableColumns the columns present in the result set
     * @throws SQLException if there is an error accessing ResultSet
     */
    private static void mapLocationColumns(Merchant.MerchantBuilder builder, ResultSet rs, String safePrefix,
                                           Set<String> availableColumns) throws SQLException {
        if (hasColumn(safePrefix + "city", availableColumns)) {
            builder.city(rs.getString(safePrefix + "city"));
        }
        if (hasColumn(safePrefix + "state", availableColumns)) {
            builder.state(rs.getString(safePrefix + "state"));
        }
        if (hasColumn(safePrefix + "postal_code", availableColumns)) {
            builder.postalCode(rs.getString(safePrefix + "postal_code"));
        }
        if (hasColumn(safePrefix + "country", availableColumns)) {
            builder.country(rs.getString(safePrefix + "country"));
        }
        if (hasColumn(safePrefix + "link_count", availableColumns)) {
            builder.linkCount(rs.getInt(safePrefix + "link_count"));
        }
    }
}
