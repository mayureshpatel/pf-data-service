package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardData;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardData_ShouldCalculateTotals() {
        Long userId = 1L;
        int month = 1;
        int year = 2026;
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionRepository.getSumByDateRange(userId, start, end, TransactionType.INCOME))
                .thenReturn(BigDecimal.valueOf(1000));
        when(transactionRepository.getSumByDateRange(userId, start, end, TransactionType.EXPENSE))
                .thenReturn(BigDecimal.valueOf(500));
        
        List<CategoryBreakdownDto> breakdown = List.of(new CategoryBreakdownDto("Food", BigDecimal.valueOf(200)));
        when(transactionRepository.findCategoryTotals(userId, start, end)).thenReturn(breakdown);

        DashboardData result = dashboardService.getDashboardData(userId, month, year);

        assertThat(result.totalIncome()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.totalExpense()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(result.netSavings()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(result.categoryBreakdown()).hasSize(1);

        verify(transactionRepository).getSumByDateRange(userId, start, end, TransactionType.INCOME);
        verify(transactionRepository).getSumByDateRange(userId, start, end, TransactionType.EXPENSE);
        verify(transactionRepository).findCategoryTotals(userId, start, end);
    }

    @Test
    void getDashboardData_ShouldHandleNullSums() {
        Long userId = 1L;
        int month = 1;
        int year = 2026;
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionRepository.getSumByDateRange(userId, start, end, TransactionType.INCOME)).thenReturn(null);
        when(transactionRepository.getSumByDateRange(userId, start, end, TransactionType.EXPENSE)).thenReturn(null);
        when(transactionRepository.findCategoryTotals(userId, start, end)).thenReturn(Collections.emptyList());

        DashboardData result = dashboardService.getDashboardData(userId, month, year);

        assertThat(result.totalIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.totalExpense()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.netSavings()).isEqualTo(BigDecimal.ZERO);
    }
}