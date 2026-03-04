package com.mayureshpatel.pfdataservice.repository.currency.mapper;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CurrencyRowMapper extends JdbcMapperUtils implements RowMapper<Currency> {

    @Override
    public Currency mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Currency.builder()
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .symbol(rs.getString("symbol"))
                .active(rs.getBoolean("is_active"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
