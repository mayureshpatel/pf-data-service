package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.NetWorthDataPointDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes report-oriented views over a user's data that don't belong to any single existing
 * domain Service. Introduced by PF-304 for net-worth-over-time; the home for future report types
 * this epic's own refinement named as candidates (budget-vs-actual, recurring-transaction
 * forecasting), rather than growing an unrelated existing Service to fit them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /**
     * Session timezone can differ from UTC (see PF-828/PF-836); every {@link LocalDate} boundary
     * this service hands to a repository is anchored to UTC explicitly rather than left to
     * whatever the JDBC session happens to be set to.
     */
    private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;

    private final AccountRepository accountRepository;
    private final SnapshotService snapshotService;
    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;

    /**
     * Computes the user's total net worth -- the sum of every account they owned -- as of the
     * end of each month in {@code [startDate, endDate]}. Computed on-demand per request (no
     * persisted {@code account_snapshots} rows as a side effect, per this epic's own 2026-09-03
     * decision) by reusing {@link SnapshotService}'s existing balance math, not replaying
     * transactions independently.
     * <p>
     * An account is excluded from any month-end before it existed (its own {@code createdAt}),
     * rather than contributing a meaningless "balance" for a period before the account was real.
     *
     * @param userId    the authenticated user
     * @param startDate any date in the first month of the requested range
     * @param endDate   any date in the last month of the requested range
     * @return one data point per month-end in the range, oldest first
     */
    public List<NetWorthDataPointDto> getNetWorthOverTime(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Account> accounts = accountRepository.findAllByUserId(userId);

        List<NetWorthDataPointDto> series = new ArrayList<>();
        LocalDate monthCursor = startDate.withDayOfMonth(1);
        LocalDate lastMonthCursor = endDate.withDayOfMonth(1);

        while (!monthCursor.isAfter(lastMonthCursor)) {
            LocalDate monthEnd = monthCursor.withDayOfMonth(monthCursor.lengthOfMonth());

            BigDecimal netWorth = BigDecimal.ZERO;
            for (Account account : accounts) {
                if (existedAsOf(account, monthEnd)) {
                    netWorth = netWorth.add(snapshotService.calculateEndOfMonthBalance(account, monthEnd));
                }
            }
            series.add(new NetWorthDataPointDto(monthEnd, netWorth));

            monthCursor = monthCursor.plusMonths(1);
        }

        return series;
    }

    /**
     * Whether an account had already been created as of the given date -- an account created
     * partway through the requested range shouldn't contribute a computed "balance" to month-ends
     * before it existed.
     */
    private boolean existedAsOf(Account account, LocalDate date) {
        return account.getAudit() == null || account.getAudit().getCreatedAt() == null
                || !account.getAudit().getCreatedAt().toLocalDate().isAfter(date);
    }

    /**
     * Computes the Categories tab's spending breakdown for {@code [startDate, endDate]}, inclusive
     * of both endpoints from the caller's point of view (PF-823). Aggregated fully server-side --
     * no row cap, unlike the client-side approach it replaces.
     *
     * @param userId    the authenticated user
     * @param startDate the first date to include
     * @param endDate   the last date to include
     * @return one entry per category with spend in range (including uncategorized), highest total first
     */
    public List<CategoryReportDataDto> getCategoryReportData(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findCategoryReportData(userId, toStartOfDayUtc(startDate), toExclusiveEndUtc(endDate));
    }

    /**
     * Computes the Merchants tab's spending breakdown for {@code [startDate, endDate]}, inclusive
     * of both endpoints from the caller's point of view (PF-823). Aggregated fully server-side --
     * no row cap, unlike the client-side approach it replaces.
     *
     * @param userId    the authenticated user
     * @param startDate the first date to include
     * @param endDate   the last date to include
     * @return one entry per merchant with spend in range, highest total first
     */
    public List<MerchantReportDataDto> getMerchantReportData(Long userId, LocalDate startDate, LocalDate endDate) {
        return merchantRepository.findMerchantReportData(userId, toStartOfDayUtc(startDate), toExclusiveEndUtc(endDate));
    }

    /**
     * Computes the Cash Flow tab's monthly income/expense breakdown for {@code [startDate, endDate]},
     * inclusive of both endpoints from the caller's point of view (PF-823). Transfers are excluded
     * from both sides -- a transfer between the user's own accounts is neither real income nor real
     * spending.
     *
     * @param userId    the authenticated user
     * @param startDate the first date to include
     * @param endDate   the last date to include
     * @return one entry per month with matching activity, oldest first
     */
    public List<MonthlyReportDataDto> getMonthlyReportData(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findMonthlyIncomeExpense(userId, toStartOfDayUtc(startDate), toExclusiveEndUtc(endDate));
    }

    /**
     * Anchors a {@link LocalDate} to midnight UTC, for the inclusive start of a date-range filter.
     */
    private OffsetDateTime toStartOfDayUtc(LocalDate date) {
        return date.atStartOfDay(UTC_ZONE).toOffsetDateTime();
    }

    /**
     * Anchors the day after a {@link LocalDate} to midnight UTC, so a caller-inclusive end date can
     * be passed to a half-open {@code [start, end)} SQL range without excluding same-day activity.
     */
    private OffsetDateTime toExclusiveEndUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay(UTC_ZONE).toOffsetDateTime();
    }
}
