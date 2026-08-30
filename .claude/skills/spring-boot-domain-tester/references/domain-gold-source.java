package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gold Standard examples for Domain Object unit testing.
 * Demonstrates testing for logic, immutability, equality, and null safety.
 */
@DisplayName("Domain Object Gold Standard Tests")
class DomainGoldStandardTest {

    @Nested
    @DisplayName("Logic Testing (e.g., Transaction.getNetChange)")
    class LogicTests {

        @Test
        @DisplayName("should return positive amount for INCOME")
        void getNetChange_shouldBePositiveForIncome() {
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(new BigDecimal("100.00"))
                    .build();

            assertEquals(new BigDecimal("100.00"), transaction.getNetChange());
        }

        @Test
        @DisplayName("should return negative amount for EXPENSE")
        void getNetChange_shouldBeNegativeForExpense() {
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.EXPENSE)
                    .amount(new BigDecimal("50.00"))
                    .build();

            assertEquals(new BigDecimal("-50.00"), transaction.getNetChange());
        }

        @Test
        @DisplayName("should return ZERO if amount is null")
        void getNetChange_shouldHandleNullAmount() {
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(null)
                    .build();

            assertEquals(BigDecimal.ZERO, transaction.getNetChange());
        }
    }

    @Nested
    @DisplayName("Immutability Testing (e.g., Account.applyTransaction)")
    class ImmutabilityTests {

        @Test
        @DisplayName("applyTransaction should return a NEW instance with updated balance")
        void applyTransaction_shouldReturnNewInstance() {
            // Arrange
            Account original = Account.builder()
                    .currentBalance(new BigDecimal("100.00"))
                    .build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(new BigDecimal("50.00"))
                    .build();

            // Act
            Account updated = original.applyTransaction(transaction);

            // Assert
            assertNotSame(original, updated, "Should return a fresh instance");
            assertEquals(new BigDecimal("100.00"), original.getCurrentBalance(), "Original should remain unchanged");
            assertEquals(new BigDecimal("150.00"), updated.getCurrentBalance(), "New instance should have updated balance");
        }
    }

    @Nested
    @DisplayName("Equality Testing")
    class EqualityTests {

        @Test
        @DisplayName("Equality should be based strictly on ID")
        void equality_shouldBeBasedOnId() {
            Account a1 = Account.builder().id(1L).name("Checking").build();
            Account a2 = Account.builder().id(1L).name("Savings").build(); // Different name
            Account a3 = Account.builder().id(2L).name("Checking").build(); // Different ID

            assertEquals(a1, a2, "Objects with same ID must be equal");
            assertNotEquals(a1, a3, "Objects with different ID must not be equal");
            assertEquals(a1.hashCode(), a2.hashCode(), "Equal objects must have same hash code");
        }
    }
}
