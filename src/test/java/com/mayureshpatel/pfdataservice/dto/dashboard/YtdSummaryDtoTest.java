package com.mayureshpatel.pfdataservice.dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("YtdSummaryDto Unit Tests")
class YtdSummaryDtoTest {

    @Nested
    @DisplayName("Builder and Structure")
    class BuilderTests {
        @Test
        @DisplayName("should correctly map all fields via builder")
        void shouldPopulateFieldsViaBuilder() {
            int year = 2024;
            BigDecimal totalIncome = new BigDecimal("12000.00");
            BigDecimal totalExpense = new BigDecimal("8000.00");
            BigDecimal netSavings = new BigDecimal("4000.00");
            BigDecimal avgSavingsRate = new BigDecimal("33.33");

            YtdSummaryDto dto = YtdSummaryDto.builder()
                    .year(year)
                    .totalIncome(totalIncome)
                    .totalExpense(totalExpense)
                    .netSavings(netSavings)
                    .avgSavingsRate(avgSavingsRate)
                    .build();

            assertEquals(year, dto.year());
            assertEquals(totalIncome, dto.totalIncome());
            assertEquals(totalExpense, dto.totalExpense());
            assertEquals(netSavings, dto.netSavings());
            assertEquals(avgSavingsRate, dto.avgSavingsRate());
        }

        @Test
        @DisplayName("should support toBuilder")
        void shouldSupportToBuilder() {
            YtdSummaryDto original = YtdSummaryDto.builder()
                    .year(2023)
                    .build();

            YtdSummaryDto updated = original.toBuilder()
                    .year(2024)
                    .build();

            assertEquals(2024, updated.year());
        }
    }
}
