package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryBreakdownRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.CategoryReportDataRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.mapper.MonthlyReportDataRowMapper;
import com.mayureshpatel.pfdataservice.repository.transaction.query.TransactionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * JDBC-backed read-only aggregation queries over {@link com.mayureshpatel.pfdataservice.domain.transaction.Transaction}
 * for the Dashboard and Reports features specifically (PF-809: extracted from
 * {@code TransactionRepository}, whose basic CRUD plus every analytics query the whole app
 * needed had grown it well past PMD's {@code CouplingBetweenObjects} threshold). Every method
 * here is exclusively consumed by {@code DashboardService}/{@code ReportService} -- confirmed by
 * checking every real caller before moving anything, not assumed from the method names alone.
 */
@Repository
@RequiredArgsConstructor
public class TransactionReportRepository {

    // PF-828: see TransactionRepository's own identical field comment -- same root cause
    // (a bare LocalDate parameter resolves using the database session's timezone, not UTC).
    private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;
    private static final String PARAM_START_DATE = "startDate";
    private static final String PARAM_END_DATE = "endDate";
    private static final String PARAM_TYPE = "type";

    private final JdbcClient jdbcClient;
    private final CategoryBreakdownRowMapper categoryBreakdownRowMapper;
    private final CategoryReportDataRowMapper categoryReportDataRowMapper;
    private final MonthlyReportDataRowMapper monthlyReportDataRowMapper;

    /**
     * Dashboard's category-spend-breakdown widget and the Reports feature's own simpler
     * equivalent both use this -- the Reports feature's richer equivalent
     * ({@link #findCategoryReportData}) deliberately excludes uncategorized instead.
     *
     * @param userId the owning user's id
     * @param start  the inclusive range start
     * @param end    the exclusive range end
     * @return one row per category (including uncategorized) with any spend in the range, highest first
     */
    public List<CategoryBreakdownDto> findCategoryTotals(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcClient.sql(TransactionQueries.FIND_CATEGORY_TOTALS)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, start)
                .param(PARAM_END_DATE, end)
                .query(categoryBreakdownRowMapper)
                .list();
    }

    /**
     * PF-823: Reports' Categories tab data for the given range, aggregated fully server-side --
     * no row cap, unlike the client-side approach it replaces.
     */
    public List<CategoryReportDataDto> findCategoryReportData(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcClient.sql(TransactionQueries.FIND_CATEGORY_REPORT_DATA)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, start)
                .param(PARAM_END_DATE, end)
                .query(categoryReportDataRowMapper)
                .list();
    }

    /**
     * PF-823: Reports' Cash Flow tab data for the given range, one row per month with income and
     * expense already pivoted side by side.
     */
    public List<MonthlyReportDataDto> findMonthlyIncomeExpense(Long userId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcClient.sql(TransactionQueries.FIND_MONTHLY_INCOME_EXPENSE)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, start)
                .param(PARAM_END_DATE, end)
                .query(monthlyReportDataRowMapper)
                .list();
    }

    /**
     * Dashboard pulse widget's trailing-months income/expense totals, one row per
     * (year, month, type) rather than {@link #findMonthlyIncomeExpense}'s pre-pivoted shape --
     * open-ended from {@code startDate} to now, not bounded by an explicit end like the Reports
     * feature's equivalent.
     *
     * @param userId    the owning user's id
     * @param startDate the inclusive start of the trailing window
     * @return raw {@code [year, month, type, total]} rows; not mapped to a DTO since callers
     *         reduce this further before it ever reaches the API boundary
     */
    public List<Object[]> findMonthlySums(Long userId, LocalDate startDate) {
        return jdbcClient.sql(TransactionQueries.FIND_MONTHLY_SUMS)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime())
                .query((rs, rowNum) -> new Object[]{
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getString(PARAM_TYPE),
                        rs.getBigDecimal("total")
                })
                .list();
    }

    /**
     * @param userId the owning user's id
     * @return the total of all uncategorized expense transactions, or zero if there are none
     */
    public BigDecimal getUncategorizedExpenseTotals(Long userId) {
        return jdbcClient.sql(TransactionQueries.GET_UNCATEGORIZED_EXPENSE_TOTALS)
                .param(SqlParams.USER_ID, userId)
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }

    /**
     * @param userId the owning user's id
     * @return the number of uncategorized expense transactions
     */
    public long getUncategorizedExpenseCount(Long userId) {
        return jdbcClient.sql(TransactionQueries.GET_UNCATEGORIZED_EXPENSE_COUNT)
                .param(SqlParams.USER_ID, userId)
                .query(Long.class)
                .single();
    }

    /**
     * @param userId the owning user's id
     * @param start  the inclusive range start
     * @param end    the inclusive range end
     * @param type   the transaction type to sum (e.g. only {@code EXPENSE})
     * @return the total for transactions of this type in the range, or zero if there are none
     */
    public BigDecimal getSumByDateRange(Long userId, OffsetDateTime start, OffsetDateTime end, TransactionType type) {
        return jdbcClient.sql(TransactionQueries.GET_SUM_BY_DATE_RANGE)
                .param(SqlParams.USER_ID, userId)
                .param(PARAM_START_DATE, start)
                .param(PARAM_END_DATE, end)
                .param(PARAM_TYPE, type.name())
                .query(BigDecimal.class)
                .optional()
                .orElse(BigDecimal.ZERO);
    }
}
