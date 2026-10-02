package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link MonthlyReportDataDto} -- one
 * year/month/income/expense aggregation row for the income-vs-expense report. Doesn't extend
 * {@code JdbcMapperUtils} like this package's other mappers since it has no embedded entity or
 * optional column to guard against; every column here is always present by construction of the
 * query it backs.
 */
@Component
public class MonthlyReportDataRowMapper implements RowMapper<MonthlyReportDataDto> {

    @Override
    public MonthlyReportDataDto mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return new MonthlyReportDataDto(
                rs.getInt("year"),
                rs.getInt("month"),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("expense")
        );
    }
}
