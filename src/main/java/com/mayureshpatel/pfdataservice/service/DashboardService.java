package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.*;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregated data for the dashboard: spending breakdowns, income/expense pulse, cash-flow trend,
 * year-to-date summary, and actionable items. Most methods come in a (userId, month, year)
 * overload that resolves to a calendar month, and a (userId, startDate, endDate) overload for an
 * arbitrary explicit range -- the month/year overloads just compute the range and delegate.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId UTC_ZONE = java.time.ZoneOffset.UTC;

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;

    /**
     * Retrieves dashboard data for a given user, month, and year.
     *
     * @param userId the user identifier
     * @param month  the month of the dashboard data
     * @param year   the year of the dashboard data
     * @return dashboard data for the specified user, month, and year
     */
    public DashboardData getDashboardData(Long userId, int month, int year) {
        OffsetDateTime startOfMonth = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, UTC_ZONE).toOffsetDateTime();
        OffsetDateTime endOfMonth = endOfMonth(startOfMonth);

        BigDecimal totalIncome = this.transactionRepository.getSumByDateRange(userId, startOfMonth, endOfMonth, TransactionType.INCOME);
        BigDecimal totalExpenses = this.transactionRepository.getSumByDateRange(userId, startOfMonth, endOfMonth, TransactionType.EXPENSE);

        List<CategoryBreakdownDto> breakdown = this.transactionRepository.findCategoryTotals(userId, startOfMonth, endOfMonth);

        return DashboardData.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpenses)
                .netSavings(totalIncome.subtract(totalExpenses))
                .categoryBreakdown(breakdown)
                .build();
    }

    /**
     * Spending grouped by category for a calendar month.
     *
     * @param userId the user id
     * @param month  the month
     * @param year   the year
     * @return the category breakdown for that month
     */
    public List<CategoryBreakdownDto> getCategoryBreakdown(Long userId, int month, int year) {
        OffsetDateTime startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, UTC_ZONE).toOffsetDateTime();
        OffsetDateTime endDate = endOfMonth(startDate);

        return getCategoryBreakdown(userId, startDate, endDate);
    }

    /**
     * Spending grouped by category for an explicit date range.
     *
     * @param userId    the user id
     * @param startDate the range start (inclusive)
     * @param endDate   the range end (inclusive)
     * @return the category breakdown for that range
     */
    public List<CategoryBreakdownDto> getCategoryBreakdown(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return transactionRepository.findCategoryTotals(userId, startDate, endDate);
    }

    /**
     * Spending grouped by merchant for a calendar month.
     *
     * @param userId the user id
     * @param month  the month
     * @param year   the year
     * @return the merchant breakdown for that month
     */
    public List<MerchantBreakdownDto> getMerchantBreakdown(Long userId, int month, int year) {
        OffsetDateTime startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, UTC_ZONE).toOffsetDateTime();
        OffsetDateTime endDate = endOfMonth(startDate);

        return getMerchantBreakdown(userId, startDate, endDate);
    }

    /**
     * Spending grouped by merchant for an explicit date range.
     *
     * @param userId    the user id
     * @param startDate the range start (inclusive)
     * @param endDate   the range end (inclusive)
     * @return the merchant breakdown for that range
     */
    public List<MerchantBreakdownDto> getMerchantBreakdown(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return this.merchantRepository.findMerchantTotals(userId, startDate, endDate);
    }

    /**
     * Income/expense pulse for a calendar month, compared against the prior month.
     *
     * @param userId the user id
     * @param month  the month
     * @param year   the year
     * @return current vs. previous month income, expense, and savings rate
     */
    public DashboardPulseDto getPulse(Long userId, int month, int year) {
        OffsetDateTime startCurrent = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, UTC_ZONE).toOffsetDateTime();
        OffsetDateTime endCurrent = endOfMonth(startCurrent);

        // previous month
        OffsetDateTime startPrevious = startCurrent.minusMonths(1);
        OffsetDateTime endPrevious = startCurrent.minusDays(1);

        return calculatePulse(userId, startCurrent, endCurrent, startPrevious, endPrevious);
    }

    /**
     * Income/expense pulse for an explicit date range, compared against the immediately
     * preceding period of the same length.
     *
     * @param userId    the user id
     * @param startDate the range start (inclusive)
     * @param endDate   the range end (inclusive)
     * @return current vs. previous period income, expense, and savings rate
     */
    public DashboardPulseDto getPulse(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        // calculate duration to find equivalent previous period
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        OffsetDateTime startPrevious = startDate.minusDays(days);
        OffsetDateTime endPrevious = startDate.minusDays(1);

        return calculatePulse(userId, startDate, endDate, startPrevious, endPrevious);
    }

    /**
     * Computes income, expense, and savings rate for a current and a previous period.
     *
     * @param userId        the user id
     * @param startCurrent  the current period's start
     * @param endCurrent    the current period's end
     * @param startPrevious the previous period's start
     * @param endPrevious   the previous period's end
     * @return the combined current-vs-previous pulse data
     */
    private DashboardPulseDto calculatePulse(
            Long userId,
            OffsetDateTime startCurrent, OffsetDateTime endCurrent,
            OffsetDateTime startPrevious, OffsetDateTime endPrevious) {

        BigDecimal currentIncome = getSum(userId, startCurrent, endCurrent, TransactionType.INCOME);
        BigDecimal previousIncome = getSum(userId, startPrevious, endPrevious, TransactionType.INCOME);

        BigDecimal currentExpense = getSum(userId, startCurrent, endCurrent, TransactionType.EXPENSE);
        BigDecimal previousExpense = getSum(userId, startPrevious, endPrevious, TransactionType.EXPENSE);

        return new DashboardPulseDto(
                currentIncome,
                previousIncome,
                currentExpense,
                previousExpense,
                calculateSavingsRate(currentIncome, currentExpense),
                calculateSavingsRate(previousIncome, previousExpense)
        );
    }

    /**
     * Monthly income/expense totals for the trailing 12 months, with any month that has no
     * transactions filled in as zero so the series has no gaps.
     *
     * @param userId the user id
     * @return the 12-month cash-flow trend, oldest month first
     */
    public List<CashFlowTrendDto> getCashFlowTrend(Long userId) {
        LocalDate startDate = LocalDate.now().minusMonths(11).withDayOfMonth(1); // last 12 months
        List<Object[]> results = transactionRepository.findMonthlySums(userId, startDate);

        Map<String, CashFlowTrendDto> trendMap = results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue() + "-" + ((Number) row[1]).intValue(), // key: "2025-11"
                row -> new CashFlowTrendDto(
                        ((Number) row[1]).intValue(),
                        ((Number) row[0]).intValue(),
                        TransactionType.INCOME.name().equals(row[2]) ? (BigDecimal) row[3] : BigDecimal.ZERO,
                        TransactionType.EXPENSE.name().equals(row[2]) ? (BigDecimal) row[3] : BigDecimal.ZERO
                ),
                (d1, d2) -> new CashFlowTrendDto(
                        d1.month(),
                        d1.year(),
                        d1.income().add(d2.income()),
                        d1.expense().add(d2.expense())
                )
        ));

        // fill last 12 months to ensure continuity
        List<CashFlowTrendDto> trendList = new ArrayList<>();
        LocalDate iterator = startDate;
        LocalDate now = LocalDate.now();

        while (!iterator.isAfter(now)) {
            String key = iterator.getYear() + "-" + iterator.getMonthValue();
            trendList.add(trendMap.getOrDefault(key, new CashFlowTrendDto(
                    iterator.getMonthValue(),
                    iterator.getYear(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            )));
            iterator = iterator.plusMonths(1);
        }

        return trendList;
    }

    /**
     * Year-to-date income, expense, and savings for a year. A future year returns all zeros
     * rather than an error.
     *
     * @param userId the user id
     * @param year   the year to summarize
     * @return the year-to-date summary
     */
    public YtdSummaryDto getYtdSummary(Long userId, int year) {
//        LocalDate startYtd = LocalDate.of(year, 1, 1);
//        LocalDate endYtd = LocalDate.now();
        OffsetDateTime startYtd = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, UTC_ZONE).toOffsetDateTime();
        OffsetDateTime endYtd = ZonedDateTime.of(year, 12, 31, 23, 59, 59, 999999999, UTC_ZONE).toOffsetDateTime();

        if (endYtd.getYear() > year) {
            endYtd = ZonedDateTime.of(year, 12, 31, 23, 59, 59, 999999999, UTC_ZONE).toOffsetDateTime();
        }
        if (endYtd.getYear() < year) {
            return new YtdSummaryDto(year, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal income = getSum(userId, startYtd, endYtd, TransactionType.INCOME);
        BigDecimal expense = getSum(userId, startYtd, endYtd, TransactionType.EXPENSE);
        BigDecimal netSavings = income.subtract(expense);
        BigDecimal savingsRate = calculateSavingsRate(income, expense);

        return new YtdSummaryDto(year, income, expense, netSavings, savingsRate);
    }

    /**
     * Suggested follow-up actions for the user: potential transfers awaiting confirmation, and
     * uncategorized expenses worth reviewing. Either category is omitted if there's nothing to
     * report.
     *
     * @param userId the user id
     * @return the current action items
     */
    public List<ActionItemDto> getActionItems(Long userId) {
        List<ActionItemDto> actions = new ArrayList<>();

        // potential transfers
        int transferCount = transactionService.findPotentialTransfers(userId).size();
        if (transferCount > 0) {
            actions.add(new ActionItemDto(
                    ActionItemDto.ActionType.TRANSFER_REVIEW,
                    transferCount,
                    transferCount + " potential transfers found",
                    "/transactions?action=review-transfers"
            ));
        }

        // uncategorized expenses
        BigDecimal uncategorizedSum = transactionRepository.getUncategorizedExpenseTotals(userId);
        if (uncategorizedSum != null && uncategorizedSum.compareTo(BigDecimal.ZERO) > 0) {
            actions.add(new ActionItemDto(
                    ActionItemDto.ActionType.UNCATEGORIZED,
                    uncategorizedSum.longValue(),
                    "Uncategorized expenses found",
                    "/transactions?category=null"
            ));
        }

        return actions;
    }

    /**
     * The last moment of the calendar month a given month-start instant belongs to (23:59:59, not
     * midnight). {@code startOfMonth.plusMonths(1)} alone lands on next month's first midnight,
     * which silently excludes any transaction timestamped later in the day on the month's actual
     * last day when used as an inclusive range's end bound -- see PF-196.
     *
     * @param startOfMonth midnight on the first day of the month
     * @return the last second of that same month
     */
    private OffsetDateTime endOfMonth(OffsetDateTime startOfMonth) {
        return startOfMonth.plusMonths(1).minusSeconds(1);
    }

    /**
     * Sums transactions of one type over a date range, treating a null repository result as zero.
     *
     * @param userId the user id
     * @param start  the range start (inclusive)
     * @param end    the range end (inclusive)
     * @param type   the transaction type to sum
     * @return the sum, or zero if there were no matching transactions
     */
    private BigDecimal getSum(Long userId, OffsetDateTime start, OffsetDateTime end, TransactionType type) {
        BigDecimal sum = transactionRepository.getSumByDateRange(userId, start, end, type);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Computes savings rate as a percentage of income. Returns zero rather than dividing by zero
     * when income is zero.
     *
     * @param income  the period's income
     * @param expense the period's expense
     * @return the savings rate as a percentage (e.g. 25.0000 for 25%)
     */
    private BigDecimal calculateSavingsRate(BigDecimal income, BigDecimal expense) {
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return income.subtract(expense)
                .divide(income, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
