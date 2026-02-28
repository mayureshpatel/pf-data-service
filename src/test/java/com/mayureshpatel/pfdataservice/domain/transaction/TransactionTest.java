package com.mayureshpatel.pfdataservice.domain.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction domain object tests")
class TransactionTest {

    @Nested
    @DisplayName("getNetChange")
    class GetNetChangeTest {

        @Test
        @DisplayName("should return positive amount for INCOME")
        void getNetChange_income_returnsPositive() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("100.00"));
            tx.setType(TransactionType.INCOME);

            assertThat(tx.getNetChange()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("should return negative amount for EXPENSE")
        void getNetChange_expense_returnsNegative() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("75.50"));
            tx.setType(TransactionType.EXPENSE);

            assertThat(tx.getNetChange()).isEqualByComparingTo("-75.50");
        }

        @Test
        @DisplayName("should return positive amount for TRANSFER_IN")
        void getNetChange_transferIn_returnsPositive() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("200.00"));
            tx.setType(TransactionType.TRANSFER_IN);

            assertThat(tx.getNetChange()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("should return negative amount for TRANSFER_OUT")
        void getNetChange_transferOut_returnsNegative() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("200.00"));
            tx.setType(TransactionType.TRANSFER_OUT);

            assertThat(tx.getNetChange()).isEqualByComparingTo("-200.00");
        }

        @Test
        @DisplayName("should return raw signed amount for ADJUSTMENT (positive)")
        void getNetChange_adjustmentPositive_returnsRawAmount() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("50.00"));
            tx.setType(TransactionType.ADJUSTMENT);

            assertThat(tx.getNetChange()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("should return raw signed amount for ADJUSTMENT (negative)")
        void getNetChange_adjustmentNegative_returnsRawAmount() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("-10.00"));
            tx.setType(TransactionType.ADJUSTMENT);

            assertThat(tx.getNetChange()).isEqualByComparingTo("-10.00");
        }

        @Test
        @DisplayName("should return zero when amount is null")
        void getNetChange_nullAmount_returnsZero() {
            Transaction tx = new Transaction();
            tx.setAmount(null);
            tx.setType(TransactionType.EXPENSE);

            assertThat(tx.getNetChange()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should use absolute value for EXPENSE even when amount is negative")
        void getNetChange_negativeAmountExpense_usesAbsoluteValue() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("-25.00"));
            tx.setType(TransactionType.EXPENSE);

            assertThat(tx.getNetChange()).isEqualByComparingTo("-25.00");
        }
    }
}
