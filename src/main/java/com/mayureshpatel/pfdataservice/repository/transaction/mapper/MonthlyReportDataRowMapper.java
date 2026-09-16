package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

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
