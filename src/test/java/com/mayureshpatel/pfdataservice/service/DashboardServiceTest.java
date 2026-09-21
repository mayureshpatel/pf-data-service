package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.*;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private TransactionService transactionService;

    @InjectMocks private DashboardService dashboardService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getDashboardData")
    class GetDashboardDataTests {
        @Test
        @DisplayName("should aggregate income, expense and category breakdown")
        void shouldReturnDashboardData() {
            // arrange
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("5000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("3000.00"));
            
            CategoryDto catDto = CategoryDto.builder().name("Food").build();
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(new CategoryBreakdownDto(catDto, new BigDecimal("500.00"))));

            // act
            DashboardData result = dashboardService.getDashboardData(USER_ID, 3, 2026);

            // assert & verify
            assertEquals(new BigDecimal("5000.00"), result.totalIncome());
            assertEquals(new BigDecimal("3000.00"), result.totalExpense());
            assertEquals(new BigDecimal("2000.00"), result.netSavings());
            assertEquals(1, result.categoryBreakdown().size());
        }

        @Test
        @DisplayName("should query through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryThroughEndOfMonth() {
            // arrange -- a transaction timestamped later in the day on Mar 31 must still be
            // included; a midnight endDate would silently exclude it
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getDashboardData(USER_ID, 3, 2026);

            // assert & verify
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), any(), eq(expectedEnd), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), any(), eq(expectedEnd), eq(TransactionType.EXPENSE));
            verify(transactionRepository).findCategoryTotals(eq(USER_ID), any(), eq(expectedEnd));
        }
    }

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdownTests {
        @Test
        @DisplayName("should return breakdown from repository")
        void shouldReturnBreakdown() {
            // arrange
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of());

            // act
            List<CategoryBreakdownDto> result = dashboardService.getCategoryBreakdown(USER_ID, 3, 2026);

            // assert & verify
            assertNotNull(result);
            verify(transactionRepository).findCategoryTotals(eq(USER_ID), any(), any());
        }

        @Test
        @DisplayName("should query through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryThroughEndOfMonth() {
            // arrange
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getCategoryBreakdown(USER_ID, 3, 2026);

            // assert & verify
            verify(transactionRepository).findCategoryTotals(eq(USER_ID), any(), eq(expectedEnd));
        }
    }

    @Nested
    @DisplayName("getMerchantBreakdown")
    class GetMerchantBreakdownTests {
        @Test
        @DisplayName("should return merchant totals from repository")
        void shouldReturnMerchantTotals() {
            // arrange
            when(merchantRepository.findMerchantTotals(eq(USER_ID), any(), any())).thenReturn(List.of());

            // act
            List<MerchantBreakdownDto> result = dashboardService.getMerchantBreakdown(USER_ID, 3, 2026);

            // assert & verify
            assertNotNull(result);
            verify(merchantRepository).findMerchantTotals(eq(USER_ID), any(), any());
        }

        @Test
        @DisplayName("should query through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryThroughEndOfMonth() {
            // arrange
            when(merchantRepository.findMerchantTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getMerchantBreakdown(USER_ID, 3, 2026);

            // assert & verify
            verify(merchantRepository).findMerchantTotals(eq(USER_ID), any(), eq(expectedEnd));
        }
    }

    @Nested
    @DisplayName("getPulse")
    class GetPulseTests {
        @Test
        @DisplayName("should calculate savings rate and handle zero income")
        void shouldCalculatePulse() {
            // arrange
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("1000.00"), new BigDecimal("0.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("500.00"), new BigDecimal("200.00"));

            // act
            DashboardPulseDto result = dashboardService.getPulse(USER_ID, 3, 2026);

            // assert & verify
            assertEquals(0, new BigDecimal("50.0000").compareTo(result.currentSavingsRate()));
            assertEquals(BigDecimal.ZERO, result.previousSavingsRate());
        }

        @Test
        @DisplayName("should calculate pulse for arbitrary date range")
        void shouldCalculatePulseForRange() {
            // arrange
            when(transactionRepository.getSumByDateRange(anyLong(), any(), any(), any())).thenReturn(BigDecimal.TEN);

            // act
            DashboardPulseDto result = dashboardService.getPulse(USER_ID, OffsetDateTime.now().minusDays(10), OffsetDateTime.now());

            // assert & verify
            assertNotNull(result);
            verify(transactionRepository, times(4)).getSumByDateRange(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("should compute the previous period as exactly the prior calendar month, with no overlap (PF-195)")
        void shouldComputePreviousPeriodWithNoOverlap() {
            // arrange -- March 2026: previous period must be exactly [Feb 1, Feb 28], not
            // [Feb 1, Mar 30] -- the bug anchored endPrevious to endCurrent instead of startCurrent.
            // End boundary is end-of-day (23:59:59), not midnight -- PF-827; this assertion used to
            // encode the pre-fix midnight value as "correct" since PF-195's own fix was about the
            // start/end anchor, not this distinct end-of-day bug in the same method.
            OffsetDateTime expectedStartPrevious = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEndPrevious = OffsetDateTime.of(2026, 2, 28, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getPulse(USER_ID, 3, 2026);

            // assert & verify
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should compute the previous period correctly across a year boundary (PF-195)")
        void shouldComputePreviousPeriodAcrossYearBoundary() {
            // arrange -- January 2026: previous period must be exactly December 2025. End
            // boundary is end-of-day, not midnight -- PF-827.
            OffsetDateTime expectedStartPrevious = OffsetDateTime.of(2025, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEndPrevious = OffsetDateTime.of(2025, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getPulse(USER_ID, 1, 2026);

            // assert & verify
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should compute a non-overlapping, same-length previous period for the explicit date-range overload (PF-195)")
        void shouldComputePreviousPeriodWithNoOverlapForExplicitRange() {
            // arrange -- a 31-day range; the immediately preceding 31-day period must end the day
            // before startDate, not the day before endDate. End boundary is end-of-day, not
            // midnight -- PF-827.
            OffsetDateTime startDate = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime endDate = OffsetDateTime.of(2026, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedStartPrevious = OffsetDateTime.of(2026, 1, 29, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEndPrevious = OffsetDateTime.of(2026, 2, 28, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getPulse(USER_ID, startDate, endDate);

            // assert & verify
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should query the current period through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryCurrentPeriodThroughEndOfMonth() {
            // arrange
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), any())).thenReturn(BigDecimal.ZERO);
            OffsetDateTime expectedEndCurrent = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // act
            dashboardService.getPulse(USER_ID, 3, 2026);

            // assert & verify
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), any(), eq(expectedEndCurrent), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), any(), eq(expectedEndCurrent), eq(TransactionType.EXPENSE));
        }
    }

    @Nested
    @DisplayName("getCashFlowTrend")
    class GetCashFlowTrendTests {
        @Test
        @DisplayName("should fill missing months with zero values")
        void shouldReturnContinuousTrend() {
            // arrange
            // Return only one month of data: 2026-03 Income 100
            Object[] row = new Object[]{2026, 3, "INCOME", new BigDecimal("100.00")};
            when(transactionRepository.findMonthlySums(eq(USER_ID), any())).thenReturn(List.<Object[]>of(row));

            // act
            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            // assert & verify
            assertEquals(12, result.size());
            CashFlowTrendDto march = result.stream().filter(t -> t.month() == 3 && t.year() == 2026).findFirst().orElseThrow();
            assertEquals(new BigDecimal("100.00"), march.income());
            assertEquals(BigDecimal.ZERO, march.expense());
        }

        @Test
        @DisplayName("should sum multiple rows for same month (Income and Expense)")
        void shouldSumMonthlyTypes() {
            // arrange
            Object[] row1 = new Object[]{2026, 3, "INCOME", new BigDecimal("100.00")};
            Object[] row2 = new Object[]{2026, 3, "EXPENSE", new BigDecimal("50.00")};
            when(transactionRepository.findMonthlySums(eq(USER_ID), any())).thenReturn(List.<Object[]>of(row1, row2));

            // act
            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            // assert & verify
            CashFlowTrendDto march = result.stream().filter(t -> t.month() == 3 && t.year() == 2026).findFirst().orElseThrow();
            assertEquals(new BigDecimal("100.00"), march.income());
            assertEquals(new BigDecimal("50.00"), march.expense());
        }
    }

    @Nested
    @DisplayName("getYtdSummary")
    class GetYtdSummaryTests {
        @Test
        @DisplayName("should return summary for current year")
        void shouldReturnYtd() {
            // arrange
            int year = 2026;
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("10000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("8000.00"));

            // act
            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, year);

            // assert & verify
            assertEquals(new BigDecimal("10000.00"), result.totalIncome());
            assertEquals(new BigDecimal("2000.00"), result.netSavings());
            assertEquals(0, new BigDecimal("20.0000").compareTo(result.avgSavingsRate()));
        }

        @Test
        @DisplayName("bug regression: the current year's summary must stop at today, not run through "
                + "Dec 31 (PF-826) -- dead code left over from an incomplete LocalDate.now()-to-"
                + "OffsetDateTime refactor meant \"YTD\" for the current year silently included months "
                + "that haven't happened yet")
        void shouldCapCurrentYearAtToday() {
            // arrange
            int currentYear = LocalDate.now().getYear();
            ArgumentCaptor<OffsetDateTime> endCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(BigDecimal.ZERO);

            // act
            dashboardService.getYtdSummary(USER_ID, currentYear);

            // assert & verify -- the end bound's calendar date is today, not December 31st
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), any(), endCaptor.capture(), eq(TransactionType.INCOME));
            assertEquals(LocalDate.now(), endCaptor.getValue().toLocalDate());
        }

        @Test
        @DisplayName("bug regression: a past year's end boundary must be an exact multiple of "
                + "1000 nanoseconds (PF-828) -- ZonedDateTime.of(year, 12, 31, 23, 59, 59, "
                + "999_999_999, UTC) is finer than Postgres's microsecond-resolution timestamptz, "
                + "which rounds the 9-digit nanosecond value up to midnight of the following day: "
                + "confirmed live, '2024-12-31 23:59:59.999999999+00'::timestamptz literally "
                + "evaluates to '2025-01-01 00:00:00+00', silently pulling a Jan-1-next-year "
                + "transaction into the prior year's total")
        void shouldNotUseSubMicrosecondPrecisionForPastYearEndBoundary() {
            // arrange
            int year = 2024;
            ArgumentCaptor<OffsetDateTime> endCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(BigDecimal.ZERO);

            // act
            dashboardService.getYtdSummary(USER_ID, year);

            // assert & verify -- Postgres's timestamptz only has microsecond resolution; any nanosecond
            // remainder finer than that gets rounded rather than truncated, so the bound must land
            // exactly on a microsecond to round-trip unchanged
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), any(), endCaptor.capture(), eq(TransactionType.INCOME));
            OffsetDateTime end = endCaptor.getValue();
            assertEquals(year, end.getYear());
            assertEquals(0, end.getNano() % 1000,
                    "end boundary's nanosecond component must be a whole number of microseconds");
        }

        @Test
        @DisplayName("should return zeros for future year")
        void shouldHandleFutureYear() {
            // act
            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, 2030);

            // assert & verify
            assertEquals(2030, result.year());
            assertEquals(BigDecimal.ZERO, result.totalIncome());
        }

        @Test
        @DisplayName("should handle year in past correctly")
        void shouldHandlePastYear() {
            // arrange
            int year = 2020;
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("100.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("50.00"));

            // act
            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, year);

            // assert & verify
            assertEquals(year, result.year());
            assertEquals(new BigDecimal("100.00"), result.totalIncome());
        }
    }

    @Nested
    @DisplayName("getActionItems")
    class GetActionItemsTests {
        @Test
        @DisplayName("should include transfer and uncategorized review actions")
        void shouldReturnActions() {
            // arrange
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of(new TransferSuggestionDto(null, null, 0.9)));
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(new BigDecimal("150.00"));

            // act
            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            // assert & verify
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(a -> a.type() == ActionItemDto.ActionType.TRANSFER_REVIEW));
            assertTrue(result.stream().anyMatch(a -> a.type() == ActionItemDto.ActionType.UNCATEGORIZED));
        }

        @Test
        @DisplayName("bug regression: uncategorized route must use the 'categoryName' query param, "
                + "not 'category' -- TransactionsComponent.hydrateFromParams() only reads "
                + "'categoryName' off the URL, so the old route silently failed to filter anything "
                + "and the component's own URL-sync effect immediately wiped the unrecognized "
                + "'category=null' param back off the URL on load")
        void shouldUseCategoryNameQueryParamForUncategorizedRoute() {
            // arrange
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(new BigDecimal("150.00"));

            // act
            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            // assert & verify
            ActionItemDto uncategorized = result.stream()
                    .filter(a -> a.type() == ActionItemDto.ActionType.UNCATEGORIZED)
                    .findFirst()
                    .orElseThrow();
            assertEquals("/transactions?categoryName=null", uncategorized.route());
        }

        @Test
        @DisplayName("bug regression: UNCATEGORIZED item's count must be a real transaction count, "
                + "not the dollar sum truncated to a long (PF-825) -- confirmed live, this displayed "
                + "as \"425239 unresolved items\" in the real app for a $425,239.61 uncategorized total")
        void shouldReturnRealCountNotDollarSumForUncategorizedItem() {
            // arrange -- deliberately different numbers so the two are unmistakable if swapped
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(new BigDecimal("425239.61"));
            when(transactionRepository.getUncategorizedExpenseCount(USER_ID)).thenReturn(2935L);

            // act
            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            // assert & verify
            ActionItemDto uncategorized = result.stream()
                    .filter(a -> a.type() == ActionItemDto.ActionType.UNCATEGORIZED)
                    .findFirst()
                    .orElseThrow();
            assertEquals(2935L, uncategorized.count());
        }

        @Test
        @DisplayName("should return empty list if no actions needed")
        void shouldReturnEmpty() {
            // arrange
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(BigDecimal.ZERO);

            // act
            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }
}
