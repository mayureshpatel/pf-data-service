package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.repository.RowMapperFactory;
import com.mayureshpatel.pfdataservice.repository.account.model.Account;
import com.mayureshpatel.pfdataservice.repository.account.model.BankName;
import com.mayureshpatel.pfdataservice.repository.user.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AccountRowMapper extends RowMapperFactory implements RowMapper<Account> {

    @Override
    public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setName(rs.getString("name"));
        account.setType(rs.getString("type"));
        account.setCurrentBalance(getBigDecimal(rs, "current_balance"));
        account.setCurrencyCode(rs.getString("currency_code"));
        
        String bankName = rs.getString("bank_name");
        if (bankName != null) {
            account.setBankName(BankName.valueOf(bankName));
        }

        Long userId = getLongOrNull(rs, "user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            account.setUser(user);
        }

        account.setVersion(getLongOrNull(rs, "version"));
        account.setCreatedAt(getOffsetDateTime(rs, "created_at"));
        account.setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
        account.setDeletedAt(getOffsetDateTime(rs, "deleted_at"));

        Long createdById = getLongOrNull(rs, "created_by");
        if (createdById != null) {
            User createdBy = new User();
            createdBy.setId(createdById);
            account.setCreatedBy(createdBy);
        }

        Long updatedById = getLongOrNull(rs, "updated_by");
        if (updatedById != null) {
            User updatedBy = new User();
            updatedBy.setId(updatedById);
            account.setUpdatedBy(updatedBy);
        }

        Long deletedById = getLongOrNull(rs, "deleted_by");
        if (deletedById != null) {
            User deletedBy = new User();
            deletedBy.setId(deletedById);
            account.setDeletedBy(deletedBy);
        }

        return account;
    }
}
