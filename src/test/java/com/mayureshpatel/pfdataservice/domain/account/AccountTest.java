package com.mayureshpatel.pfdataservice.domain.account;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Account Domain Object Tests")
class AccountTest {

    @Test
    @DisplayName("Constructor should set default balance to ZERO")
    void constructor_shouldSetDefaultBalance() {
        Account account = Account.builder().build();
        assertEquals(BigDecimal.ZERO, account.getCurrentBalance());
    }

    @Nested
    @DisplayName("Transaction Application (Domain Object)")
    class ApplyTransactionDomainTests {

        @Test
        @DisplayName("applyTransaction should increase balance for INCOME")
        void applyTransaction_shouldIncreaseForIncome() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(new BigDecimal("50.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("150.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("applyTransaction should decrease balance for EXPENSE")
        void applyTransaction_shouldDecreaseForExpense() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.EXPENSE)
                    .amount(new BigDecimal("30.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("70.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("applyTransaction should handle ADJUSTMENT (positive)")
        void applyTransaction_shouldHandlePositiveAdjustment() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.ADJUSTMENT)
                    .amount(new BigDecimal("10.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("110.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("applyTransaction should handle null balance by defaulting to ZERO")
        void applyTransaction_shouldHandleNullBalance() {
            // arrange - forcing null balance via toBuilder
            Account account = Account.builder().currentBalance(null).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(new BigDecimal("50.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("50.00"), updatedAccount.getCurrentBalance());
        }
    }

    @Nested
    @DisplayName("Transaction Undo (Domain Object)")
    class UndoTransactionDomainTests {

        @Test
        @DisplayName("undoTransaction should reverse INCOME application")
        void undoTransaction_shouldReverseIncome() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("150.00")).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(new BigDecimal("50.00"))
                    .build();

            // act
            Account updatedAccount = account.undoTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("100.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("undoTransaction should reverse EXPENSE application")
        void undoTransaction_shouldReverseExpense() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("70.00")).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.EXPENSE)
                    .amount(new BigDecimal("30.00"))
                    .build();

            // act
            Account updatedAccount = account.undoTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("100.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("undoTransaction should handle null balance")
        void undoTransaction_shouldHandleNullBalance() {
            // arrange
            Account account = Account.builder().currentBalance(null).build();
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INCOME)
                    .amount(new BigDecimal("50.00"))
                    .build();

            // act
            Account updatedAccount = account.undoTransaction(transaction);

            // assert & verify
            assertEquals(new BigDecimal("-50.00"), updatedAccount.getCurrentBalance());
        }
    }

    @Nested
    @DisplayName("Transaction Application (DTO)")
    class ApplyTransactionDtoTests {

        @Test
        @DisplayName("applyTransaction (DTO) should handle INCOME correctly")
        void applyTransactionDto_shouldAddIncome() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .type("INCOME")
                    .amount(new BigDecimal("25.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(request);

            // assert & verify
            assertEquals(new BigDecimal("125.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("applyTransaction (DTO) should handle EXPENSE correctly")
        void applyTransactionDto_shouldSubtractExpense() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .type("EXPENSE")
                    .amount(new BigDecimal("25.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(request);

            // assert & verify
            assertEquals(new BigDecimal("75.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("applyTransaction (DTO) should handle null balance")
        void applyTransactionDto_shouldHandleNullBalance() {
            // arrange
            Account account = Account.builder().currentBalance(null).build();
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .type("INCOME")
                    .amount(new BigDecimal("25.00"))
                    .build();

            // act
            Account updatedAccount = account.applyTransaction(request);

            // assert & verify
            assertEquals(new BigDecimal("25.00"), updatedAccount.getCurrentBalance());
        }
    }

    @Nested
    @DisplayName("Transaction Undo (DTO)")
    class UndoTransactionDtoTests {

        @Test
        @DisplayName("undoTransaction (DTO) should reverse INCOME correctly")
        void undoTransactionDto_shouldSubtractIncome() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .type("INCOME")
                    .amount(new BigDecimal("25.00"))
                    .build();

            // act
            Account updatedAccount = account.undoTransaction(request);

            // assert & verify
            assertEquals(new BigDecimal("75.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("undoTransaction (DTO) should reverse EXPENSE correctly")
        void undoTransactionDto_shouldAddExpense() {
            // arrange
            Account account = Account.builder().currentBalance(new BigDecimal("100.00")).build();
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .type("EXPENSE")
                    .amount(new BigDecimal("25.00"))
                    .build();

            // act
            Account updatedAccount = account.undoTransaction(request);

            // assert & verify
            assertEquals(new BigDecimal("125.00"), updatedAccount.getCurrentBalance());
        }

        @Test
        @DisplayName("undoTransaction (DTO) should handle null balance")
        void undoTransactionDto_shouldHandleNullBalance() {
            // arrange
            Account account = Account.builder().currentBalance(null).build();
            TransactionCreateRequest request = TransactionCreateRequest.builder()
                    .type("INCOME")
                    .amount(new BigDecimal("25.00"))
                    .build();

            // act
            Account updatedAccount = account.undoTransaction(request);

            // assert & verify
            assertEquals(new BigDecimal("-25.00"), updatedAccount.getCurrentBalance());
        }
    }

    @Test
    @DisplayName("Account equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        Account a1 = Account.builder().id(1L).name("A").build();
        Account a2 = Account.builder().id(1L).name("B").build();
        Account a3 = Account.builder().id(2L).name("A").build();

        assertEquals(a1, a2);
        assertNotEquals(a1, a3);
        assertEquals(a1.hashCode(), a2.hashCode());
    }
}
