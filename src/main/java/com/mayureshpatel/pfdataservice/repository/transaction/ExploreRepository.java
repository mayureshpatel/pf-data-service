package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTransactionDto;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.transaction.query.ExploreQueryBuilder;
import com.mayureshpatel.pfdataservice.repository.transaction.query.ExploreQueryBuilder.BuiltQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Aggregates a user's income and expense transactions by any grouping the Reports explorer offers
 * (category, merchant, account, tag, or a time bucket) under stacked filters. The SQL comes from
 * {@link ExploreQueryBuilder}; this class only binds the user, the window and the filter values.
 * Always scoped to the owning user and excludes soft-deleted rows, transfers and adjustments.
 */
@Repository
@RequiredArgsConstructor
public class ExploreRepository {

    private static final String START = "start";
    private static final String END = "end";
    private static final String TYPES = "types";

    private final JdbcClient jdbcClient;

    /**
     * One group of the aggregation.
     *
     * @param key          the group's id, a date for time buckets, or null for the "none" group
     * @param label        the group's display name ("Uncategorized", "No merchant", "No tag" for null keys)
     * @param income       the group's income total
     * @param expense      the group's expense total
     * @param incomeCount  the number of income transactions
     * @param expenseCount the number of expense transactions
     */
    public record ExploreRowData(String key, String label, BigDecimal income, BigDecimal expense, long incomeCount, long expenseCount) {
    }

    /**
     * Totals the user's income and expense per group.
     *
     * @param userId  the owning user's id
     * @param groupBy how to group
     * @param filter  the stacked filters
     * @param window  the half-open UTC window to look at
     * @return one row per group with activity, ordered by group key
     */
    public List<ExploreRowData> findGroups(Long userId, ExploreGroupBy groupBy, ExploreFilter filter, DateWindow window) {
        BuiltQuery query = ExploreQueryBuilder.build(groupBy, filter);
        return jdbcClient.sql(query.sql())
                .param(SqlParams.USER_ID, userId)
                .param(START, window.start())
                .param(END, window.end())
                .params(query.params())
                .query((rs, rowNum) -> mapRow(rs))
                .list();
    }

    /**
     * Counts the transactions the explorer leaves out of the window: transfers and adjustments.
     *
     * @param userId the owning user's id
     * @param filter the stacked filters
     * @param window the half-open UTC window to look at
     * @return how many transfer and adjustment transactions match
     */
    public long countExcluded(Long userId, ExploreFilter filter, DateWindow window) {
        BuiltQuery query = ExploreQueryBuilder.buildExcludedCount(filter);
        return jdbcClient.sql(query.sql())
                .param(SqlParams.USER_ID, userId)
                .param(START, window.start())
                .param(END, window.end())
                .params(query.params())
                .query(Long.class)
                .single();
    }

    /**
     * Pages through the transactions behind an explorer number, newest first.
     *
     * @param userId   the owning user's id
     * @param filter   the stacked filters
     * @param window   the half-open UTC window to look at
     * @param types    the transaction types to include (income, expense)
     * @param pageable the page to fetch
     * @return the page of transactions with the total match count
     */
    public Page<ExploreTransactionDto> findTransactions(Long userId, ExploreFilter filter, DateWindow window, List<String> types, Pageable pageable) {
        BuiltQuery count = ExploreQueryBuilder.buildTransactionCount(filter);
        long total = jdbcClient.sql(count.sql())
                .param(SqlParams.USER_ID, userId)
                .param(START, window.start())
                .param(END, window.end())
                .param(TYPES, types)
                .params(count.params())
                .query(Long.class)
                .single();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        BuiltQuery list = ExploreQueryBuilder.buildTransactionList(filter);
        List<ExploreTransactionDto> content = jdbcClient.sql(list.sql())
                .param(SqlParams.USER_ID, userId)
                .param(START, window.start())
                .param(END, window.end())
                .param(TYPES, types)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .params(list.params())
                .query((rs, rowNum) -> mapTransaction(rs))
                .list();
        return new PageImpl<>(content, pageable, total);
    }

    private ExploreTransactionDto mapTransaction(ResultSet rs) throws SQLException {
        return new ExploreTransactionDto(
                rs.getLong("id"),
                rs.getDate("tx_date").toLocalDate(),
                rs.getString("description"),
                rs.getString("merchant_name"),
                rs.getString("account_name"),
                rs.getString("category_name"),
                rs.getBigDecimal("amount"),
                TransactionType.valueOf(rs.getString("type")));
    }

    private ExploreRowData mapRow(ResultSet rs) throws SQLException {
        String key = keyOf(rs.getObject("group_key"));
        String label = key != null && isTimeLabel(rs) ? key : rs.getString("group_label");
        return new ExploreRowData(
                key,
                label,
                rs.getBigDecimal("income"),
                rs.getBigDecimal("expense"),
                rs.getLong("income_count"),
                rs.getLong("expense_count"));
    }

    /** A time bucket's label column is the bucket expression itself, which would print as a raw timestamp. */
    private static boolean isTimeLabel(ResultSet rs) throws SQLException {
        Object label = rs.getObject("group_label");
        return label instanceof Timestamp || label instanceof java.sql.Date || label instanceof BigDecimal;
    }

    private static String keyOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate().toString();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }
}
