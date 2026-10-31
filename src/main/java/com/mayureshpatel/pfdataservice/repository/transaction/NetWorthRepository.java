package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.transaction.query.NetWorthQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The set-based reads behind the Reports net worth view: monthly net flows per account (for month-end
 * balances), per-account deposits, withdrawals and the rest (for an account's story), and income and
 * expense totals (for "saved" and for spending). Always scoped to the owning user and leaves
 * soft-deleted rows out.
 */
@Repository
@RequiredArgsConstructor
public class NetWorthRepository {

    private static final String ACCOUNT_ID = "account_id";
    private static final String START = "start";
    private static final String END = "end";
    private static final String ACCOUNT_IDS = "accountIds";

    private final JdbcClient jdbcClient;

    /**
     * An account's money in and out over a window.
     *
     * @param deposits               income and transfers in
     * @param withdrawals            expenses and transfers out, as a positive amount
     * @param interestAndAdjustments the signed sum of every other transaction type
     */
    public record StoryFlows(BigDecimal deposits, BigDecimal withdrawals, BigDecimal interestAndAdjustments) {
    }

    /**
     * Income and expense totals over a window.
     *
     * @param income  the sum of income transactions
     * @param expense the sum of expense transactions
     */
    public record IncomeExpense(BigDecimal income, BigDecimal expense) {
    }

    /**
     * Each account's signed net flow per calendar month from a month onward, using the same sign rules as the
     * stored balance: income and transfers in add, expenses and transfers out subtract, anything else adds.
     *
     * @param userId         the owning user's id
     * @param fromMonthStart any date in the first month to include (the whole month is read)
     * @return account id to (month to net flow); accounts and months with no activity are absent
     */
    public Map<Long, Map<YearMonth, BigDecimal>> findMonthlyNetFlow(Long userId, LocalDate fromMonthStart) {
        OffsetDateTime from = YearMonth.from(fromMonthStart).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Map<Long, Map<YearMonth, BigDecimal>> flows = new HashMap<>();
        jdbcClient.sql(NetWorthQueries.FIND_MONTHLY_NET_FLOW)
                .param(SqlParams.USER_ID, userId)
                .param("from", from)
                .query((rs, rowNum) -> {
                    flows.computeIfAbsent(rs.getLong(ACCOUNT_ID), id -> new HashMap<>())
                            .put(YearMonth.parse(rs.getString("flow_month")), rs.getBigDecimal("net_flow"));
                    return rowNum;
                })
                .list();
        return flows;
    }

    /**
     * Each account's deposits, withdrawals and other movements over a window.
     *
     * @param userId the owning user's id
     * @param window the half-open UTC window to look at
     * @return account id to its flows; accounts with no activity in the window are absent
     */
    public Map<Long, StoryFlows> findStoryFlows(Long userId, DateWindow window) {
        Map<Long, StoryFlows> story = new HashMap<>();
        jdbcClient.sql(NetWorthQueries.FIND_STORY_FLOWS)
                .param(SqlParams.USER_ID, userId)
                .param(START, window.start())
                .param(END, window.end())
                .query((rs, rowNum) -> {
                    story.put(rs.getLong(ACCOUNT_ID), new StoryFlows(
                            rs.getBigDecimal("deposits"), rs.getBigDecimal("withdrawals"), rs.getBigDecimal("other_flow")));
                    return rowNum;
                })
                .list();
        return story;
    }

    /**
     * Income and expense totals over a window; transfers and adjustments are not counted.
     *
     * @param userId     the owning user's id
     * @param accountIds the accounts to include, or empty for all of the user's accounts
     * @param window     the half-open UTC window to look at
     * @return the two totals (zero when there is nothing)
     */
    public IncomeExpense findIncomeExpense(Long userId, List<Long> accountIds, DateWindow window) {
        String sql = accountIds.isEmpty() ? NetWorthQueries.FIND_INCOME_EXPENSE
                : NetWorthQueries.FIND_INCOME_EXPENSE + NetWorthQueries.AND_ACCOUNTS;
        JdbcClient.StatementSpec spec = jdbcClient.sql(sql)
                .param(SqlParams.USER_ID, userId)
                .param(START, window.start())
                .param(END, window.end());
        if (!accountIds.isEmpty()) {
            spec = spec.param(ACCOUNT_IDS, accountIds);
        }
        return spec.query((rs, rowNum) -> new IncomeExpense(rs.getBigDecimal("income"), rs.getBigDecimal("expense")))
                .single();
    }

    /**
     * Expense total over a window, for the average monthly spending.
     *
     * @param userId     the owning user's id
     * @param accountIds the accounts to include, or empty for all of the user's accounts
     * @param window     the half-open UTC window to look at
     * @return the sum of expense transactions
     */
    public BigDecimal findExpense(Long userId, List<Long> accountIds, DateWindow window) {
        return findIncomeExpense(userId, accountIds, window).expense();
    }
}
