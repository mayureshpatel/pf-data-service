package com.mayureshpatel.pfdataservice.repository.transaction.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper;
import com.mayureshpatel.pfdataservice.repository.category.mapper.CategoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Maps a fully-joined transaction result set (with account, account_type,
 * category, parent category, and merchant) into a {@link Transaction} domain object.
 * Use this mapper with queries that include {@link com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries#ENRICHED_COLUMNS}
 * and {@link com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries#ENRICHED_JOINS}.
 */
@Component
public class TransactionDetailRowMapper extends JdbcMapperUtils implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a fully-joined transaction result set (with account, account_type,
     * category, parent category, and merchant) into a {@link Transaction} domain object.
     *
     * @param rs     the result set to map
     * @param prefix the prefix to use for column names
     * @return the mapped {@link Transaction} object
     * @throws SQLException if an error occurs while accessing the result set
     */
    public static Transaction mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        Transaction.TransactionBuilder builder = Transaction.builder();

        if (availableColumns.contains(safePrefix + "id")) {
            builder.id(rs.getLong(safePrefix + "id"));
        }
        if (availableColumns.contains(safePrefix + "account_id")) {
            builder.account(AccountRowMapper.mapRow(rs, "account"));
        }
        if (availableColumns.contains(safePrefix + "category_id")) {
            builder.category(CategoryRowMapper.mapRow(rs, "category"));
        }
        if (availableColumns.contains(safePrefix + "amount")) {
            builder.amount(rs.getBigDecimal(safePrefix + "amount"));
        }
        if (availableColumns.contains(safePrefix + "transaction_date")) {
            builder.transactionDate(getOffsetDateTime(rs, safePrefix + "transaction_date"));
        }
        if (availableColumns.contains(safePrefix + "post_date")) {
            builder.postDate(getOffsetDateTime(rs, safePrefix + "post_date"));
        }
        if (availableColumns.contains(safePrefix + "description")) {
            builder.description(rs.getString(safePrefix + "description"));
        }
        if (availableColumns.contains(safePrefix + "type")) {
            builder.type(TransactionType.valueOf(rs.getString(safePrefix + "type")));
        }
        if (availableColumns.contains(safePrefix + "merchant_id")) {
            builder.merchant(MerchantRowMapper.mapRow(rs, "merchant"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
