package com.mayureshpatel.pfdataservice.dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies {@code CashFlowTrendDto}'s record accessor in {@link StructureTests}. */
@DisplayName("CashFlowTrendDto Unit Tests")
class CashFlowTrendDtoTest {

    /** {@code CashFlowTrendDto}'s record accessors map their constructor arguments 1:1. */
    @Nested
    @DisplayName("Record Structure")
    class StructureTests {
        @Test
        @DisplayName("should correctly map all fields")
        void shouldPopulateFields() {
            BigDecimal income = new BigDecimal("1000.00");
            BigDecimal expense = new BigDecimal("500.00");
            int month = 1;
            int year = 2024;

            CashFlowTrendDto dto = new CashFlowTrendDto(month, year, income, expense);

            assertEquals(month, dto.month());
            assertEquals(year, dto.year());
            assertEquals(income, dto.income());
            assertEquals(expense, dto.expense());
        }
    }
}
