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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(RecurringTransactionRepository.class)
@DisplayName("RecurringTransactionRepository Integration Tests (PostgreSQL)")
class RecurringTransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RecurringTransactionRepository repository;

    private static final Long USER_1 = 1L;
    private static final Long ACCOUNT_1 = 1L;
    private static final Long MERCHANT_AMAZON = 2L;

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
}
