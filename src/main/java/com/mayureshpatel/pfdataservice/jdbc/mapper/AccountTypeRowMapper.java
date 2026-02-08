package com.mayureshpatel.pfdataservice.jdbc.mapper;

import com.mayureshpatel.pfdataservice.model.AccountTypeLookup;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;

@Component
public class AccountTypeRowMapper implements RowMapper<AccountTypeLookup> {

    @Override
    public AccountTypeLookup mapRow(ResultSet rs, int rowNum) throws SQLException {
        AccountTypeLookup accountType =  new AccountTypeLookup();
        accountType.setCode(rs.getString("code"));
        accountType.setLabel(rs.getString("label"));
        accountType.setIcon(rs.getString("icon"));
        accountType.setColor(rs.getString("color"));
        accountType.setIsAsset(rs.getBoolean("is_asset"));
        accountType.setSortOrder(rs.getInt("sort_order"));
        accountType.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null){
            accountType.setCreatedAt(createdAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null){
            accountType.setUpdatedAt(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        return accountType;
    }
}
