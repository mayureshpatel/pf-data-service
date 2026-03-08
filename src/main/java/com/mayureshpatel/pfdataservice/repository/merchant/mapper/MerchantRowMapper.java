package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

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
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        Merchant.MerchantBuilder builder = Merchant.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(rs.getLong(safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + "original_name", availableColumns)) {
            builder.originalName(rs.getString(safePrefix + "original_name"));
        }
        if (hasColumn(safePrefix + "clean_name", availableColumns)) {
            builder.cleanName(rs.getString(safePrefix + "clean_name"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
