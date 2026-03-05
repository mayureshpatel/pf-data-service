package com.mayureshpatel.pfdataservice.dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MonthlySpending Unit Tests")
class MonthlySpendingTest {

    @Nested
    @DisplayName("Record Structure")
    class StructureTests {
        @Test
        @DisplayName("should correctly map all fields")
        void shouldPopulateFields() {
            int year = 2024;
            int month = 1;
            BigDecimal total = new BigDecimal("1234.56");

            MonthlySpending dto = new MonthlySpending(year, month, total);

            assertEquals(year, dto.year());
            assertEquals(month, dto.month());
            assertEquals(total, dto.total());
        }
    }
}
