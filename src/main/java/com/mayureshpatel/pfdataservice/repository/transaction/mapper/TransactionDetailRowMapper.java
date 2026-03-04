package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a fully-joined transaction result set (with account, account_type,
 * category, parent category, and merchant) into a {@link Transaction} domain object.
 * Use this mapper with queries that include {@link com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries#ENRICHED_COLUMNS}
 * and {@link com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries#ENRICHED_JOINS}.
 */
@Component
public class TransactionDetailRowMapper extends JdbcMapperUtils implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Transaction.builder()
                .id(rs.getLong("id"))
                .account(mapAccount(rs))
                .category(mapCategory(rs))
                .amount(getBigDecimal(rs, "amount"))
                .transactionDate(getOffsetDateTime(rs, "date"))
                .postDate(getOffsetDateTime(rs, "post_date"))
                .description(rs.getString("description"))
                .merchant(mapMerchant(rs))
                .type(mapType(rs))
                .audit(getAuditColumns(rs))
                .build();
    }

    private TransactionType mapType(ResultSet rs) throws SQLException {
        return TransactionType.valueOf(rs.getString("type"));
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        Long accountId = getLongOrNull(rs, "account_id");
        if (accountId == null) return null;

        Long userId = mapUser(rs) != null ? mapUser(rs).getId() : null;
        String typeCode = mapAccountType(rs) != null ? mapAccountType(rs).getCode() : null;
        String currencyCode = mapCurrency(rs) != null ? mapCurrency(rs).getCode() : null;
        String bankName = mapBankName(rs) != null ? mapBankName(rs).name() : null;

        return Account.builder()
                .id(accountId)
                .userId(userId)
                .name(rs.getString("acc_name"))
                .typeCode(typeCode)
                .currentBalance(rs.getBigDecimal("acc_balance"))
                .currencyCode(currencyCode)
                .version(rs.getLong("acc_version"))
                .bankCode(bankName)
                .audit(getAuditColumns(rs))
                .build();
    }

    private User mapUser(ResultSet rs) throws SQLException {
        Long userId = getLongOrNull(rs, "acc_user_id");
        if (userId == null) return null;

        return User.builder()
                .id(userId)
                .build();
    }

    private BankName mapBankName(ResultSet rs) throws SQLException {
        String bankNameStr = rs.getString("acc_bank_name");
        if (bankNameStr == null) return null;

        return BankName.valueOf(bankNameStr);
    }

    private Currency mapCurrency(ResultSet rs) throws SQLException {
        String currencyCode = rs.getString("acc_currency_code");
        if (currencyCode == null) return null;

        return Currency.builder()
                .code(currencyCode)
                .build();
    }

    private AccountType mapAccountType(ResultSet rs) throws SQLException {
        String accTypeCode = rs.getString("acc_type_code");
        if (accTypeCode == null) return null;

        return AccountType.builder()
                .code(accTypeCode)
                .label(rs.getString("acc_type_label"))
                .asset(rs.getBoolean("acc_type_is_asset"))
                .sortOrder(rs.getInt("acc_type_sort_order"))
                .active(rs.getBoolean("acc_type_is_active"))
                .color(rs.getString("acc_type_color"))
                .icon(rs.getString("acc_type_icon"))
                .build();
    }

    private Category mapCategory(ResultSet rs) throws SQLException {
        return Category.builder()
                .id(rs.getLong("category_id"))
                .userId(rs.getLong("cat_user_id"))
                .name(rs.getString("cat_name"))
                .type(CategoryType.valueOf(rs.getString("cat_type")).name())
                .parentId(rs.getLong("pcat_id"))
                .color(rs.getString("cat_color"))
                .icon(rs.getString("cat_icon"))
                .build();
    }

    private Merchant mapMerchant(ResultSet rs) throws SQLException {
        return Merchant.builder()
                .id(rs.getLong("merchant_id"))
                .userId(rs.getLong("merch_user_id"))
                .originalName(rs.getString("merch_original_name"))
                .cleanName(rs.getString("merch_clean_name"))
                .build();
    }
}
