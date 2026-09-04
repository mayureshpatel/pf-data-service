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
import java.time.ZoneOffset;
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

    @Nested
    @DisplayName("findMatches - timezone crossover")
    class TimezoneCrossoverTests {

        @Test
        @DisplayName("should match transactions close in absolute time despite different local calendar dates")
        void shouldMatchAcrossTimezoneBoundary() {
            // Arrange
            // t1 local date is Jan 1, t2 local date is Jan 2 - but only 12 hours apart in
            // absolute time (t1 instant: 2026-01-02T04:00Z, t2 instant: 2026-01-01T16:00Z).
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.of(2026, 1, 1, 23, 0, 0, 0, ZoneOffset.of("-05:00")));
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME,
                    OffsetDateTime.of(2026, 1, 2, 1, 0, 0, 0, ZoneOffset.of("+09:00")));

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertEquals(1, result.size());
            assertEquals(0.9, result.get(0).confidenceScore());
        }

        @Test
        @DisplayName("should not match transactions whose local calendar dates look close but are more than 3 days apart in absolute time")
        void shouldNotMatchAcrossTimezoneBoundaryWhenActuallyFarApart() {
            // Arrange
            // Local calendar dates are Jan 1 and Jan 4 (naively 3 days apart), but the +14:00 /
            // -12:00 offset spread puts the actual instants just over 5 days apart.
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.of("+14:00")));
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME,
                    OffsetDateTime.of(2026, 1, 4, 23, 0, 0, 0, ZoneOffset.of("-12:00")));

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findMatches - out-of-order date lists")
    class OutOfOrderDateListTests {

        @Test
        @DisplayName("should still find a valid match when an unrelated far-apart transaction appears between it in the list")
        void shouldMatchAcrossOutOfOrderInput() {
            // Arrange
            // t2 sits between t1 and t3 in list order but is 10 days away from both. Before the
            // matcher sorted its input, the inner loop's early break on daysDiff > 3 would exit
            // as soon as it hit t2 and never reach t3, silently dropping the valid t1/t3 match.
            OffsetDateTime day0 = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, day0);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, day0.plusDays(10));
            Transaction t3 = createTxn(3L, 30L, BigDecimal.TEN, TransactionType.INCOME, day0);

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2, t3));

            // Assert
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(3L, result.get(0).targetTransaction().id());
        }
    }

    @Nested
    @DisplayName("findMatches - multi-transaction splits")
    class MultiTransactionSplitTests {

        @Test
        @DisplayName("should not detect a transfer split across multiple transactions (algorithm only supports 1:1 pair matching)")
        void shouldNotMatchSplitTransfer() {
            // Arrange
            // A single $100 withdrawal that was actually deposited as two separate $50 amounts
            // has no single counterpart transaction of equal value, so it cannot be matched by
            // this pairwise algorithm. This documents current (unsupported) behavior, not a bug.
            OffsetDateTime now = OffsetDateTime.now();
            Transaction withdrawal = createTxn(1L, 10L, new BigDecimal("100"), TransactionType.EXPENSE, now);
            Transaction depositPart1 = createTxn(2L, 20L, new BigDecimal("50"), TransactionType.INCOME, now);
            Transaction depositPart2 = createTxn(3L, 20L, new BigDecimal("50"), TransactionType.INCOME, now);

            // Act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(withdrawal, depositPart1, depositPart2));

            // Assert
            assertTrue(result.isEmpty());
        }
    }
}
