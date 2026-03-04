package com.mayureshpatel.pfdataservice.repository.budget.mapper;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BudgetRowMapper extends JdbcMapperUtils implements RowMapper<Budget> {

    @Override
    public Budget mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Budget.builder()
                .id(rs.getLong("id"))
                .categoryId(rs.getLong("category_id"))
                .amount(rs.getBigDecimal("amount"))
                .month(rs.getInt("month"))
                .year(rs.getInt("year"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
