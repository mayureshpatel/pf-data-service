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
        Merchant merchant = new Merchant();
        merchant.setId(rs.getLong("id"));
        merchant.getUser().setId(rs.getLong("user_id"));
        merchant.setOriginalName(rs.getString("original_name"));
        merchant.setName(rs.getString("name"));

        merchant.getAudit().setCreatedAt(getOffsetDateTime(rs, "created_at"));
        merchant.getAudit().setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
        return merchant;
    }
}
