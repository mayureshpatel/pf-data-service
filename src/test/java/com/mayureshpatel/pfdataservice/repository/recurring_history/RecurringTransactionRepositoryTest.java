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
            // Act
            List<RecurringTransaction> result = repository.findAll();

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 2); // Based on baseline
        }

        @Test
        @DisplayName("should find by user ID")
        void shouldFindByUserId() {
            // Act
            List<RecurringTransaction> result = repository.findAllByUserId(USER_1);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.getUserId().equals(USER_1)));
        }

        @Test
        @DisplayName("should find active by user ID ordered by next date")
        void shouldFindActiveByUserId() {
            // Act
            List<RecurringTransaction> result = repository.findByUserIdAndActiveTrueOrderByNextDate(USER_1);

            // Assert
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
            // Arrange
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            Long id = all.get(0).getId();

            // Act
            Optional<RecurringTransaction> result = repository.findById(id);

            // Assert
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
            // Arrange
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                    .accountId(ACCOUNT_1)
                    .merchantId(MERCHANT_AMAZON)
                    .amount(new BigDecimal("99.99"))
                    .frequency("MONTHLY")
                    .nextDate(LocalDate.now().plusMonths(1))
                    .active(true)
                    .build();

            // Act
            int rows = repository.insert(request, USER_1);

            // Assert
            assertEquals(1, rows);
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            assertEquals(3, all.size());
        }

        @Test
        @DisplayName("should update an existing recurring transaction")
        void shouldUpdate() {
            // Arrange
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

            // Act
            int rows = repository.update(request, USER_1);

            // Assert
            assertEquals(1, rows);
            RecurringTransaction updated = repository.findById(existing.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("150.00").compareTo(updated.getAmount()));
            assertEquals("YEARLY", updated.getFrequency());
            assertFalse(updated.isActive());
        }

        @Test
        @DisplayName("should soft delete a recurring transaction")
        void shouldDelete() {
            // Arrange
            List<RecurringTransaction> all = repository.findAllByUserId(USER_1);
            Long id = all.get(0).getId();

            // Act
            int rows = repository.delete(id, USER_1);

            // Assert
            assertEquals(1, rows);
            assertTrue(repository.findById(id).isEmpty());
        }

        @Test
        @DisplayName("should throw error on deleteById")
        void shouldThrowOnDeleteById() {
            assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
        }
    }
}
