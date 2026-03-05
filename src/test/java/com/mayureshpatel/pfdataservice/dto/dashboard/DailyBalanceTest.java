package com.mayureshpatel.pfdataservice.dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DailyBalance Unit Tests")
class DailyBalanceTest {

    @Nested
    @DisplayName("Record Structure")
    class StructureTests {
        @Test
        @DisplayName("should correctly map all fields")
        void shouldPopulateFields() {
            LocalDate date = LocalDate.of(2024, 1, 1);
            BigDecimal balance = new BigDecimal("100.00");

            DailyBalance dto = new DailyBalance(date, balance);

            assertEquals(date, dto.date());
            assertEquals(balance, dto.balance());
        }
    }
}
