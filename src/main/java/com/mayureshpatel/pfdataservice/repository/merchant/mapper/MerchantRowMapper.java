package com.mayureshpatel.pfdataservice.repository.merchant.mapper;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.user.User;
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

        Long userId = rs.getLong("user_id");
        if (!userId.equals(0L)) {
            User user = new User();
            user.setId(userId);

            merchant.setUser(user);
        }
        merchant.setOriginalName(rs.getString("original_name"));
        merchant.setCleanName(rs.getString("clean_name"));

        TimestampAudit audit = new TimestampAudit();
        audit.setCreatedAt(getOffsetDateTime(rs, "created_at"));
        audit.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
        merchant.setAudit(audit);
        return merchant;
    }
}
