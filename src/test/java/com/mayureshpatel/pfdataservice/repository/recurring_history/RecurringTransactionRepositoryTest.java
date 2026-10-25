package com.mayureshpatel.pfdataservice.repository.recurring_history;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code RecurringTransactionRepository} against a real PostgreSQL instance (via {@link BaseRepositoryTest}'s Testcontainers setup and a shared baseline fixture), exercising the JDBC Client mapping and ownership-scoped writes directly rather than mocking them. */
@Import(RecurringTransactionRepository.class)
@DisplayName("RecurringTransactionRepository Integration Tests (PostgreSQL)")
class RecurringTransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RecurringTransactionRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long USER_1 = 1L;
    private static final Long ACCOUNT_1 = 1L;
    private static final Long MERCHANT_AMAZON = 2L;

    /** {@code findAll}/{@code findAllByUserId}/{@code findById} read correctly against the baseline fixture, and {@code findByUserIdAndActiveTrueOrderByNextDate} returns only active rows, genuinely ordered by {@code nextDate} ascending. */
    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find all recurring transactions")
        void shouldFindAll() {
            // act
            List<RecurringTransaction> result = repository.findAll();

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 2); // Based on baseline
        }

        @Test
        @DisplayName("should find by user ID")
        void shouldFindByUserId() {
            // act
            List<RecurringTransaction> result = repository.findAllByUserId(USER_1);

            // assert & verify
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.getUserId().equals(USER_1)));
        }

        @Test
        @DisplayName("should find active by user ID ordered by next date")
        void shouldFindActiveByUserId() {
            // act
            List<RecurringTransaction> result = repository.findByUserIdAndActiveTrueOrderByNextDate(USER_1);

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(RecurringTransaction::isActive));
            // Verify order
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getNextDate().isBefore(result.get(i + 1).getNextDate()) 
                        || result.get(i).getNextDate().isEqual(result.get(i + 1).getNextDate()));
            }
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // arrange
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            Long id = all.get(0).getId();

            // act
            Optional<RecurringTransaction> result = repository.findById(id);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(id, result.get().getId());
        }
    }

    /** {@code insert} returns the real database-generated id (checked against a fresh {@code findAllByUserId}/{@code findById}, not just {@code update}'s rows-affected count, which would otherwise coincidentally collide with baseline id 1); {@code update}/{@code delete} (soft) round-trip correctly, and {@code deleteById} is deliberately unsupported -- only the ownership-scoped {@code delete(id, userId)} overload is exposed. */
    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new recurring transaction")
        void shouldInsert() {
            // arrange
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                    .accountId(ACCOUNT_1)
                    .merchantId(MERCHANT_AMAZON)
                    .amount(new BigDecimal("99.99"))
                    .frequency("MONTHLY")
                    .nextDate(LocalDate.now().plusMonths(1))
                    .active(true)
                    .build();

            // act
            int newId = repository.insert(request, USER_1);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count (always
            // 1 on a successful single-row insert, which would coincidentally collide with
            // baseline recurring transaction id 1 and mask the bug this regresses against)
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            assertEquals(3, all.size());
            RecurringTransaction inserted = repository.findById((long) newId).orElseThrow();
            assertEquals(0, new BigDecimal("99.99").compareTo(inserted.getAmount()));
            assertEquals("MONTHLY", inserted.getFrequency());
        }

        @Test
        @DisplayName("should update an existing recurring transaction")
        void shouldUpdate() {
            // arrange
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            RecurringTransaction existing = all.get(0);

            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder()
                    .id(existing.getId())
                    .accountId(ACCOUNT_1)
                    .merchantId(MERCHANT_AMAZON)
                    .amount(new BigDecimal("150.00"))
                    .frequency("YEARLY")
                    .nextDate(LocalDate.now().plusYears(1))
                    .active(false)
                    .build();

            // act
            int rows = repository.update(request, USER_1);

            // assert & verify
            assertEquals(1, rows);
            RecurringTransaction updated = repository.findById(existing.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("150.00").compareTo(updated.getAmount()));
            assertEquals("YEARLY", updated.getFrequency());
            assertFalse(updated.isActive());
        }

        @Test
        @DisplayName("should soft delete a recurring transaction")
        void shouldDelete() {
            // arrange
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            Long id = all.get(0).getId();

            // act
            int rows = repository.delete(id, USER_1);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findById(id).isEmpty());
        }

        @Test
        @DisplayName("should throw error on deleteById")
        void shouldThrowOnDeleteById() {
            assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
        }
    }

    /** {@code countByAccountId} counts real recurring transactions for an account, returns 0 for an account with none, and excludes soft-deleted rows from the count once one is deleted. */
    @Nested
    @DisplayName("Status & Counts")
    class StatusTests {
        @Test
        @DisplayName("should count recurring transactions for an account")
        void shouldCountByAccountId() {
            // act -- account 1 (Main Checking) has exactly one baseline recurring transaction (id 1)
            long count = repository.countByAccountId(ACCOUNT_1);

            // assert & verify
            assertEquals(1, count);
        }

        @Test
        @DisplayName("should count zero recurring transactions for an account with none")
        void shouldCountByAccountIdZeroWhenNone() {
            // act -- account 2 (Rainy Day Savings) has no baseline recurring transaction
            long count = repository.countByAccountId(2L);

            // assert & verify
            assertEquals(0, count);
        }

        @Test
        @DisplayName("should exclude soft-deleted recurring transactions from the count")
        void shouldCountByAccountIdExcludingDeleted() {
            // arrange -- baseline recurring transaction id 1 belongs to account 1

            // act
            long countBeforeDelete = repository.countByAccountId(ACCOUNT_1);
            repository.delete(1L, USER_1);
            long countAfterDelete = repository.countByAccountId(ACCOUNT_1);

            // assert & verify
            assertEquals(1, countBeforeDelete);
            assertEquals(0, countAfterDelete);
        }
    }

    /** A merchant used by a tracked item can not be deleted: {@code countActiveByMerchantId} says whether one still uses it, and {@code purgeDeletedByMerchantId} removes the already-deleted rows whose NOT NULL merchant reference would otherwise block deleting the merchant. */
    @Nested
    @DisplayName("merchant usage")
    class MerchantUsageTests {

        @Test
        @DisplayName("should count the user's undeleted items that use the merchant, paused ones included")
        void shouldCountUndeletedUse() {
            // arrange -- one item is paused (still tracked), the other is deleted
            jdbcClient.sql("update recurring_transactions set active = false where id = 2").update();
            jdbcClient.sql("update recurring_transactions set deleted_at = now() where id = 1").update();

            // act & assert & verify
            assertEquals(1L, repository.countActiveByMerchantId(3L, USER_1));
            assertEquals(0L, repository.countActiveByMerchantId(MERCHANT_AMAZON, USER_1));
            assertEquals(0L, repository.countActiveByMerchantId(3L, 2L));
        }

        @Test
        @DisplayName("should purge deleted rows so the merchant can then be deleted")
        void shouldPurgeDeletedRowsSoMerchantCanBeDeleted() {
            // arrange -- the Amazon item is deleted (soft) and so no longer blocks in the user's eyes
            jdbcClient.sql("update recurring_transactions set deleted_at = now() where id = 1").update();

            // act
            int purged = repository.purgeDeletedByMerchantId(MERCHANT_AMAZON, USER_1);
            int merchantsDeleted = jdbcClient.sql("delete from merchants where id = :id").param("id", MERCHANT_AMAZON).update();

            // assert & verify
            assertEquals(1, purged);
            assertEquals(1, merchantsDeleted);
        }

        @Test
        @DisplayName("should leave active items alone when purging")
        void shouldNotPurgeActiveItems() {
            // act & assert & verify
            assertEquals(0, repository.purgeDeletedByMerchantId(3L, USER_1));
            assertEquals(1L, repository.countActiveByMerchantId(3L, USER_1));
        }
    }

    /** {@code findRecentChargesByRecurringId} matches a tracked item to the user's real expense charges through its merchant id or its merchant's linked descriptions (folded for case and spacing), newest first, at most 12 per item, never crossing users or counting deleted or non-expense rows. */
    @Nested
    @DisplayName("findRecentChargesByRecurringId")
    class FindRecentChargesTests {

        private static final String EXPENSE = "EXPENSE";
        private static final Long RECURRING_AMAZON = 1L;
        private static final String TXN_INSERT = """
                insert into transactions (account_id, merchant_id, amount, date, description, type)
                values (:accountId, :merchantId, :amount, :date::timestamptz, :description, :type)
                """;

        private void insertCharge(long accountId, Long merchantId, String amount, String date, String description, String type) {
            jdbcClient.sql(TXN_INSERT)
                    .param("accountId", accountId)
                    .param("merchantId", merchantId)
                    .param("amount", new java.math.BigDecimal(amount))
                    .param("date", date)
                    .param("description", description)
                    .param("type", type)
                    .update();
        }

        private void insertLink(String description, String normalized) {
            jdbcClient.sql("""
                    insert into merchant_description_links (user_id, merchant_id, description, normalized_description)
                    values (1, :merchantId, :description, :normalized)
                    """)
                    .param("merchantId", MERCHANT_AMAZON)
                    .param("description", description)
                    .param("normalized", normalized)
                    .update();
        }

        private List<com.mayureshpatel.pfdataservice.domain.transaction.RecurringCharge> chargesOfAmazonItem() {
            return repository.findRecentChargesByRecurringId(USER_1).getOrDefault(RECURRING_AMAZON, List.of());
        }

        @Test
        @DisplayName("should match charges through the merchant id")
        void shouldMatchThroughMerchantId() {
            // arrange
            insertCharge(ACCOUNT_1, MERCHANT_AMAZON, "15.99", "2026-09-01T12:00:00Z", "AMAZON PRIME", EXPENSE);

            // act & assert & verify
            assertEquals(1, chargesOfAmazonItem().size());
            assertEquals(0, new java.math.BigDecimal("15.99").compareTo(chargesOfAmazonItem().get(0).amount()));
        }

        @Test
        @DisplayName("should match charges through a linked description folded for case and spacing")
        void shouldMatchThroughLinkedDescription() {
            // arrange
            insertLink("AMZN MKTP", "amzn mktp");
            insertCharge(ACCOUNT_1, null, "9.50", "2026-09-02T12:00:00Z", "  AMZN   mktp\t", EXPENSE);

            // act & assert & verify
            assertEquals(1, chargesOfAmazonItem().size());
        }

        @Test
        @DisplayName("should ignore soft-deleted, income and another user's transactions")
        void shouldIgnoreNonMatching() {
            // arrange
            insertLink("AMZN MKTP", "amzn mktp");
            insertCharge(ACCOUNT_1, MERCHANT_AMAZON, "1.00", "2026-09-03T12:00:00Z", "deleted", EXPENSE);
            jdbcClient.sql("update transactions set deleted_at = now() where description = 'deleted'").update();
            insertCharge(ACCOUNT_1, MERCHANT_AMAZON, "2.00", "2026-09-04T12:00:00Z", "refund", "INCOME");
            insertCharge(4L, null, "3.00", "2026-09-05T12:00:00Z", "AMZN MKTP", EXPENSE);

            // act & assert & verify
            assertTrue(chargesOfAmazonItem().isEmpty());
        }

        @Test
        @DisplayName("should ignore charges older than the three-year lookback")
        void shouldIgnoreChargesOlderThanTheLookback() {
            // arrange
            insertCharge(ACCOUNT_1, MERCHANT_AMAZON, "5.00", java.time.OffsetDateTime.now().minusYears(4).toString(), "OLD", EXPENSE);
            insertCharge(ACCOUNT_1, MERCHANT_AMAZON, "6.00", java.time.OffsetDateTime.now().minusMonths(2).toString(), "RECENT", EXPENSE);

            // act
            var charges = chargesOfAmazonItem();

            // assert & verify
            assertEquals(1, charges.size());
            assertEquals(0, new java.math.BigDecimal("6.00").compareTo(charges.get(0).amount()));
        }

        @Test
        @DisplayName("should return at most 12 newest first per item")
        void shouldCapAtTwelveNewestFirst() {
            // arrange
            for (int day = 1; day <= 15; day++) {
                insertCharge(ACCOUNT_1, MERCHANT_AMAZON, "5.00", String.format("2026-08-%02dT12:00:00Z", day), "AMAZON", EXPENSE);
            }

            // act
            var charges = chargesOfAmazonItem();

            // assert & verify
            assertEquals(12, charges.size());
            assertTrue(charges.get(0).date().isAfter(charges.get(11).date()));
        }

        @Test
        @DisplayName("should return no entry for an item without matching charges")
        void shouldReturnNothingForUnmatchedItem() {
            // act & assert & verify
            assertNull(repository.findRecentChargesByRecurringId(USER_1).get(RECURRING_AMAZON));
        }

        @Test
        @DisplayName("should return paused items from findAllByUserId")
        void shouldReturnPausedFromFindAll() {
            // arrange
            jdbcClient.sql("update recurring_transactions set active = false where id = :id").param("id", RECURRING_AMAZON).update();

            // act & assert & verify
            assertEquals(2, repository.findAllByUserId(USER_1).size());
            assertEquals(1, repository.findByUserIdAndActiveTrueOrderByNextDate(USER_1).size());
        }
    }
}
