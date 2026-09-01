package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
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

        @Test
        @DisplayName("should find all transactions for a user across all their accounts")
        void shouldFindByUserId() {
            // Act
            List<Transaction> result = transactionRepository.findByUserId(USER_ID);

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(t -> t.getAccount().getUserId().equals(USER_ID)));
            // account 3 (Credit Card) belongs to user 1 too -- confirms the join isn't scoped to one account
            assertTrue(result.stream().anyMatch(t -> t.getAccount().getId().equals(3L)));
        }

        @Test
        @DisplayName("should find recent non-transfer transactions since a date")
        void shouldFindRecentNonTransferTransactions() {
            // Act
            List<Transaction> result = transactionRepository.findRecentNonTransferTransactions(
                    USER_ID, LocalDate.of(2026, 3, 1));

            // Assert
            assertEquals(3, result.size()); // baseline's 3 specific transactions (1000, 1001, 1002)
            assertTrue(result.stream().noneMatch(t -> t.getType() == TransactionType.TRANSFER
                    || t.getType() == TransactionType.TRANSFER_IN
                    || t.getType() == TransactionType.TRANSFER_OUT));
        }

        @Test
        @DisplayName("should find expenses since a date, excluding income")
        void shouldFindExpensesSince() {
            // Act
            List<Transaction> result = transactionRepository.findExpensesSince(USER_ID, LocalDate.of(2026, 3, 1));

            // Assert
            assertEquals(2, result.size()); // 1000 (Coffee) and 1001 (Gas), not 1002 (INCOME)
            assertTrue(result.stream().allMatch(t -> t.getType() == TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should find existing transactions in a date range for duplicate-import checks")
        void shouldFindExistingForDuplicateCheck() {
            // Act
            List<Transaction> result = transactionRepository.findExistingForDuplicateCheck(
                    1L,
                    OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                    OffsetDateTime.parse("2026-03-03T23:59:59Z"));

            // Assert -- account 1's transactions 1000 and 1002 fall in range; 1001 belongs to account 3
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(t -> t.getAccount().getId().equals(1L)));
        }

        @Test
        @DisplayName("should find transactions by a list of ids, scoped to the user")
        void shouldFindAllById() {
            // Act
            List<Transaction> result = transactionRepository.findAllById(USER_ID, List.of(1000L, 1002L));

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.stream().map(Transaction::getId).toList().containsAll(List.of(1000L, 1002L)));
        }

        @Test
        @DisplayName("should return empty list for findAllById with no ids")
        void shouldReturnEmptyForFindAllByIdWithNoIds() {
            // Act
            List<Transaction> result = transactionRepository.findAllById(USER_ID, List.of());

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should find transactions with account and user details by id list (currently an alias for findAllById)")
        void shouldFindAllByIdWithAccountAndUser() {
            // Act
            List<Transaction> result = transactionRepository.findAllByIdWithAccountAndUser(USER_ID, List.of(1000L));

            // Assert
            assertEquals(1, result.size());
            assertEquals(1000L, result.get(0).getId());
            assertNotNull(result.get(0).getAccount());
        }

        @Test
        @DisplayName("should find categories that have at least one transaction (subcategories only)")
        void shouldGetCategoriesWithTransactions() {
            // Act
            List<Category> result = transactionRepository.getCategoriesWithTransactions(USER_ID);

            // Assert -- Rent, Groceries, Dining Out, Gas, Salary all have transactions in the baseline;
            // parent categories (e.g. "Food") are excluded by the query's own parent_id filter
            assertEquals(5, result.size());
            assertTrue(result.stream().allMatch(c -> c.getParentId() != null));
            assertTrue(result.stream().anyMatch(c -> c.getName().equals("Rent")));
            assertTrue(result.stream().anyMatch(c -> c.getName().equals("Groceries")));
        }

        @Test
        @DisplayName("should find merchants that have at least one transaction, excluding null-merchant transactions")
        void shouldGetMerchantsWithTransactions() {
            // Act
            List<Merchant> result = transactionRepository.getMerchantsWithTransactions(USER_ID);

            // Assert -- Whole Foods (groceries), Shell (1001), My Favorite Cafe (1000) all qualify;
            // Rent/Salary/1002 have no merchant and must NOT produce a null entry in the list
            // (regression test: MERCHANTS_WITH_TRANSACTIONS previously had no `merchants.id is not
            // null` guard, so a user with any merchant-less transaction got a literal null element
            // in this list -- which MerchantDtoMapper.toDto() would pass straight through into the
            // JSON response as a null array entry)
            assertTrue(result.stream().noneMatch(java.util.Objects::isNull));
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(m -> m.getCleanName().equals("Whole Foods")));
            assertTrue(result.stream().anyMatch(m -> m.getCleanName().equals("Shell")));
            assertTrue(result.stream().anyMatch(m -> m.getCleanName().equals("My Favorite Cafe")));
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

        @Test
        @DisplayName("should return zero for uncategorized expense totals when everything is categorized")
        void shouldGetZeroUncategorizedExpenseTotalsWhenAllCategorized() {
            // Act -- every EXPENSE transaction in the baseline already has a category
            BigDecimal result = transactionRepository.getUncategorizedExpenseTotals(USER_ID);

            // Assert
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("should sum uncategorized expenses once one exists")
        void shouldGetUncategorizedExpenseTotals() {
            // Arrange
            TransactionCreateRequest uncategorizedExpense = TransactionCreateRequest.builder()
                    .accountId(1L)
                    .amount(new BigDecimal("42.00"))
                    .transactionDate(OffsetDateTime.parse("2026-03-05T00:00:00Z"))
                    .description("Uncategorized Purchase")
                    .type(TransactionType.EXPENSE.name())
                    .build();
            transactionRepository.insert(uncategorizedExpense);

            // Act
            BigDecimal result = transactionRepository.getUncategorizedExpenseTotals(USER_ID);

            // Assert
            assertEquals(0, new BigDecimal("42.00").compareTo(result));
        }

        @Test
        @DisplayName("should calculate net flow after a date, income positive and expense negative")
        void shouldGetNetFlowAfterDate() {
            // Act -- only 1000 (EXPENSE 25.50) and 1002 (INCOME 500.00) on account 1 are after 2026-02-28
            BigDecimal result = transactionRepository.getNetFlowAfterDate(1L, LocalDate.of(2026, 2, 28));

            // Assert
            assertEquals(0, new BigDecimal("474.50").compareTo(result));
        }

        @Test
        @DisplayName("should return zero net flow after a date with no later transactions")
        void shouldGetZeroNetFlowAfterLatestDate() {
            // Act
            BigDecimal result = transactionRepository.getNetFlowAfterDate(1L, LocalDate.of(2026, 12, 31));

            // Assert
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
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

        @Test
        @DisplayName("should count transactions for a specific account")
        void shouldCountByAccountId() {
            // Act -- account 3 (Credit Card) has exactly one baseline transaction (1001)
            long count = transactionRepository.countByAccountId(3L);

            // Assert
            assertEquals(1, count);
        }

        @Test
        @DisplayName("should count transactions for a specific category")
        void shouldCountByCategoryId() {
            // Act -- category 9 (Gas) has exactly one baseline transaction (1001)
            long count = transactionRepository.countByCategoryId(9L);

            // Assert
            assertEquals(1, count);
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

        @Test
        @DisplayName("should delete multiple transactions in one call")
        void shouldDeleteAll() {
            // Arrange
            List<Transaction> toDelete = transactionRepository.findAllById(USER_ID, List.of(1000L, 1002L));

            // Act
            transactionRepository.deleteAll(USER_ID, toDelete);

            // Assert
            assertTrue(transactionRepository.findAllById(USER_ID, List.of(1000L, 1002L)).isEmpty());
        }

        @Test
        @DisplayName("should not fail when deleting a list containing a transaction with no ID")
        void shouldSkipNullIdOnDeleteAll() {
            // Arrange
            Transaction noId = Transaction.builder().build();

            // Act & Assert -- must not throw
            assertDoesNotThrow(() -> transactionRepository.deleteAll(USER_ID, List.of(noId)));
        }
    }
}
