package com.mayureshpatel.pfdataservice.repository.currency.mapper;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class CurrencyRowMapper extends JdbcMapperUtils implements RowMapper<Currency> {

    @Override
    public Currency mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps currency row from ResultSet with specified prefix on column names.
     *
     * @param rs     the ResultSet to map from
     * @param prefix the prefix to apply to column names
     * @return the mapped Currency object
     * @throws SQLException if an error occurs while accessing ResultSet
     */
    public static Currency mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        Currency.CurrencyBuilder builder = Currency.builder();
        builder.code(rs.getString(safePrefix + "code"));

        if (hasColumn(safePrefix + "name", availableColumns)) {
            builder.name(rs.getString(safePrefix + "name"));
        }
        if (hasColumn(safePrefix + "symbol", availableColumns)) {
            builder.symbol(rs.getString(safePrefix + "symbol"));
        }
        if (hasColumn(safePrefix + "is_active", availableColumns)) {
            builder.active(rs.getBoolean(safePrefix + "is_active"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
