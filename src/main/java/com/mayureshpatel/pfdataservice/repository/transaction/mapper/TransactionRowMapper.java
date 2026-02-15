package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransactionRowMapper extends JdbcMapperUtils implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setAmount(getBigDecimal(rs, "amount"));
        transaction.setDate(getLocalDate(rs, "date"));
        transaction.setPostDate(getLocalDate(rs, "post_date"));
        transaction.setDescription(rs.getString("description"));
        transaction.setOriginalVendorName(rs.getString("original_vendor_name"));
        transaction.setVendorName(rs.getString("vendor_name"));
        
        String type = rs.getString("type");
        if (type != null) {
            transaction.setType(TransactionType.valueOf(type));
        }

        Long accountId = getLongOrNull(rs, "account_id");
        if (accountId != null) {
            Account account = new Account();
            account.setId(accountId);
            transaction.setAccount(account);
        }

        Long categoryId = getLongOrNull(rs, "category_id");
        if (categoryId != null) {
            Category category = new Category();
            category.setId(categoryId);
            transaction.setCategory(category);
        }

        transaction.setCreatedAt(getLocalDateTime(rs, "created_at"));
        transaction.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        transaction.setDeletedAt(getLocalDateTime(rs, "deleted_at"));

        return transaction;
    }
}
