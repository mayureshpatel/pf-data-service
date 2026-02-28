package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AccountRowMapper extends JdbcMapperUtils implements RowMapper<Account> {

    @Override
    public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setName(rs.getString("name"));
        
        account.setCurrentBalance(getBigDecimal(rs, "current_balance"));

        account.setCurrency(new com.mayureshpatel.pfdataservice.domain.currency.Currency());
        account.getCurrency().setCode(rs.getString("currency_code"));

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

        AccountType accountType = new AccountType();
        accountType.setCode(rs.getString("account_type_code"));
        accountType.setLabel(rs.getString("account_type_label"));
        accountType.setAsset(rs.getBoolean("account_type_is_asset"));
        accountType.setSortOrder(rs.getInt("account_type_sort_order"));
        accountType.setActive(rs.getBoolean("account_type_is_active"));

        Iconography accountTypeIconography = new Iconography();
        accountTypeIconography.setColor(rs.getString("account_type_color"));
        accountTypeIconography.setIcon(rs.getString("account_type_icon"));
        accountType.setIconography(accountTypeIconography);
        account.setType(accountType);

        account.setVersion(getLongOrNull(rs, "version"));
        
        account.setAudit(new com.mayureshpatel.pfdataservice.domain.TableAudit());
        account.getAudit().setCreatedAt(getOffsetDateTime(rs, "created_at"));
        account.getAudit().setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
        account.getAudit().setDeletedAt(getOffsetDateTime(rs, "deleted_at"));

        Long createdById = getLongOrNull(rs, "created_by");
        if (createdById != null) {
            User createdBy = new User();
            createdBy.setId(createdById);
            account.getAudit().setCreatedBy(createdBy);
        }

        Long updatedById = getLongOrNull(rs, "updated_by");
        if (updatedById != null) {
            User updatedBy = new User();
            updatedBy.setId(updatedById);
            account.getAudit().setUpdatedBy(updatedBy);
        }

        Long deletedById = getLongOrNull(rs, "deleted_by");
        if (deletedById != null) {
            User deletedBy = new User();
            deletedBy.setId(deletedById);
            account.getAudit().setDeletedBy(deletedBy);
        }

        return account;
    }
}
