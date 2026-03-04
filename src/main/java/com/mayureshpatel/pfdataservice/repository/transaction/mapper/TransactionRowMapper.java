package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransactionRowMapper extends JdbcMapperUtils implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        Merchant merchant = Merchant.builder()
                .id(rs.getLong("merchant_id"))
                .build();

        Transaction transaction = Transaction.builder()
                .id(rs.getLong("id"))
                .amount(getBigDecimal(rs, "amount"))
                .transactionDate(getOffsetDateTime(rs, "date"))
                .postDate(getOffsetDateTime(rs, "post_date"))
                .description(rs.getString("description"))
                .merchant(merchant)
                .build();

        String type = rs.getString("type");
        if (type != null) {
            transaction.toBuilder().type(TransactionType.valueOf(type));
        }

        Long accountId = getLongOrNull(rs, "account_id");
        if (accountId != null) {
            Account account = Account.builder()
                    .id(accountId)
                    .build();
            transaction.toBuilder().account(account);
        }

        Long categoryId = getLongOrNull(rs, "category_id");
        if (categoryId != null) {
            Category category = Category.builder()
                    .id(categoryId)
                    .build();
            transaction.toBuilder().category(category);
        }

        transaction.toBuilder().audit(getAuditColumns(rs));
        return transaction;
    }
}
