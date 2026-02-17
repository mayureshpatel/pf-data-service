package com.mayureshpatel.pfdataservice.repository.vendor.mapper;

import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class VendorRuleRowMapper extends JdbcMapperUtils implements RowMapper<VendorRule> {

    @Override
    public VendorRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        VendorRule rule = new VendorRule();
        rule.setId(rs.getLong("id"));
        rule.setKeyword(rs.getString("keyword"));
        rule.getVendor().setName(rs.getString("vendor_name"));
        rule.setPriority(rs.getInt("priority"));

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            rule.setUser(user);
        }

        rule.getAudit().setCreatedAt(getOffsetDateTime(rs, "created_at"));
        rule.getAudit().setUpdatedAt(getOffsetDateTime(rs, "updated_at"));

        return rule;
    }
}
