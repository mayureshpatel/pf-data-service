package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryTotal;
import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardData_ShouldCalculateNetSavingsCorrectly() {
        // Arrange
        Long userId = 1L;
        int month = 10;
        int year = 2023;
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);

        when(transactionRepository.getSumByDateRange(eq(userId), eq(startDate), eq(endDate), eq(TransactionType.INCOME)))
                .thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.getSumByDateRange(eq(userId), eq(startDate), eq(endDate), eq(TransactionType.EXPENSE)))
                .thenReturn(new BigDecimal("3000.50"));

        List<CategoryTotal> breakdown = List.of(new CategoryTotal("Groceries", new BigDecimal("500")));
        when(transactionRepository.findCategoryTotals(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(breakdown);

        // Act
        DashboardData result = dashboardService.getDashboardData(userId, month, year);

        // Assert
        assertThat(result.totalIncome()).isEqualByComparingTo("5000.00");
        assertThat(result.totalExpense()).isEqualByComparingTo("3000.50");
        assertThat(result.netSavings()).isEqualByComparingTo("1999.50");
        assertThat(result.categoryBreakdown()).hasSize(1);
    }

    @Test
    void getDashboardData_ShouldHandleNullSumsAsZero() {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);

        when(transactionRepository.getSumByDateRange(eq(userId), eq(startDate), eq(endDate), any()))
                .thenReturn(null);

        when(transactionRepository.findCategoryTotals(any(), any(), any()))
                .thenReturn(List.of());

        // Act
        DashboardData result = dashboardService.getDashboardData(userId, 10, 2023);

        // Assert
        assertThat(result.totalIncome()).isEqualByComparingTo("0");
        assertThat(result.totalExpense()).isEqualByComparingTo("0");
        assertThat(result.netSavings()).isEqualByComparingTo("0");
    }
}
