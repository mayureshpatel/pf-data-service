package com.mayureshpatel.pfdataservice.dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DashboardPulseDto Unit Tests")
class DashboardPulseDtoTest {

    @Nested
    @DisplayName("Builder and Structure")
    class BuilderTests {
        @Test
        @DisplayName("should correctly map all fields via builder")
        void shouldPopulateFieldsViaBuilder() {
            BigDecimal currentIncome = new BigDecimal("1000.00");
            BigDecimal previousIncome = new BigDecimal("900.00");
            BigDecimal currentExpense = new BigDecimal("500.00");
            BigDecimal previousExpense = new BigDecimal("450.00");
            BigDecimal currentSavingsRate = new BigDecimal("50.00");
            BigDecimal previousSavingsRate = new BigDecimal("50.00");

            DashboardPulseDto dto = DashboardPulseDto.builder()
                    .currentIncome(currentIncome)
                    .previousIncome(previousIncome)
                    .currentExpense(currentExpense)
                    .previousExpense(previousExpense)
                    .currentSavingsRate(currentSavingsRate)
                    .previousSavingsRate(previousSavingsRate)
                    .build();

            assertEquals(currentIncome, dto.currentIncome());
            assertEquals(previousIncome, dto.previousIncome());
            assertEquals(currentExpense, dto.currentExpense());
            assertEquals(previousExpense, dto.previousExpense());
            assertEquals(currentSavingsRate, dto.currentSavingsRate());
            assertEquals(previousSavingsRate, dto.previousSavingsRate());
        }

        @Test
        @DisplayName("should support toBuilder")
        void shouldSupportToBuilder() {
            DashboardPulseDto original = DashboardPulseDto.builder()
                    .currentIncome(BigDecimal.TEN)
                    .build();
            
            DashboardPulseDto updated = original.toBuilder()
                    .currentIncome(BigDecimal.ONE)
                    .build();

            assertEquals(BigDecimal.ONE, updated.currentIncome());
        }
    }
}
