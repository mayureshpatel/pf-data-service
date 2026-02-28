package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
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
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setAmount(getBigDecimal(rs, "amount"));
        transaction.setTransactionDate(getOffsetDateTime(rs, "date"));
        transaction.setPostDate(getOffsetDateTime(rs, "post_date"));
        transaction.setDescription(rs.getString("description"));

        String type = rs.getString("type");
        if (type != null) {
            transaction.setType(TransactionType.valueOf(type));
        }

        transaction.setAudit(new TableAudit());
        transaction.getAudit().setCreatedAt(getOffsetDateTime(rs, "created_at"));
        transaction.getAudit().setUpdatedAt(getOffsetDateTime(rs, "updated_at"));
        transaction.getAudit().setDeletedAt(getOffsetDateTime(rs, "deleted_at"));

        transaction.setAccount(mapAccount(rs));
        transaction.setCategory(mapCategory(rs));
        transaction.setMerchant(mapMerchant(rs));

        return transaction;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        Long accountId = getLongOrNull(rs, "account_id");
        if (accountId == null) return null;

        Account account = new Account();
        account.setId(accountId);
        account.setName(rs.getString("acc_name"));
        account.setCurrentBalance(getBigDecimal(rs, "acc_balance"));
        account.setVersion(getLongOrNull(rs, "acc_version"));

        Long userId = getLongOrNull(rs, "acc_user_id");
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            account.setUser(user);
        }

        String currencyCode = rs.getString("acc_currency_code");
        if (currencyCode != null) {
            Currency currency = new Currency();
            currency.setCode(currencyCode);
            account.setCurrency(currency);
        }

        String bankNameStr = rs.getString("acc_bank_name");
        if (bankNameStr != null) {
            account.setBankName(BankName.valueOf(bankNameStr));
        }

        String accTypeCode = rs.getString("acc_type_code");
        if (accTypeCode != null) {
            AccountType accountType = new AccountType();
            accountType.setCode(accTypeCode);
            accountType.setLabel(rs.getString("acc_type_label"));
            accountType.setIsAsset(rs.getBoolean("acc_type_is_asset"));
            accountType.setSortOrder(rs.getInt("acc_type_sort_order"));
            accountType.setIsActive(rs.getBoolean("acc_type_is_active"));
            accountType.setIconography(new Iconography(
                    rs.getString("acc_type_icon"),
                    rs.getString("acc_type_color")
            ));
            account.setType(accountType);
        }

        return account;
    }

    private Category mapCategory(ResultSet rs) throws SQLException {
        Long categoryId = getLongOrNull(rs, "category_id");
        if (categoryId == null) return null;

        Category category = new Category();
        category.setId(categoryId);
        category.setName(rs.getString("cat_name"));
        category.setIconography(new Iconography(
                rs.getString("cat_icon"),
                rs.getString("cat_color")
        ));

        String catType = rs.getString("cat_type");
        if (catType != null) {
            category.setType(CategoryType.valueOf(catType));
        }

        Long catUserId = getLongOrNull(rs, "cat_user_id");
        if (catUserId != null) {
            User catUser = new User();
            catUser.setId(catUserId);
            category.setUser(catUser);
        }

        Long parentId = getLongOrNull(rs, "pcat_id");
        if (parentId != null) {
            Category parent = new Category();
            parent.setId(parentId);
            parent.setName(rs.getString("pcat_name"));
            parent.setIconography(new Iconography(
                    rs.getString("pcat_icon"),
                    rs.getString("pcat_color")
            ));
            String pcatType = rs.getString("pcat_type");
            if (pcatType != null) {
                parent.setType(CategoryType.valueOf(pcatType));
            }
            category.setParent(parent);
        }

        return category;
    }

    private Merchant mapMerchant(ResultSet rs) throws SQLException {
        Long merchantId = getLongOrNull(rs, "merchant_id");
        if (merchantId == null) return null;

        Merchant merchant = new Merchant();
        merchant.setId(merchantId);
        merchant.setOriginalName(rs.getString("merch_original_name"));
        merchant.setName(rs.getString("merch_clean_name"));

        Long merchUserId = getLongOrNull(rs, "merch_user_id");
        if (merchUserId != null) {
            User merchUser = new User();
            merchUser.setId(merchUserId);
            merchant.setUser(merchUser);
        }

        return merchant;
    }
}
