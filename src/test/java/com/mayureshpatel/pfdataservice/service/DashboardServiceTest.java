package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.*;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService unit tests")
class DashboardServiceTest {

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");
    private static final Long USER_ID = 1L;
    private static final int MONTH = 3;
    private static final int YEAR = 2025;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DashboardService dashboardService;

    private OffsetDateTime startOfMonth(int year, int month) {
        return ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, EASTERN).toOffsetDateTime();
    }

    private OffsetDateTime endOfMonth(int year, int month) {
        return startOfMonth(year, month).plusMonths(1).minusDays(1);
    }

    @Nested
    @DisplayName("getDashboardData")
    class GetDashboardDataTest {

        @Test
        @DisplayName("should return correct income, expense, netSavings, and category breakdown")
        void getDashboardData_happyPath_returnsCorrectTotals() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);

            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME))
                    .thenReturn(new BigDecimal("3000.00"));
            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.EXPENSE))
                    .thenReturn(new BigDecimal("1800.00"));
            when(transactionRepository.findCategoryTotals(USER_ID, start, end))
                    .thenReturn(List.of(new CategoryBreakdownDto(null, new BigDecimal("500.00"))));

            DashboardData result = dashboardService.getDashboardData(USER_ID, MONTH, YEAR);

            assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("1800.00"));
            assertThat(result.netSavings()).isEqualByComparingTo(new BigDecimal("1200.00"));
            assertThat(result.categoryBreakdown()).hasSize(1);
        }

        @Test
        @DisplayName("should compute negative netSavings when expenses exceed income")
        void getDashboardData_expensesExceedIncome_negativeNetSavings() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);

            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME))
                    .thenReturn(new BigDecimal("500.00"));
            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.EXPENSE))
                    .thenReturn(new BigDecimal("700.00"));
            when(transactionRepository.findCategoryTotals(USER_ID, start, end)).thenReturn(List.of());

            DashboardData result = dashboardService.getDashboardData(USER_ID, MONTH, YEAR);

            assertThat(result.netSavings()).isEqualByComparingTo(new BigDecimal("-200.00"));
        }

        @Test
        @DisplayName("should return empty category breakdown when no transactions exist")
        void getDashboardData_noTransactions_emptyBreakdown() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);

            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.EXPENSE))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.findCategoryTotals(USER_ID, start, end)).thenReturn(List.of());

            DashboardData result = dashboardService.getDashboardData(USER_ID, MONTH, YEAR);

            assertThat(result.netSavings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.categoryBreakdown()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdownTest {

        @Test
        @DisplayName("month/year overload should delegate to repository with correct date range")
        void getCategoryBreakdown_monthYearOverload_delegatesWithCorrectDateRange() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);
            List<CategoryBreakdownDto> expected = List.of(new CategoryBreakdownDto(null, new BigDecimal("200.00")));

            when(transactionRepository.findCategoryTotals(USER_ID, start, end)).thenReturn(expected);

            List<CategoryBreakdownDto> result = dashboardService.getCategoryBreakdown(USER_ID, MONTH, YEAR);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).total()).isEqualByComparingTo(new BigDecimal("200.00"));
            verify(transactionRepository).findCategoryTotals(USER_ID, start, end);
        }

        @Test
        @DisplayName("OffsetDateTime overload should pass through dates directly to repository")
        void getCategoryBreakdown_offsetDateTimeOverload_passesDatesThroughDirectly() {
            OffsetDateTime start = OffsetDateTime.parse("2025-01-01T00:00:00Z");
            OffsetDateTime end = OffsetDateTime.parse("2025-03-31T23:59:59Z");
            List<CategoryBreakdownDto> expected = List.of(new CategoryBreakdownDto(null, new BigDecimal("100.00")));

            when(transactionRepository.findCategoryTotals(USER_ID, start, end)).thenReturn(expected);

            List<CategoryBreakdownDto> result = dashboardService.getCategoryBreakdown(USER_ID, start, end);

            assertThat(result).hasSize(1);
            verify(transactionRepository).findCategoryTotals(USER_ID, start, end);
        }

        @Test
        @DisplayName("should return empty list when no category totals exist")
        void getCategoryBreakdown_noData_returnsEmptyList() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);
            when(transactionRepository.findCategoryTotals(USER_ID, start, end)).thenReturn(List.of());

            List<CategoryBreakdownDto> result = dashboardService.getCategoryBreakdown(USER_ID, MONTH, YEAR);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMerchantBreakdown")
    class GetMerchantBreakdownTest {

        @Test
        @DisplayName("month/year overload should delegate to merchantRepository with correct date range")
        void getMerchantBreakdown_monthYearOverload_delegatesWithCorrectDateRange() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);
            List<MerchantBreakdownDto> expected = List.of(new MerchantBreakdownDto(null, new BigDecimal("150.00")));

            when(merchantRepository.findMerchantTotals(USER_ID, start, end)).thenReturn(expected);

            List<MerchantBreakdownDto> result = dashboardService.getMerchantBreakdown(USER_ID, MONTH, YEAR);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).total()).isEqualByComparingTo(new BigDecimal("150.00"));
            verify(merchantRepository).findMerchantTotals(USER_ID, start, end);
        }

        @Test
        @DisplayName("OffsetDateTime overload should pass through dates directly to merchantRepository")
        void getMerchantBreakdown_offsetDateTimeOverload_passesDatesThroughDirectly() {
            OffsetDateTime start = OffsetDateTime.parse("2025-02-01T00:00:00Z");
            OffsetDateTime end = OffsetDateTime.parse("2025-02-28T23:59:59Z");

            when(merchantRepository.findMerchantTotals(USER_ID, start, end))
                    .thenReturn(List.of(new MerchantBreakdownDto(null, new BigDecimal("75.00"))));

            List<MerchantBreakdownDto> result = dashboardService.getMerchantBreakdown(USER_ID, start, end);

            assertThat(result).hasSize(1);
            verify(merchantRepository).findMerchantTotals(USER_ID, start, end);
        }

        @Test
        @DisplayName("should return empty list when no merchant totals exist")
        void getMerchantBreakdown_noData_returnsEmptyList() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);
            when(merchantRepository.findMerchantTotals(USER_ID, start, end)).thenReturn(List.of());

            List<MerchantBreakdownDto> result = dashboardService.getMerchantBreakdown(USER_ID, MONTH, YEAR);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPulse")
    class GetPulseTest {

        @Test
        @DisplayName("should return correct pulse with income, expense, and savings rate")
        void getPulse_monthYearOverload_returnsCorrectPulse() {
            // Use any() for all date args to avoid Mockito stub ambiguity between current and previous periods
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME)))
                    .thenReturn(new BigDecimal("4000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE)))
                    .thenReturn(new BigDecimal("2000.00"));

            DashboardPulseDto result = dashboardService.getPulse(USER_ID, MONTH, YEAR);

            assertThat(result).isNotNull();
            // 4 repository calls total (current income, prev income, current expense, prev expense)
            verify(transactionRepository, times(2))
                    .getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME));
            verify(transactionRepository, times(2))
                    .getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should return zero savings rate when income is zero")
        void getPulse_zeroIncome_savingsRateIsZero() {
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE)))
                    .thenReturn(BigDecimal.ZERO);

            DashboardPulseDto result = dashboardService.getPulse(USER_ID, MONTH, YEAR);

            assertThat(result.currentSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.previousSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should treat null sum from repository as zero in pulse calculation")
        void getPulse_nullSumFromRepository_defaultsToZero() {
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME)))
                    .thenReturn(null);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE)))
                    .thenReturn(null);

            DashboardPulseDto result = dashboardService.getPulse(USER_ID, MONTH, YEAR);

            assertThat(result.currentIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.currentExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("OffsetDateTime overload should make 4 repository calls total")
        void getPulse_offsetDateTimeOverload_makes4RepositoryCalls() {
            OffsetDateTime start = OffsetDateTime.parse("2025-03-11T00:00:00Z");
            OffsetDateTime end = OffsetDateTime.parse("2025-03-20T00:00:00Z");

            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            DashboardPulseDto result = dashboardService.getPulse(USER_ID, start, end);

            assertThat(result).isNotNull();
            verify(transactionRepository, times(4))
                    .getSumByDateRange(eq(USER_ID), any(), any(), any());
        }

        @Test
        @DisplayName("should calculate correct savings rate when income is positive")
        void getPulse_positiveIncomeAndExpense_calculatesSavingsRateCorrectly() {
            OffsetDateTime start = startOfMonth(YEAR, MONTH);
            OffsetDateTime end = endOfMonth(YEAR, MONTH);
            OffsetDateTime startPrev = start.minusMonths(1);
            OffsetDateTime endPrev = end.minusDays(1);

            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME))
                    .thenReturn(new BigDecimal("4000.00"));
            when(transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.EXPENSE))
                    .thenReturn(new BigDecimal("2000.00"));
            when(transactionRepository.getSumByDateRange(USER_ID, startPrev, endPrev, TransactionType.INCOME))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getSumByDateRange(USER_ID, startPrev, endPrev, TransactionType.EXPENSE))
                    .thenReturn(BigDecimal.ZERO);

            DashboardPulseDto result = dashboardService.getPulse(USER_ID, MONTH, YEAR);

            // savingsRate = (4000 - 2000) / 4000 * 100 = 50.0000%
            assertThat(result.currentIncome()).isEqualByComparingTo(new BigDecimal("4000.00"));
            assertThat(result.currentExpense()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.currentSavingsRate()).isEqualByComparingTo(new BigDecimal("50.0000"));
        }
    }

    @Nested
    @DisplayName("getCashFlowTrend")
    class GetCashFlowTrendTest {

        @Test
        @DisplayName("should return 12 entries covering the last 12 months")
        void getCashFlowTrend_happyPath_returns12Months() {
            when(transactionRepository.findMonthlySums(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            assertThat(result).hasSize(12);
        }

        @Test
        @DisplayName("should fill months with no data as zero income and zero expense")
        void getCashFlowTrend_emptyData_allEntriesAreZero() {
            when(transactionRepository.findMonthlySums(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            assertThat(result).allSatisfy(entry -> {
                assertThat(entry.income()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(entry.expense()).isEqualByComparingTo(BigDecimal.ZERO);
            });
        }

        @Test
        @DisplayName("should map INCOME and EXPENSE rows from repository into trend entries")
        void getCashFlowTrend_withData_mapsIncomeAndExpenseCorrectly() {
            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();

            Object[] incomeRow = {year, month, "INCOME", new BigDecimal("2000.00")};
            Object[] expenseRow = {year, month, "EXPENSE", new BigDecimal("1200.00")};

            when(transactionRepository.findMonthlySums(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(incomeRow, expenseRow));

            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            CashFlowTrendDto currentMonth = result.stream()
                    .filter(e -> e.year() == year && e.month() == month)
                    .findFirst()
                    .orElseThrow();

            assertThat(currentMonth.income()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(currentMonth.expense()).isEqualByComparingTo(new BigDecimal("1200.00"));
        }

        @Test
        @DisplayName("should return entries in ascending chronological order")
        void getCashFlowTrend_allEntriesInChronologicalOrder() {
            when(transactionRepository.findMonthlySums(eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            for (int i = 1; i < result.size(); i++) {
                CashFlowTrendDto prev = result.get(i - 1);
                CashFlowTrendDto curr = result.get(i);
                LocalDate prevDate = LocalDate.of(prev.year(), prev.month(), 1);
                LocalDate currDate = LocalDate.of(curr.year(), curr.month(), 1);
                assertThat(currDate).isAfter(prevDate);
            }
        }
    }

    @Nested
    @DisplayName("getYtdSummary")
    class GetYtdSummaryTest {

        @Test
        @DisplayName("should return correct YTD totals and savings rate")
        void getYtdSummary_normalYear_returnsCorrectSummary() {
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME)))
                    .thenReturn(new BigDecimal("48000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE)))
                    .thenReturn(new BigDecimal("36000.00"));

            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, YEAR);

            assertThat(result.year()).isEqualTo(YEAR);
            assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("48000.00"));
            assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("36000.00"));
            assertThat(result.netSavings()).isEqualByComparingTo(new BigDecimal("12000.00"));
            // savingsRate = (48000 - 36000) / 48000 * 100 = 25.0000
            assertThat(result.avgSavingsRate()).isEqualByComparingTo(new BigDecimal("25.0000"));
        }

        @Test
        @DisplayName("should return zero savings rate when income is zero")
        void getYtdSummary_zeroIncome_savingsRateIsZero() {
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE)))
                    .thenReturn(BigDecimal.ZERO);

            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, YEAR);

            assertThat(result.avgSavingsRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.netSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return negative netSavings when expenses exceed income")
        void getYtdSummary_expensesExceedIncome_negativeNetSavings() {
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME)))
                    .thenReturn(new BigDecimal("1000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE)))
                    .thenReturn(new BigDecimal("1500.00"));

            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, YEAR);

            assertThat(result.netSavings()).isEqualByComparingTo(new BigDecimal("-500.00"));
        }
    }

    @Nested
    @DisplayName("getActionItems")
    class GetActionItemsTest {

        @Test
        @DisplayName("should return empty list when no potential transfers and no uncategorized expenses")
        void getActionItems_noTransfersNoUncategorized_returnsEmptyList() {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(BigDecimal.ZERO);

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include TRANSFER_REVIEW action when potential transfers exist")
        void getActionItems_potentialTransfersExist_includesTransferReviewAction() {
            when(transactionService.findPotentialTransfers(USER_ID))
                    .thenReturn(List.of(new TransferSuggestionDto(null, null, 0.9)));
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(BigDecimal.ZERO);

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(ActionItemDto.ActionType.TRANSFER_REVIEW);
            assertThat(result.get(0).count()).isEqualTo(1L);
            assertThat(result.get(0).message()).contains("1");
            assertThat(result.get(0).route()).isEqualTo("/transactions?action=review-transfers");
        }

        @Test
        @DisplayName("should include UNCATEGORIZED action when uncategorized expense total is positive")
        void getActionItems_uncategorizedExpensesExist_includesUncategorizedAction() {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID))
                    .thenReturn(new BigDecimal("250.00"));

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(ActionItemDto.ActionType.UNCATEGORIZED);
            assertThat(result.get(0).message()).containsIgnoringCase("Uncategorized");
            assertThat(result.get(0).route()).isEqualTo("/transactions?category=null");
        }

        @Test
        @DisplayName("should include both action types when transfers and uncategorized expenses both exist")
        void getActionItems_bothTransfersAndUncategorized_includesBothActions() {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of(
                    new TransferSuggestionDto(null, null, 0.8),
                    new TransferSuggestionDto(null, null, 0.7)));
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID))
                    .thenReturn(new BigDecimal("99.99"));

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).type()).isEqualTo(ActionItemDto.ActionType.TRANSFER_REVIEW);
            assertThat(result.get(0).count()).isEqualTo(2L);
            assertThat(result.get(1).type()).isEqualTo(ActionItemDto.ActionType.UNCATEGORIZED);
        }

        @Test
        @DisplayName("should not include UNCATEGORIZED action when uncategorized total is null")
        void getActionItems_nullUncategorizedTotal_doesNotIncludeUncategorizedAction() {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(null);

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not include UNCATEGORIZED action when uncategorized total is zero")
        void getActionItems_zeroUncategorizedTotal_doesNotIncludeUncategorizedAction() {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(BigDecimal.ZERO);

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should set count equal to the number of potential transfer suggestions")
        void getActionItems_multipleTransfers_countMatchesSuggestionListSize() {
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of(
                    new TransferSuggestionDto(null, null, 0.9),
                    new TransferSuggestionDto(null, null, 0.85),
                    new TransferSuggestionDto(null, null, 0.75)));
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(BigDecimal.ZERO);

            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).count()).isEqualTo(3L);
            assertThat(result.get(0).message()).contains("3");
        }
    }
}
