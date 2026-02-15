package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.repository.transaction.model.Currency;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;

@Component
public class CurrencyRowMapper implements RowMapper<Currency> {

    @Override
    public Currency mapRow(ResultSet rs, int rowNum) throws SQLException {
        Currency currency = new Currency();
        currency.setCode(rs.getString("code"));
        currency.setName(rs.getString("name"));
        currency.setSymbol(rs.getString("symbol"));
        currency.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            currency.setCreatedAt(createdAtTimestamp.toInstant()
                    .atOffset(ZoneOffset.UTC));
        }

        return currency;
    }
}
