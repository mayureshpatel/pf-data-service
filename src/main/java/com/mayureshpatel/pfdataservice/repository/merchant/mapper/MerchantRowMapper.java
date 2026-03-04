package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MerchantRowMapper extends JdbcMapperUtils implements RowMapper<Merchant> {

    @Override
    public Merchant mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Merchant.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .originalName(rs.getString("original_name"))
                .cleanName(rs.getString("clean_name"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
