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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
            // Arrange
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("5000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("3000.00"));
            
            CategoryDto catDto = CategoryDto.builder().name("Food").build();
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(new CategoryBreakdownDto(catDto, new BigDecimal("500.00"))));

            // Act
            DashboardData result = dashboardService.getDashboardData(USER_ID, 3, 2026);

            // Assert
            assertEquals(new BigDecimal("5000.00"), result.totalIncome());
            assertEquals(new BigDecimal("3000.00"), result.totalExpense());
            assertEquals(new BigDecimal("2000.00"), result.netSavings());
            assertEquals(1, result.categoryBreakdown().size());
        }

        @Test
        @DisplayName("should query through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryThroughEndOfMonth() {
            // Arrange -- a transaction timestamped later in the day on Mar 31 must still be
            // included; a midnight endDate would silently exclude it
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getDashboardData(USER_ID, 3, 2026);

            // Assert
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
            // Arrange
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of());

            // Act
            List<CategoryBreakdownDto> result = dashboardService.getCategoryBreakdown(USER_ID, 3, 2026);

            // Assert
            assertNotNull(result);
            verify(transactionRepository).findCategoryTotals(eq(USER_ID), any(), any());
        }

        @Test
        @DisplayName("should query through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryThroughEndOfMonth() {
            // Arrange
            when(transactionRepository.findCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getCategoryBreakdown(USER_ID, 3, 2026);

            // Assert
            verify(transactionRepository).findCategoryTotals(eq(USER_ID), any(), eq(expectedEnd));
        }
    }

    @Nested
    @DisplayName("getMerchantBreakdown")
    class GetMerchantBreakdownTests {
        @Test
        @DisplayName("should return merchant totals from repository")
        void shouldReturnMerchantTotals() {
            // Arrange
            when(merchantRepository.findMerchantTotals(eq(USER_ID), any(), any())).thenReturn(List.of());

            // Act
            List<MerchantBreakdownDto> result = dashboardService.getMerchantBreakdown(USER_ID, 3, 2026);

            // Assert
            assertNotNull(result);
            verify(merchantRepository).findMerchantTotals(eq(USER_ID), any(), any());
        }

        @Test
        @DisplayName("should query through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryThroughEndOfMonth() {
            // Arrange
            when(merchantRepository.findMerchantTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getMerchantBreakdown(USER_ID, 3, 2026);

            // Assert
            verify(merchantRepository).findMerchantTotals(eq(USER_ID), any(), eq(expectedEnd));
        }
    }

    @Nested
    @DisplayName("getPulse")
    class GetPulseTests {
        @Test
        @DisplayName("should calculate savings rate and handle zero income")
        void shouldCalculatePulse() {
            // Arrange
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("1000.00"), new BigDecimal("0.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("500.00"), new BigDecimal("200.00"));

            // Act
            DashboardPulseDto result = dashboardService.getPulse(USER_ID, 3, 2026);

            // Assert
            assertEquals(0, new BigDecimal("50.0000").compareTo(result.currentSavingsRate()));
            assertEquals(BigDecimal.ZERO, result.previousSavingsRate());
        }

        @Test
        @DisplayName("should calculate pulse for arbitrary date range")
        void shouldCalculatePulseForRange() {
            // Arrange
            when(transactionRepository.getSumByDateRange(anyLong(), any(), any(), any())).thenReturn(BigDecimal.TEN);

            // Act
            DashboardPulseDto result = dashboardService.getPulse(USER_ID, OffsetDateTime.now().minusDays(10), OffsetDateTime.now());

            // Assert
            assertNotNull(result);
            verify(transactionRepository, times(4)).getSumByDateRange(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("should compute the previous period as exactly the prior calendar month, with no overlap (PF-195)")
        void shouldComputePreviousPeriodWithNoOverlap() {
            // Arrange -- March 2026: previous period must be exactly [Feb 1, Feb 28], not
            // [Feb 1, Mar 30] -- the bug anchored endPrevious to endCurrent instead of startCurrent
            OffsetDateTime expectedStartPrevious = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEndPrevious = OffsetDateTime.of(2026, 2, 28, 0, 0, 0, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getPulse(USER_ID, 3, 2026);

            // Assert
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should compute the previous period correctly across a year boundary (PF-195)")
        void shouldComputePreviousPeriodAcrossYearBoundary() {
            // Arrange -- January 2026: previous period must be exactly December 2025
            OffsetDateTime expectedStartPrevious = OffsetDateTime.of(2025, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEndPrevious = OffsetDateTime.of(2025, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getPulse(USER_ID, 1, 2026);

            // Assert
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should compute a non-overlapping, same-length previous period for the explicit date-range overload (PF-195)")
        void shouldComputePreviousPeriodWithNoOverlapForExplicitRange() {
            // Arrange -- a 31-day range; the immediately preceding 31-day period must end the day
            // before startDate, not the day before endDate
            OffsetDateTime startDate = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime endDate = OffsetDateTime.of(2026, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedStartPrevious = OffsetDateTime.of(2026, 1, 29, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEndPrevious = OffsetDateTime.of(2026, 2, 28, 0, 0, 0, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getPulse(USER_ID, startDate, endDate);

            // Assert
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.INCOME));
            verify(transactionRepository).getSumByDateRange(eq(USER_ID), eq(expectedStartPrevious), eq(expectedEndPrevious), eq(TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should query the current period through the end of the month's last day, not midnight (PF-196)")
        void shouldQueryCurrentPeriodThroughEndOfMonth() {
            // Arrange
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), any())).thenReturn(BigDecimal.ZERO);
            OffsetDateTime expectedEndCurrent = OffsetDateTime.of(2026, 3, 31, 23, 59, 59, 0, ZoneOffset.UTC);

            // Act
            dashboardService.getPulse(USER_ID, 3, 2026);

            // Assert
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
            // Arrange
            // Return only one month of data: 2026-03 Income 100
            Object[] row = new Object[]{2026, 3, "INCOME", new BigDecimal("100.00")};
            when(transactionRepository.findMonthlySums(eq(USER_ID), any())).thenReturn(List.<Object[]>of(row));

            // Act
            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            // Assert
            assertEquals(12, result.size());
            CashFlowTrendDto march = result.stream().filter(t -> t.month() == 3 && t.year() == 2026).findFirst().orElseThrow();
            assertEquals(new BigDecimal("100.00"), march.income());
            assertEquals(BigDecimal.ZERO, march.expense());
        }

        @Test
        @DisplayName("should sum multiple rows for same month (Income and Expense)")
        void shouldSumMonthlyTypes() {
            // Arrange
            Object[] row1 = new Object[]{2026, 3, "INCOME", new BigDecimal("100.00")};
            Object[] row2 = new Object[]{2026, 3, "EXPENSE", new BigDecimal("50.00")};
            when(transactionRepository.findMonthlySums(eq(USER_ID), any())).thenReturn(List.<Object[]>of(row1, row2));

            // Act
            List<CashFlowTrendDto> result = dashboardService.getCashFlowTrend(USER_ID);

            // Assert
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
            // Arrange
            int year = 2026;
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("10000.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("8000.00"));

            // Act
            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, year);

            // Assert
            assertEquals(new BigDecimal("10000.00"), result.totalIncome());
            assertEquals(new BigDecimal("2000.00"), result.netSavings());
            assertEquals(0, new BigDecimal("20.0000").compareTo(result.avgSavingsRate()));
        }

        @Test
        @DisplayName("should return zeros for future year")
        void shouldHandleFutureYear() {
            // Act
            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, 2030);

            // Assert
            assertEquals(2030, result.year());
            assertEquals(BigDecimal.ZERO, result.totalIncome());
        }

        @Test
        @DisplayName("should handle year in past correctly")
        void shouldHandlePastYear() {
            // Arrange
            int year = 2020;
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.INCOME))).thenReturn(new BigDecimal("100.00"));
            when(transactionRepository.getSumByDateRange(eq(USER_ID), any(), any(), eq(TransactionType.EXPENSE))).thenReturn(new BigDecimal("50.00"));

            // Act
            YtdSummaryDto result = dashboardService.getYtdSummary(USER_ID, year);

            // Assert
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
            // Arrange
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of(new TransferSuggestionDto(null, null, 0.9)));
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(new BigDecimal("150.00"));

            // Act
            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(a -> a.type() == ActionItemDto.ActionType.TRANSFER_REVIEW));
            assertTrue(result.stream().anyMatch(a -> a.type() == ActionItemDto.ActionType.UNCATEGORIZED));
        }

        @Test
        @DisplayName("should return empty list if no actions needed")
        void shouldReturnEmpty() {
            // Arrange
            when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.getUncategorizedExpenseTotals(USER_ID)).thenReturn(BigDecimal.ZERO);

            // Act
            List<ActionItemDto> result = dashboardService.getActionItems(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
        }
    }
}
