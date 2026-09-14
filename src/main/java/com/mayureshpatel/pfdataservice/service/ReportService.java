package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.report.NetWorthDataPointDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private final AccountRepository accountRepository;
    private final SnapshotService snapshotService;

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
}
