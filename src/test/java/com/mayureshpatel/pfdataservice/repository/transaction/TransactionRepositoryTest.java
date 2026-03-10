package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TransactionRepository.class)
@DisplayName("TransactionRepository Integration Tests (PostgreSQL)")
class TransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("Dynamic Filtering (Specification)")
    class FilterTests {
        @Test
        @DisplayName("should filter by type and user")
        void shouldFilterByType() {
            // Arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, TransactionType.INCOME, null, null, null, null, null, null, null
            );

            // Act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.getContent().stream().allMatch(t -> t.getType() == TransactionType.INCOME));
        }

        @Test
        @DisplayName("should filter by amount range")
        void shouldFilterByAmount() {
            // Arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, new BigDecimal("1000.00"), new BigDecimal("2000.00"), null, null
            );

            // Act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.getContent().stream().allMatch(t -> 
                t.getAmount().compareTo(new BigDecimal("1000.00")) >= 0 && 
                t.getAmount().compareTo(new BigDecimal("2000.00")) <= 0
            ));
        }

        @Test
        @DisplayName("should safely handle invalid sort direction and property")
        void shouldHandleInvalidSort() {
            // Arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, null, null, null, null
            );
            // Try to inject SQL in Sort direction and property
            Sort maliciousSort = Sort.by(Sort.Order.desc("date; DROP TABLE transactions; --"));
            
            // Act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), 
                    PageRequest.of(0, 10, maliciousSort)
            );

            // Assert
            assertFalse(result.isEmpty()); // Should not crash and should return data
        }
    }

    @Nested
    @DisplayName("Aggregations")
    class AggregationTests {
        @Test
        @DisplayName("should calculate sum for date range and type")
        void shouldCalculateSum() {
            // Arrange
            OffsetDateTime start = LocalDate.of(2026, 3, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // Act
            BigDecimal sum = transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME);

            // Assert
            // Based on baseline: 1002 is 500.00 INCOME
            assertEquals(0, new BigDecimal("500.00").compareTo(sum));
        }

        @Test
        @DisplayName("should find category totals")
        void shouldFindCategoryTotals() {
            // Arrange
            OffsetDateTime start = LocalDate.of(2026, 3, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // Act
            List<CategoryBreakdownDto> result = transactionRepository.findCategoryTotals(USER_ID, start, end);

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.stream()
                    .filter(b -> b.category() != null)
                    .anyMatch(b -> b.category().name().equals("Dining Out")));
        }

        @Test
        @DisplayName("should find monthly sums for cash flow trend")
        void shouldFindMonthlySums() {
            // Act
            List<Object[]> result = transactionRepository.findMonthlySums(USER_ID, LocalDate.of(2025, 9, 1));

            // Assert
            assertFalse(result.isEmpty());
            // result is [year, month, type, sum]
            Object[] first = result.get(0);
            assertEquals(4, first.length);
        }
    }

    @Nested
    @DisplayName("Status & Counts")
    class StatusTests {
        @Test
        @DisplayName("should get count by category")
        void shouldGetCountByCategory() {
            // Act
            List<CategoryTransactionsDto> result = transactionRepository.getCountByCategory(USER_ID);

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.stream()
                    .filter(c -> c.category() != null)
                    .anyMatch(c -> c.category().name().equals("Groceries")));
        }

        @Test
        @DisplayName("should check if transaction exists by specific fields")
        void shouldCheckExistence() {
            // Arrange
            OffsetDateTime date = OffsetDateTime.parse("2026-03-01T10:00:00Z");
            
            // Act
            boolean exists = transactionRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndType(
                    1L, date, new BigDecimal("25.50"), "Morning Coffee", TransactionType.EXPENSE
            );

            // Assert
            assertTrue(exists);
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should find by id and userId")
        void shouldFindByIdAndUserId() {
            Transaction t = transactionRepository.findById(1001L, USER_ID).orElseThrow();
            assertEquals(1001L, t.getId());
            assertEquals(USER_ID, t.getAccount().getUserId());
        }

        @Test
        @DisplayName("should throw error for insecure findById")
        void shouldThrowOnInsecureFind() {
            assertThrows(UnsupportedOperationException.class, () -> transactionRepository.findById(1001L));
        }

        @Test
        @DisplayName("should update transaction")
        void shouldUpdate() {
            Transaction t = transactionRepository.findById(1001L, USER_ID).orElseThrow();
            Transaction updated = t.toBuilder().description("UPDATED DESC").build();
            
            int rows = transactionRepository.update(USER_ID, updated);
            
            assertEquals(1, rows);
            Transaction result = transactionRepository.findById(1001L, USER_ID).orElseThrow();
            assertEquals("UPDATED DESC", result.getDescription());
        }

        @Test
        @DisplayName("should update multiple transactions")
        void shouldUpdateAll() {
            Transaction t1 = transactionRepository.findById(1001L, USER_ID).orElseThrow();
            Transaction t2 = transactionRepository.findById(1002L, USER_ID).orElseThrow();
            
            List<Transaction> list = List.of(
                t1.toBuilder().description("BULK 1").build(),
                t2.toBuilder().description("BULK 2").build()
            );
            
            int total = transactionRepository.updateAll(USER_ID, list);
            assertEquals(2, total);
        }

        @Test
        @DisplayName("should delete by id and userId")
        void shouldDeleteByIdAndUserId() {
            int rows = transactionRepository.deleteById(1001L, USER_ID);
            assertEquals(1, rows);
            assertTrue(transactionRepository.findById(1001L, USER_ID).isEmpty());
        }

        @Test
        @DisplayName("should throw error for insecure deleteById")
        void shouldThrowOnInsecureDelete() {
            assertThrows(UnsupportedOperationException.class, () -> transactionRepository.deleteById(1001L));
        }

        @Test
        @DisplayName("should insert multiple transactions dynamically in batches")
        void shouldInsertAll() {
            // arrange
            List<TransactionCreateRequest> requests = new ArrayList<>();
            int totalRecordsToInsert = 1050; // Tests that chunking (500 limit) works properly
            
            for (int i = 0; i < totalRecordsToInsert; i++) {
                requests.add(TransactionCreateRequest.builder()
                        .accountId(1L)
                        .amount(new BigDecimal("10.50"))
                        .transactionDate(OffsetDateTime.now(ZoneOffset.UTC))
                        .postDate(OffsetDateTime.now(ZoneOffset.UTC))
                        .description("Batch Insert " + i)
                        .type(TransactionType.EXPENSE.name())
                        .build());
            }

            long beforeCount = transactionRepository.count();

            // act
            int inserted = transactionRepository.insertAll(requests);

            // assert & verify
            assertEquals(totalRecordsToInsert, inserted);
            assertEquals(beforeCount + totalRecordsToInsert, transactionRepository.count());
        }

        @Test
        @DisplayName("should handle empty list on insertAll without failing")
        void shouldHandleEmptyInsertAll() {
            // arrange
            List<TransactionCreateRequest> requests = List.of();

            // act
            int inserted = transactionRepository.insertAll(requests);

            // assert & verify
            assertEquals(0, inserted);
        }
    }
}
