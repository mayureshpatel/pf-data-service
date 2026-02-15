package com.mayureshpatel.pfdataservice.jdbc.mapper;

import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.model.VendorRule;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class VendorRuleRowMapper extends RowMapperFactory implements RowMapper<VendorRule> {

    @Override
    public VendorRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        VendorRule rule = new VendorRule();
        rule.setId(rs.getLong("id"));
        rule.setKeyword(rs.getString("keyword"));
        rule.setVendorName(rs.getString("vendor_name"));
        rule.setPriority(rs.getInt("priority"));

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            rule.setUser(user);
        }

        rule.setCreatedAt(getLocalDateTime(rs, "created_at"));
        rule.setUpdatedAt(getLocalDateTime(rs, "updated_at"));

        return rule;
    }
}
