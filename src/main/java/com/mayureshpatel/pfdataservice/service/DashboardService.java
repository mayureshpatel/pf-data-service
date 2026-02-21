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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");

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
        OffsetDateTime startOfMonth = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, EASTERN).toOffsetDateTime();
        OffsetDateTime endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

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

    public List<CategoryBreakdownDto> getCategoryBreakdown(Long userId, int month, int year) {
        OffsetDateTime startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, EASTERN).toOffsetDateTime();
        OffsetDateTime endDate = startDate.plusMonths(1).minusDays(1);

        return getCategoryBreakdown(userId, startDate, endDate);
    }

    public List<CategoryBreakdownDto> getCategoryBreakdown(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return transactionRepository.findCategoryTotals(userId, startDate, endDate);
    }

    public List<MerchantBreakdownDto> getMerchantBreakdown(Long userId, int month, int year) {
        OffsetDateTime startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, EASTERN).toOffsetDateTime();
        OffsetDateTime endDate = startDate.plusMonths(1).minusDays(1);

        return getMerchantBreakdown(userId, startDate, endDate);
    }

    public List<MerchantBreakdownDto> getMerchantBreakdown(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return this.merchantRepository.findMerchantTotals(userId, startDate, endDate);
    }

    public DashboardPulseDto getPulse(Long userId, int month, int year) {
        OffsetDateTime startCurrent = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, EASTERN).toOffsetDateTime();
        OffsetDateTime endCurrent = startCurrent.plusMonths(1).minusDays(1);

        // previous month
        OffsetDateTime startPrevious = startCurrent.minusMonths(1);
        OffsetDateTime endPrevious = endCurrent.minusDays(1);

        return calculatePulse(userId, startCurrent, endCurrent, startPrevious, endPrevious);
    }

    public DashboardPulseDto getPulse(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        // Calculate duration to find equivalent previous period
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        OffsetDateTime startPrevious = startDate.minusDays(days);
        OffsetDateTime endPrevious = endDate.minusDays(1);

        return calculatePulse(userId, startDate, endDate, startPrevious, endPrevious);
    }

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

    public List<CashFlowTrendDto> getCashFlowTrend(Long userId) {
        LocalDate startDate = LocalDate.now().minusMonths(11).withDayOfMonth(1); // Last 12 months
        List<Object[]> results = transactionRepository.findMonthlySums(userId, startDate);

        // Map results to trend DTOs
        // Object[]: [year, month, type, total]

        Map<String, CashFlowTrendDto> trendMap = results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue() + "-" + ((Number) row[1]).intValue(), // Key: "2025-11"
                row -> new CashFlowTrendDto(
                        ((Number) row[1]).intValue(),
                        ((Number) row[0]).intValue(),
                        TransactionType.INCOME.equals(row[2]) ? (BigDecimal) row[3] : BigDecimal.ZERO,
                        TransactionType.EXPENSE.equals(row[2]) ? (BigDecimal) row[3] : BigDecimal.ZERO
                ),
                (d1, d2) -> new CashFlowTrendDto(
                        d1.month(),
                        d1.year(),
                        d1.income().add(d2.income()),
                        d1.expense().add(d2.expense())
                )
        ));

        // Fill last 12 months to ensure continuity
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

    public YtdSummaryDto getYtdSummary(Long userId, int year) {
        LocalDate startYtd = LocalDate.of(year, 1, 1);
        LocalDate endYtd = LocalDate.now();
        if (endYtd.getYear() > year) endYtd = LocalDate.of(year, 12, 31); // Past year full
        if (endYtd.getYear() < year)
            return new YtdSummaryDto(year, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal income = getSum(userId, startYtd, endYtd, TransactionType.INCOME);
        BigDecimal expense = getSum(userId, startYtd, endYtd, TransactionType.EXPENSE);
        BigDecimal netSavings = income.subtract(expense);
        BigDecimal savingsRate = calculateSavingsRate(income, expense);

        return new YtdSummaryDto(year, income, expense, netSavings, savingsRate);
    }

    public List<ActionItemDto> getActionItems(Long userId) {
        List<ActionItemDto> actions = new ArrayList<>();

        // 1. Potential Transfers
        int transferCount = transactionService.findPotentialTransfers(userId).size();
        if (transferCount > 0) {
            actions.add(new ActionItemDto(
                    ActionItemDto.ActionType.TRANSFER_REVIEW,
                    transferCount,
                    transferCount + " potential transfers found",
                    "/transactions?action=review-transfers"
            ));
        }

        // 2. Uncategorized Transactions
        BigDecimal uncategorizedSum = transactionRepository.getUncategorizedExpenseTotals(userId);
        // Note: Repository method returns sum, we might want count. Let's stick with sum for now or add count query later.
        // For MVP, if sum > 0, we show alert.
        if (uncategorizedSum != null && uncategorizedSum.compareTo(BigDecimal.ZERO) > 0) {
            actions.add(new ActionItemDto(
                    ActionItemDto.ActionType.UNCATEGORIZED,
                    1, // Using 1 as flag for now, ideally count query
                    "Uncategorized expenses found",
                    "/transactions?category=null"
            ));
        }

        return actions;
    }

    private BigDecimal getSum(Long userId, OffsetDateTime start, OffsetDateTime end, TransactionType type) {
        BigDecimal sum = transactionRepository.getSumByDateRange(userId, start, end, type);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private BigDecimal calculateSavingsRate(BigDecimal income, BigDecimal expense) {
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return income.subtract(expense)
                .divide(income, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
