package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class BudgetRowMapper extends JdbcMapperUtils implements RowMapper<Budget> {

    @Override
    public Budget mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to a Budget object using the specified prefix.
     *
     * @param rs      the ResultSet containing the row data
     * @param prefix  the prefix to use for column names
     * @return        the mapped Budget object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public static Budget mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        Budget.BudgetBuilder builder = Budget.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(rs.getLong(safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + "category_id", availableColumns)) {
            builder.categoryId(rs.getLong(safePrefix + "category_id"));
        }
        if (hasColumn(safePrefix + "amount", availableColumns)) {
            builder.amount(rs.getBigDecimal(safePrefix + "amount"));
        }
        if (hasColumn(safePrefix + "month", availableColumns)) {
            builder.month(rs.getInt(safePrefix + "month"));
        }
        if (hasColumn(safePrefix + "year", availableColumns)) {
            builder.year(rs.getInt(safePrefix + "year"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
