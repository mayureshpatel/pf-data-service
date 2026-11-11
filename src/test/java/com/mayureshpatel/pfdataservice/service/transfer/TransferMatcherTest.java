package com.mayureshpatel.pfdataservice.service.transfer;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.category.Category;
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

/** Verifies {@code TransferMatcher}'s pairwise transfer-detection algorithm, one {@code @Nested} class per matching concern below. */
@DisplayName("TransferMatcher Unit Tests")
class TransferMatcherTest {

    private static final String CREDIT_CARD = "CREDIT_CARD";
    private static final String CHECKING = "CHECKING";
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TWO_FIFTY = new BigDecimal("250.00");

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


    private Transaction createTxn(Long id, Long accountId, String accountTypeCode, String categoryName, String parentCategoryName,
                                  BigDecimal amount, TransactionType type, OffsetDateTime date) {
        Category parent = parentCategoryName == null ? null : Category.builder().id(900L).name(parentCategoryName).build();
        Category category = categoryName == null ? null : Category.builder().id(901L).name(categoryName).parent(parent).build();
        return Transaction.builder()
                .id(id)
                .account(Account.builder().id(accountId).type(AccountType.builder().code(accountTypeCode).build()).build())
                .category(category)
                .amount(amount)
                .type(type)
                .transactionDate(date)
                .description("Txn " + id)
                .build();
    }

    /**
     * {@code findMatches} pairs an EXPENSE with an INCOME of equal amount from two *different*
     * accounts, within a 3-day window, with confidence decaying 0.1 per day apart (same-day =
     * 0.9); it never matches same-account or same-type transactions or unequal amounts, and once a
     * transaction is matched it's removed from consideration for any later pairing in the same run.
     */
    @Nested
    @DisplayName("findMatches")
    class FindMatchesTests {

        @Test
        @DisplayName("should return empty list when no transactions provided")
        void shouldHandleEmptyList() {
            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of());

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should match two identical transactions on same day from different accounts")
        void shouldMatchPerfectPair() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now);

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(0.9, result.get(0).confidenceScore());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(2L, result.get(0).targetTransaction().id());
        }

        @Test
        @DisplayName("should match transactions within 3-day window with decreasing confidence")
        void shouldMatchWithinWindow() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now.plusDays(2));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertEquals(1, result.size());
            // 0.9 - (2 * 0.1) = 0.7
            assertEquals(0.7, result.get(0).confidenceScore(), 0.001);
        }

        @Test
        @DisplayName("should not match transactions more than 3 days apart")
        void shouldNotMatchOutsideWindow() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now.plusDays(4));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match transactions from the same account")
        void shouldNotMatchSameAccount() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 10L, BigDecimal.TEN, TransactionType.INCOME, now);

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match transactions of the same type")
        void shouldNotMatchSameType() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.EXPENSE, now);

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match transactions with different amounts")
        void shouldNotMatchDiffAmounts() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.ONE, TransactionType.INCOME, now);

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should skip already matched transactions in the target loop")
        void shouldSkipAlreadyMatchedInTargetLoop() {
            // arrange
            OffsetDateTime now = OffsetDateTime.now();
            // t1 matches with t2. t3 would also match with t2, but t2 is gone.
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, now);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, now);
            Transaction t3 = createTxn(3L, 30L, BigDecimal.TEN, TransactionType.EXPENSE, now);

            // act
            // First loop matches 1 & 2. 
            // Second loop (for t3) will see t2 in matchedIds.
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2, t3));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(2L, result.get(0).targetTransaction().id());
        }
    }

    /** The 3-day matching window is computed from absolute instants, not local calendar dates -- two transactions whose local dates differ by a day can still match if they're close in absolute time across a timezone boundary, and conversely two transactions with close-looking local dates can fail to match if extreme offsets push them genuinely far apart. */
    @Nested
    @DisplayName("findMatches - timezone crossover")
    class TimezoneCrossoverTests {

        @Test
        @DisplayName("should match transactions close in absolute time despite different local calendar dates")
        void shouldMatchAcrossTimezoneBoundary() {
            // arrange
            // t1 local date is Jan 1, t2 local date is Jan 2 - but only 12 hours apart in
            // absolute time (t1 instant: 2026-01-02T04:00Z, t2 instant: 2026-01-01T16:00Z).
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.of(2026, 1, 1, 23, 0, 0, 0, ZoneOffset.of("-05:00")));
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME,
                    OffsetDateTime.of(2026, 1, 2, 1, 0, 0, 0, ZoneOffset.of("+09:00")));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(0.9, result.get(0).confidenceScore());
        }

        @Test
        @DisplayName("should not match transactions whose local calendar dates look close but are more than 3 days apart in absolute time")
        void shouldNotMatchAcrossTimezoneBoundaryWhenActuallyFarApart() {
            // arrange
            // Local calendar dates are Jan 1 and Jan 4 (naively 3 days apart), but the +14:00 /
            // -12:00 offset spread puts the actual instants just over 5 days apart.
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE,
                    OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.of("+14:00")));
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME,
                    OffsetDateTime.of(2026, 1, 4, 23, 0, 0, 0, ZoneOffset.of("-12:00")));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2));

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    /** The matcher sorts its input before scanning, so an unrelated far-apart transaction sitting between two otherwise-matchable transactions in caller-supplied list order doesn't cause the inner loop's early exit-on-too-far-apart to miss the real match. */
    @Nested
    @DisplayName("findMatches - out-of-order date lists")
    class OutOfOrderDateListTests {

        @Test
        @DisplayName("should still find a valid match when an unrelated far-apart transaction appears between it in the list")
        void shouldMatchAcrossOutOfOrderInput() {
            // arrange
            // t2 sits between t1 and t3 in list order but is 10 days away from both. Before the
            // matcher sorted its input, the inner loop's early break on daysDiff > 3 would exit
            // as soon as it hit t2 and never reach t3, silently dropping the valid t1/t3 match.
            OffsetDateTime day0 = OffsetDateTime.now();
            Transaction t1 = createTxn(1L, 10L, BigDecimal.TEN, TransactionType.EXPENSE, day0);
            Transaction t2 = createTxn(2L, 20L, BigDecimal.TEN, TransactionType.INCOME, day0.plusDays(10));
            Transaction t3 = createTxn(3L, 30L, BigDecimal.TEN, TransactionType.INCOME, day0);

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(t1, t2, t3));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(3L, result.get(0).targetTransaction().id());
        }
    }

    /** Documents current, deliberately-unsupported behavior: a withdrawal later deposited as two separate smaller amounts has no single counterpart of equal value, so this pairwise-only algorithm can't detect it as a transfer. */
    @Nested
    @DisplayName("findMatches - multi-transaction splits")
    class MultiTransactionSplitTests {

        @Test
        @DisplayName("should not detect a transfer split across multiple transactions (algorithm only supports 1:1 pair matching)")
        void shouldNotMatchSplitTransfer() {
            // arrange
            // A single $100 withdrawal that was actually deposited as two separate $50 amounts
            // has no single counterpart transaction of equal value, so it cannot be matched by
            // this pairwise algorithm. This documents current (unsupported) behavior, not a bug.
            OffsetDateTime now = OffsetDateTime.now();
            Transaction withdrawal = createTxn(1L, 10L, new BigDecimal("100"), TransactionType.EXPENSE, now);
            Transaction depositPart1 = createTxn(2L, 20L, new BigDecimal("50"), TransactionType.INCOME, now);
            Transaction depositPart2 = createTxn(3L, 20L, new BigDecimal("50"), TransactionType.INCOME, now);

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(withdrawal, depositPart1, depositPart2));

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    /**
     * PF-938: a transfer whose two legs post more than 3 days apart (a credit card payment that takes a week to
     * clear) is still suggested, but only when the rows look like the same transfer -- both filed under a Transfers
     * category, or one of them on a credit card account -- so unrelated same-amount rows are not paired.
     */
    @Nested
    @DisplayName("findMatches - wider window for rows that look like one transfer")
    class WiderWindowTests {

        private final OffsetDateTime day0 = OffsetDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        @Test
        @DisplayName("should match a credit card payment posted 7 days after the checking withdrawal")
        void shouldMatchCreditCardPaymentSevenDaysApart() {
            // arrange
            Transaction out = createTxn(1L, 10L, CHECKING, null, null, TWO_FIFTY, TransactionType.EXPENSE, day0);
            Transaction in = createTxn(2L, 20L, CREDIT_CARD, null, null, TWO_FIFTY, TransactionType.INCOME, day0.plusDays(7));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(out, in));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).sourceTransaction().id());
            assertEquals(2L, result.get(0).targetTransaction().id());
            assertTrue(result.get(0).confidenceScore() > 0 && result.get(0).confidenceScore() < 0.6);
        }

        @Test
        @DisplayName("should suggest the five real checking-to-card pairs that posted 4 to 7 days apart")
        void shouldMatchTheFiveRealPairs() {
            // arrange
            String[][] pairs = {
                    {"2022-09-29", "2022-10-06", "781.90"},
                    {"2023-06-01", "2023-06-05", "1291.02"},
                    {"2023-09-01", "2023-09-05", "520.26"},
                    {"2023-09-01", "2023-09-05", "446.90"},
                    {"2024-02-29", "2024-03-04", "2233.31"}};
            List<Transaction> all = new java.util.ArrayList<>();
            long id = 1L;
            for (String[] pair : pairs) {
                BigDecimal amount = new BigDecimal(pair[2]);
                all.add(createTxn(id++, 10L, CHECKING, null, null, amount, TransactionType.EXPENSE,
                        java.time.LocalDate.parse(pair[0]).atStartOfDay().atOffset(ZoneOffset.UTC)));
                all.add(createTxn(id++, 20L, CREDIT_CARD, null, null, amount, TransactionType.INCOME,
                        java.time.LocalDate.parse(pair[1]).atStartOfDay().atOffset(ZoneOffset.UTC)));
            }

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(all);

            // assert & verify
            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("should match two rows both filed under a Transfers category 6 days apart, by parent or own name")
        void shouldMatchBothInTransfersCategory() {
            // arrange
            Transaction out = createTxn(1L, 10L, CHECKING, "Credit Card Payment", "Transfers", new BigDecimal("80.00"), TransactionType.EXPENSE, day0);
            Transaction in = createTxn(2L, 20L, "SAVINGS", "Transfers", null, new BigDecimal("80.00"), TransactionType.INCOME, day0.plusDays(6));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(out, in));

            // assert & verify
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should not match same-amount rows beyond 3 days when nothing says they are one transfer")
        void shouldNotMatchUnrelatedRowsBeyondThreeDays() {
            // arrange
            Transaction charge = createTxn(1L, 10L, CHECKING, "Groceries", "Food", new BigDecimal("42.00"), TransactionType.EXPENSE, day0);
            Transaction refund = createTxn(2L, 20L, "SAVINGS", "Gifts", "Income", new BigDecimal("42.00"), TransactionType.INCOME, day0.plusDays(6));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(charge, refund));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should not match beyond 7 days even when the rows look related")
        void shouldNotMatchBeyondSevenDays() {
            // arrange
            Transaction out = createTxn(1L, 10L, CHECKING, null, null, TWO_FIFTY, TransactionType.EXPENSE, day0);
            Transaction in = createTxn(2L, 20L, CREDIT_CARD, null, null, TWO_FIFTY, TransactionType.INCOME, day0.plusDays(8));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(out, in));

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should pair a row with the closest-dated candidate, and use each row once")
        void shouldPreferClosestDateAndUseEachRowOnce() {
            // arrange
            Transaction out = createTxn(1L, 10L, CHECKING, null, null, HUNDRED, TransactionType.EXPENSE, day0);
            Transaction farIn = createTxn(2L, 20L, CREDIT_CARD, null, null, HUNDRED, TransactionType.INCOME, day0.plusDays(2));
            Transaction nearIn = createTxn(3L, 20L, CREDIT_CARD, null, null, HUNDRED, TransactionType.INCOME, day0.plusDays(1));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(out, farIn, nearIn));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(3L, result.get(0).targetTransaction().id());
        }

        @Test
        @DisplayName("should prefer a closer candidate over an earlier-listed one 5 days away")
        void shouldPreferCloserOverFirstFound() {
            // arrange
            Transaction out = createTxn(1L, 10L, CHECKING, null, null, HUNDRED, TransactionType.EXPENSE, day0);
            Transaction first = createTxn(2L, 20L, CREDIT_CARD, null, null, HUNDRED, TransactionType.INCOME, day0.plusDays(2));
            Transaction closer = createTxn(3L, 30L, CREDIT_CARD, null, null, HUNDRED, TransactionType.INCOME, day0.plusDays(2).plusHours(1));
            Transaction closest = createTxn(4L, 40L, CREDIT_CARD, null, null, HUNDRED, TransactionType.INCOME, day0.plusHours(5));

            // act
            List<TransferSuggestionDto> result = matcher.findMatches(List.of(out, first, closer, closest));

            // assert & verify
            assertEquals(4L, result.get(0).targetTransaction().id());
        }
    }
}
