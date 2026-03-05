package com.mayureshpatel.pfdataservice.domain.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Transaction Domain Object Tests")
class TransactionTest {

    @Nested
    @DisplayName("getNetChange logic")
    class GetNetChangeTests {

        @Test
        @DisplayName("should return ZERO when amount is null")
        void shouldReturnZeroWhenAmountIsNull() {
            Transaction transaction = Transaction.builder().amount(null).build();
            assertEquals(BigDecimal.ZERO, transaction.getNetChange());
        }

        @Test
        @DisplayName("should return raw amount for ADJUSTMENT")
        void shouldReturnRawAmountForAdjustment() {
            Transaction t1 = Transaction.builder().type(TransactionType.ADJUSTMENT).amount(new BigDecimal("10.00")).build();
            Transaction t2 = Transaction.builder().type(TransactionType.ADJUSTMENT).amount(new BigDecimal("-10.00")).build();

            assertEquals(new BigDecimal("10.00"), t1.getNetChange());
            assertEquals(new BigDecimal("-10.00"), t2.getNetChange());
        }

        @Test
        @DisplayName("should return positive absolute for INCOME and TRANSFER_IN")
        void shouldReturnPositiveForIncomeTypes() {
            Transaction t1 = Transaction.builder().type(TransactionType.INCOME).amount(new BigDecimal("-50.00")).build();
            Transaction t2 = Transaction.builder().type(TransactionType.TRANSFER_IN).amount(new BigDecimal("50.00")).build();

            assertEquals(new BigDecimal("50.00"), t1.getNetChange());
            assertEquals(new BigDecimal("50.00"), t2.getNetChange());
        }

        @Test
        @DisplayName("should return negative absolute for EXPENSE and TRANSFER_OUT")
        void shouldReturnNegativeForExpenseTypes() {
            Transaction t1 = Transaction.builder().type(TransactionType.EXPENSE).amount(new BigDecimal("30.00")).build();
            Transaction t2 = Transaction.builder().type(TransactionType.TRANSFER_OUT).amount(new BigDecimal("-30.00")).build();

            assertEquals(new BigDecimal("-30.00"), t1.getNetChange());
            assertEquals(new BigDecimal("-30.00"), t2.getNetChange());
        }
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        Transaction t1 = Transaction.builder().id(1L).description("A").build();
        Transaction t2 = Transaction.builder().id(1L).description("B").build();
        Transaction t3 = Transaction.builder().id(2L).build();

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
