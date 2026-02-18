package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;

@Component
public class AccountTypeRowMapper implements RowMapper<AccountType> {

    @Override
    public AccountType mapRow(ResultSet rs, int rowNum) throws SQLException {
        AccountType accountType = new AccountType();
        accountType.setCode(rs.getString("code"));
        accountType.setLabel(rs.getString("label"));
        accountType.setIconography(new Iconography(rs.getString("icon"), rs.getString("color")));
        accountType.setIsAsset(rs.getBoolean("is_asset"));
        accountType.setSortOrder(rs.getInt("sort_order"));
        accountType.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            accountType.getAudit().setCreatedAt(createdAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            accountType.getAudit().setUpdatedAt(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        return accountType;
    }
}
