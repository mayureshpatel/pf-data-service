package com.mayureshpatel.pfdataservice.service.transfer;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TransferMatcher Unit Tests")
class TransferMatcherTest {

    private final TransferMatcher matcher = new TransferMatcher();

    private Transaction createTxn(Long id, Long accountId, BigDecimal amount, TransactionType type, OffsetDateTime date) {
        return Transaction.builder()
                .id(id)
                .account(Account.builder().id(accountId).build())
                .amount(amount)
                .type(type)
                .transactionDate(date)
                .description("Txn " + id)
                .build();
    }

    @Nested
    @DisplayName("findMatches")
    class FindMatchesTests {

        @Test
        @DisplayName("should return empty list when no transactions provided")
        void shouldHandleEmptyList() {
            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of());

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should match two identical transactions on same day from different accounts")
        void shouldMatchPerfectPair() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now);

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertEquals(1, result.size());
            assertEquals(0.9, result.get(0).confidenceScore());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(2L, result.get(0).targetTransaction().id());
        }

        @Test
        @DisplayName("should match transactions within 3-day window with decreasing confidence")
        void shouldMatchWithinWindow() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now.plusDays(2));

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertEquals(1, result.size());
            // 0.9 - (2 * 0.1) = 0.7
            assertEquals(0.7, result.get(0).confidenceScore(), 0.001);
        }

        @Test
        @DisplayName("should not match transactions more than 3 days apart")
        void shouldNotMatchOutsideWindow() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now.plusDays(4));

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match transactions from the same account")
        void shouldNotMatchSameAccount() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 10L, BigDecimal.TEN, TransactionType.INCOME, now);

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match transactions of the same type")
        void shouldNotMatchSameType() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.EXPENSE, now);

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match transactions with different amounts")
        void shouldNotMatchDiffAmounts() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.ONE, TransactionType.INCOME, now);

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should skip already matched transactions in the target loop")
        void shouldSkipAlreadyMatchedInTargetLoop() {
            // Arrange
            OffsetDateTime now = OffsetDateTime.now();
            // t1 matches with t2. t3 would also match with t2, but t2 is gone.
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now);
            Transaction t3 = createTxn(3L, 30L, BigDecimal.TEN, TransactionType.EXPENSE, now);

            // Act
            // First loop matches 1 & 2. 
            // Second loop (for t3) will see t2 in matchedIds.
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2, t3));

            // Assert
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(2L, result.get(0).targetTransaction().id());
        }
    }
}
