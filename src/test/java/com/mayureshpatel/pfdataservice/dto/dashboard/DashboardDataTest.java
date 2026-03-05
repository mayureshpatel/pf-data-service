package com.mayureshpatel.pfdataservice.dto.dashboard;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DashboardData Unit Tests")
class DashboardDataTest {

    @Nested
    @DisplayName("Builder and Structure")
    class BuilderTests {
        @Test
        @DisplayName("should correctly map all fields via builder")
        void shouldPopulateFieldsViaBuilder() {
            BigDecimal totalIncome = new BigDecimal("5000.00");
            BigDecimal totalExpense = new BigDecimal("3000.00");
            BigDecimal netSavings = new BigDecimal("2000.00");
            List<CategoryBreakdownDto> breakdown = Collections.emptyList();

            DashboardData dto = DashboardData.builder()
                    .totalIncome(totalIncome)
                    .totalExpense(totalExpense)
                    .netSavings(netSavings)
                    .categoryBreakdown(breakdown)
                    .build();

            assertEquals(totalIncome, dto.totalIncome());
            assertEquals(totalExpense, dto.totalExpense());
            assertEquals(netSavings, dto.netSavings());
            assertEquals(breakdown, dto.categoryBreakdown());
        }

        @Test
        @DisplayName("should correctly map all fields via constructor")
        void shouldPopulateFieldsViaConstructor() {
            BigDecimal totalIncome = new BigDecimal("5000.00");
            BigDecimal totalExpense = new BigDecimal("3000.00");
            BigDecimal netSavings = new BigDecimal("2000.00");
            List<CategoryBreakdownDto> breakdown = Collections.emptyList();

            DashboardData dto = new DashboardData(totalIncome, totalExpense, netSavings, breakdown);

            assertEquals(totalIncome, dto.totalIncome());
            assertEquals(totalExpense, dto.totalExpense());
            assertEquals(netSavings, dto.netSavings());
            assertEquals(breakdown, dto.categoryBreakdown());
        }
    }
}
