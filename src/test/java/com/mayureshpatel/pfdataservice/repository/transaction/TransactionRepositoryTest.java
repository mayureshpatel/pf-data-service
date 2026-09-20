package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
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
import org.springframework.jdbc.core.simple.JdbcClient;

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

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("Dynamic Filtering (Specification)")
    class FilterTests {
        @Test
        @DisplayName("should filter by type and user")
        void shouldFilterByType() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, TransactionType.INCOME, null, null, null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.getContent().stream().allMatch(t -> t.getType() == TransactionType.INCOME));
        }

        @Test
        @DisplayName("should filter by amount range")
        void shouldFilterByAmount() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, new BigDecimal("1000.00"), new BigDecimal("2000.00"), null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.getContent().stream().allMatch(t -> 
                t.getAmount().compareTo(new BigDecimal("1000.00")) >= 0 && 
                t.getAmount().compareTo(new BigDecimal("2000.00")) <= 0
            ));
        }

        @Test
        @DisplayName("should filter by an inclusive start/end date range")
        void shouldFilterByDateRange() {
            // arrange -- baseline's 3 specific transactions (1000, 1001, 1002) fall on
            // 2026-03-01/02/03; 1002 is timestamped 12:00:00, well after midnight on the end date
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, null, null,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3), null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            List<Long> ids = result.getContent().stream().map(Transaction::getId).toList();
            assertTrue(ids.containsAll(List.of(1000L, 1001L, 1002L)),
                    "expected all three transactions on or between the start and end dates, including " +
                    "1002 which falls later in the day on the end date itself -- got: " + ids);
        }

        @Test
        @DisplayName("should exclude transactions outside the date range")
        void shouldExcludeTransactionsOutsideDateRange() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, null, null,
                    LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2), null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify -- only 1001 is on 2026-03-02; 1000 and 1002 must not appear
            List<Long> ids = result.getContent().stream().map(Transaction::getId).toList();
            assertTrue(ids.contains(1001L));
            assertFalse(ids.contains(1000L));
            assertFalse(ids.contains(1002L));
        }

        @Test
        @DisplayName("should filter by category name substring, case-insensitively")
        void shouldFilterByCategoryName() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, "gas", null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify -- only transaction 1001 is categorized as Gas
            assertEquals(1, result.getTotalElements());
            assertEquals(1001L, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("should treat the literal string \"null\" as a sentinel for uncategorized transactions")
        void shouldFilterByNullCategorySentinel() {
            // arrange -- 1002 (ATM Deposit) has no category in the baseline
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, "null", null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            assertTrue(result.getContent().stream().anyMatch(t -> t.getId().equals(1002L)));
            assertTrue(result.getContent().stream().allMatch(t -> t.getCategory() == null));
        }

        @Test
        @DisplayName("should filter by description substring, case-insensitively")
        void shouldFilterByDescription() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, "morning", null, null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals("Morning Coffee", result.getContent().get(0).getDescription());
        }

        @Test
        @DisplayName("should filter by merchant clean name substring, case-insensitively")
        void shouldFilterByMerchantCleanName() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, "whole", null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 50)
            );

            // assert & verify -- every grocery-run transaction is linked to the Whole Foods merchant
            assertFalse(result.getContent().isEmpty());
            assertTrue(result.getContent().stream()
                    .allMatch(t -> t.getMerchant() != null && t.getMerchant().getName().equals("Whole Foods")));
        }

        @Test
        @DisplayName("should filter by account id")
        void shouldFilterByAccountId() {
            // arrange -- account 3 (Credit Card) has exactly one transaction (1001)
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    3L, null, null, null, null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals(1001L, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("PF-308: should filter by tag id")
        void shouldFilterByTagId() {
            // arrange -- baseline transaction_tags assigns tag 1 to transaction 1001 only
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, null, null, null, null, 1L
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals(1001L, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("PF-308: a tag filter should not duplicate a transaction that also carries "
                + "other tags -- EXISTS, not a JOIN that could multiply rows")
        void shouldNotDuplicateRowsWhenTransactionHasMultipleTags() {
            // arrange -- assign a second tag to the same transaction (1001) already carrying tag 1
            jdbcClient.sql("insert into tags (id, user_id, name) values (999, :userId, 'Second Tag')")
                    .param("userId", USER_ID)
                    .update();
            jdbcClient.sql("insert into transaction_tags (transaction_id, tag_id) values (1001, 999)")
                    .update();

            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, null, null, null, null, 1L
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify -- transaction 1001 appears exactly once despite carrying two tags
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("PF-308: results should include each transaction's assigned tags")
        void shouldIncludeTagsInResults() {
            // arrange -- account 3 (Credit Card) has exactly one transaction (1001), which the
            // baseline assigns tag id 1 to
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    3L, null, null, null, null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            Transaction transaction = result.getContent().get(0);
            assertNotNull(transaction.getTags());
            assertTrue(transaction.getTags().stream().anyMatch(t -> t.getId().equals(1L)));
        }

        @Test
        @DisplayName("PF-308: a transaction with no assigned tags should return an empty list, not null")
        void shouldReturnEmptyTagsListWhenNoneAssigned() {
            // arrange -- transaction 1000 carries no tags in the baseline
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    1L, null, null, null, null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify
            Transaction transaction1000 = result.getContent().stream()
                    .filter(t -> t.getId().equals(1000L))
                    .findFirst().orElseThrow();
            assertNotNull(transaction1000.getTags());
            assertTrue(transaction1000.getTags().isEmpty());
        }

        @Test
        @DisplayName("should expand the TRANSFER pseudo-type into an IN clause matching all transfer directions")
        void shouldExpandTransferTypeToInClause() {
            // arrange -- mark 1000 and 1002 as a confirmed transfer pair
            Transaction t1000 = transactionRepository.findById(1000L, USER_ID).orElseThrow();
            Transaction t1002 = transactionRepository.findById(1002L, USER_ID).orElseThrow();
            transactionRepository.update(USER_ID, t1000.toBuilder().type(TransactionType.TRANSFER_OUT).build());
            transactionRepository.update(USER_ID, t1002.toBuilder().type(TransactionType.TRANSFER_IN).build());

            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, TransactionType.TRANSFER, null, null, null, null, null, null, null, null
            );

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter), PageRequest.of(0, 10)
            );

            // assert & verify -- the TRANSFER filter must match both TRANSFER_IN and TRANSFER_OUT rows
            List<Long> ids = result.getContent().stream().map(Transaction::getId).toList();
            assertTrue(ids.contains(1000L));
            assertTrue(ids.contains(1002L));
        }

        @Test
        @DisplayName("should cap page content at the requested page size even when more rows match")
        void shouldRespectPageSize() {
            // act -- baseline has ~39 transactions for user 1; request only 5
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, new TransactionSpecification.TransactionFilter(
                            null, null, null, null, null, null, null, null, null, null)),
                    PageRequest.of(0, 5)
            );

            // assert & verify
            assertEquals(5, result.getContent().size());
            assertTrue(result.getTotalElements() > 5);
        }

        @Test
        @DisplayName("should return distinct, non-overlapping content across consecutive pages")
        void shouldPaginateAcrossPages() {
            // arrange
            TransactionSpecification.FilterResult spec = TransactionSpecification.withFilter(
                    USER_ID, new TransactionSpecification.TransactionFilter(
                            null, null, null, null, null, null, null, null, null, null));

            // act
            Page<Transaction> page0 = transactionRepository.findAll(spec, PageRequest.of(0, 10));
            Page<Transaction> page1 = transactionRepository.findAll(spec, PageRequest.of(1, 10));

            // assert & verify
            List<Long> page0Ids = page0.getContent().stream().map(Transaction::getId).toList();
            List<Long> page1Ids = page1.getContent().stream().map(Transaction::getId).toList();
            assertEquals(10, page0Ids.size());
            assertEquals(10, page1Ids.size());
            assertTrue(page0Ids.stream().noneMatch(page1Ids::contains),
                    "consecutive pages must not overlap");
            assertEquals(page0.getTotalElements(), page1.getTotalElements(),
                    "total element count must be stable across pages of the same filter");
        }

        @Test
        @DisplayName("should exclude soft-deleted transactions from dynamic query results")
        void shouldExcludeSoftDeletedTransactions() {
            // arrange
            jdbcClient.sql("UPDATE transactions SET deleted_at = NOW() WHERE id = :id")
                    .param("id", 1000L)
                    .update();

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, new TransactionSpecification.TransactionFilter(
                            null, null, null, null, null, null, null, null, null, null)),
                    PageRequest.of(0, 100)
            );

            // assert & verify
            assertTrue(result.getContent().stream().noneMatch(t -> t.getId().equals(1000L)));
        }

        @Test
        @DisplayName("should safely handle invalid sort direction and property")
        void shouldHandleInvalidSort() {
            // arrange
            TransactionSpecification.TransactionFilter filter = new TransactionSpecification.TransactionFilter(
                    null, null, null, null, null, null, null, null, null, null
            );
            // Try to inject SQL in Sort direction and property
            Sort maliciousSort = Sort.by(Sort.Order.desc("date; DROP TABLE transactions; --"));

            // act
            Page<Transaction> result = transactionRepository.findAll(
                    TransactionSpecification.withFilter(USER_ID, filter),
                    PageRequest.of(0, 10, maliciousSort)
            );

            // assert & verify
            assertFalse(result.isEmpty()); // Should not crash and should return data
        }

        @Test
        @DisplayName("should find all transactions for a user across all their accounts")
        void shouldFindByUserId() {
            // act
            List<Transaction> result = transactionRepository.findByUserId(USER_ID);

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(t -> t.getAccount().getUserId().equals(USER_ID)));
            // account 3 (Credit Card) belongs to user 1 too -- confirms the join isn't scoped to one account
            assertTrue(result.stream().anyMatch(t -> t.getAccount().getId().equals(3L)));
        }

        @Test
        @DisplayName("should find recent non-transfer transactions since a date")
        void shouldFindRecentNonTransferTransactions() {
            // act
            List<Transaction> result = transactionRepository.findRecentNonTransferTransactions(
                    USER_ID, LocalDate.of(2026, 3, 1));

            // assert & verify
            assertEquals(3, result.size()); // baseline's 3 specific transactions (1000, 1001, 1002)
            assertTrue(result.stream().noneMatch(t -> t.getType() == TransactionType.TRANSFER
                    || t.getType() == TransactionType.TRANSFER_IN
                    || t.getType() == TransactionType.TRANSFER_OUT));
        }

        @Test
        @DisplayName("should find expenses since a date, excluding income")
        void shouldFindExpensesSince() {
            // act
            List<Transaction> result = transactionRepository.findExpensesSince(USER_ID, LocalDate.of(2026, 3, 1));

            // assert & verify
            assertEquals(2, result.size()); // 1000 (Coffee) and 1001 (Gas), not 1002 (INCOME)
            assertTrue(result.stream().allMatch(t -> t.getType() == TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("should find existing transactions in a date range for duplicate-import checks")
        void shouldFindExistingForDuplicateCheck() {
            // act
            List<Transaction> result = transactionRepository.findExistingForDuplicateCheck(
                    1L,
                    OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                    OffsetDateTime.parse("2026-03-03T23:59:59Z"));

            // assert & verify -- account 1's transactions 1000 and 1002 fall in range; 1001 belongs to account 3
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(t -> t.getAccount().getId().equals(1L)));
        }

        @Test
        @DisplayName("should find transactions by a list of ids, scoped to the user")
        void shouldFindAllById() {
            // act
            List<Transaction> result = transactionRepository.findAllById(USER_ID, List.of(1000L, 1002L));

            // assert & verify
            assertEquals(2, result.size());
            assertTrue(result.stream().map(Transaction::getId).toList().containsAll(List.of(1000L, 1002L)));
        }

        @Test
        @DisplayName("should return empty list for findAllById with no ids")
        void shouldReturnEmptyForFindAllByIdWithNoIds() {
            // act
            List<Transaction> result = transactionRepository.findAllById(USER_ID, List.of());

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should find categories that have at least one transaction (subcategories only)")
        void shouldGetCategoriesWithTransactions() {
            // act
            List<Category> result = transactionRepository.getCategoriesWithTransactions(USER_ID);

            // assert & verify -- Rent, Groceries, Dining Out, Gas, Salary all have transactions in the baseline;
            // parent categories (e.g. "Food") are excluded by the query's own parent_id filter
            assertEquals(5, result.size());
            assertTrue(result.stream().allMatch(c -> c.getParentId() != null));
            assertTrue(result.stream().anyMatch(c -> c.getName().equals("Rent")));
            assertTrue(result.stream().anyMatch(c -> c.getName().equals("Groceries")));
        }

        @Test
        @DisplayName("should find merchants that have at least one transaction, excluding null-merchant transactions")
        void shouldGetMerchantsWithTransactions() {
            // act
            List<Merchant> result = transactionRepository.getMerchantsWithTransactions(USER_ID);

            // assert & verify -- Whole Foods (groceries), Shell (1001), My Favorite Cafe (1000) all qualify;
            // Rent/Salary/1002 have no merchant and must NOT produce a null entry in the list
            // (regression test: MERCHANTS_WITH_TRANSACTIONS previously had no `merchants.id is not
            // null` guard, so a user with any merchant-less transaction got a literal null element
            // in this list -- which MerchantDtoMapper.toDto() would pass straight through into the
            // JSON response as a null array entry)
            assertTrue(result.stream().noneMatch(java.util.Objects::isNull));
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(m -> m.getName().equals("Whole Foods")));
            assertTrue(result.stream().anyMatch(m -> m.getName().equals("Shell")));
            assertTrue(result.stream().anyMatch(m -> m.getName().equals("My Favorite Cafe")));
        }
    }

    @Nested
    @DisplayName("Aggregations")
    class AggregationTests {
        @Test
        @DisplayName("should calculate sum for date range and type")
        void shouldCalculateSum() {
            // arrange
            OffsetDateTime start = LocalDate.of(2026, 3, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // act
            BigDecimal sum = transactionRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME);

            // assert & verify
            // Based on baseline: 1002 is 500.00 INCOME
            assertEquals(0, new BigDecimal("500.00").compareTo(sum));
        }

        @Test
        @DisplayName("should find category totals")
        void shouldFindCategoryTotals() {
            // arrange
            OffsetDateTime start = LocalDate.of(2026, 3, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // act
            List<CategoryBreakdownDto> result = transactionRepository.findCategoryTotals(USER_ID, start, end);

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.stream()
                    .filter(b -> b.category() != null)
                    .anyMatch(b -> b.category().name().equals("Dining Out")));
        }

        @Test
        @DisplayName("should find monthly sums for cash flow trend")
        void shouldFindMonthlySums() {
            // act
            List<Object[]> result = transactionRepository.findMonthlySums(USER_ID, LocalDate.of(2025, 9, 1));

            // assert & verify
            assertFalse(result.isEmpty());
            // result is [year, month, type, sum]
            Object[] first = result.get(0);
            assertEquals(4, first.length);
        }

        @Test
        @DisplayName("should return zero for uncategorized expense totals when everything is categorized")
        void shouldGetZeroUncategorizedExpenseTotalsWhenAllCategorized() {
            // act -- every EXPENSE transaction in the baseline already has a category
            BigDecimal result = transactionRepository.getUncategorizedExpenseTotals(USER_ID);

            // assert & verify
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("should sum uncategorized expenses once one exists")
        void shouldGetUncategorizedExpenseTotals() {
            // arrange
            TransactionCreateRequest uncategorizedExpense = TransactionCreateRequest.builder()
                    .accountId(1L)
                    .amount(new BigDecimal("42.00"))
                    .transactionDate(OffsetDateTime.parse("2026-03-05T00:00:00Z"))
                    .description("Uncategorized Purchase")
                    .type(TransactionType.EXPENSE.name())
                    .build();
            transactionRepository.insert(uncategorizedExpense);

            // act
            BigDecimal result = transactionRepository.getUncategorizedExpenseTotals(USER_ID);

            // assert & verify
            assertEquals(0, new BigDecimal("42.00").compareTo(result));
        }

        @Test
        @DisplayName("should calculate net flow after a date, income positive and expense negative")
        void shouldGetNetFlowAfterDate() {
            // act -- only 1000 (EXPENSE 25.50) and 1002 (INCOME 500.00) on account 1 are after 2026-02-28
            BigDecimal result = transactionRepository.getNetFlowAfterDate(1L, LocalDate.of(2026, 2, 28));

            // assert & verify
            assertEquals(0, new BigDecimal("474.50").compareTo(result));
        }

        @Test
        @DisplayName("should return zero net flow after a date with no later transactions")
        void shouldGetZeroNetFlowAfterLatestDate() {
            // act
            BigDecimal result = transactionRepository.getNetFlowAfterDate(1L, LocalDate.of(2026, 12, 31));

            // assert & verify
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    @DisplayName("Status & Counts")
    class StatusTests {
        @Test
        @DisplayName("should get count by category")
        void shouldGetCountByCategory() {
            // act
            List<CategoryTransactionsDto> result = transactionRepository.getCountByCategory(USER_ID);

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.stream()
                    .filter(c -> c.category() != null)
                    .anyMatch(c -> c.category().name().equals("Groceries")));
        }

        @Test
        @DisplayName("should count transactions for a specific account")
        void shouldCountByAccountId() {
            // act -- account 3 (Credit Card) has exactly one baseline transaction (1001)
            long count = transactionRepository.countByAccountId(3L);

            // assert & verify
            assertEquals(1, count);
        }

        @Test
        @DisplayName("should count transactions for a specific category")
        void shouldCountByCategoryId() {
            // act -- category 9 (Gas) has exactly one baseline transaction (1001)
            long count = transactionRepository.countByCategoryId(9L);

            // assert & verify
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
            // arrange
            List<Transaction> toDelete = transactionRepository.findAllById(USER_ID, List.of(1000L, 1002L));

            // act
            transactionRepository.deleteAll(USER_ID, toDelete);

            // assert & verify
            assertTrue(transactionRepository.findAllById(USER_ID, List.of(1000L, 1002L)).isEmpty());
        }

        @Test
        @DisplayName("should not fail when deleting a list containing a transaction with no ID")
        void shouldSkipNullIdOnDeleteAll() {
            // arrange
            Transaction noId = Transaction.builder().build();

            // act & assert & verify -- must not throw
            assertDoesNotThrow(() -> transactionRepository.deleteAll(USER_ID, List.of(noId)));
        }

        @Test
        @DisplayName("bug regression: insert(Transaction) must persist a fully-resolved transaction "
                + "and return its real generated id -- this overload is unrelated to "
                + "insert(TransactionCreateRequest) (different parameter type, not an override), so "
                + "TransactionService.createTransaction's call was previously binding to "
                + "JdbcRepository's throwing default and failing every single-transaction create")
        void shouldInsertResolvedTransaction() {
            // arrange -- mirrors what TransactionService.createTransaction builds: a fully-resolved
            // Transaction with account/category/merchant already set, not the raw create request
            Transaction transaction = Transaction.builder()
                    .account(com.mayureshpatel.pfdataservice.domain.account.Account.builder().id(1L).build())
                    .category(Category.builder().id(9L).build())
                    .amount(new BigDecimal("15.00"))
                    .transactionDate(OffsetDateTime.parse("2026-03-10T00:00:00Z"))
                    .description("New Resolved Transaction")
                    .type(TransactionType.EXPENSE)
                    .merchant(Merchant.builder().id(1L).build())
                    .build();

            // act
            int newId = transactionRepository.insert(transaction);

            // assert & verify -- the returned id must resolve back to the row just inserted
            Transaction persisted = transactionRepository.findById((long) newId, USER_ID).orElseThrow();
            assertEquals("New Resolved Transaction", persisted.getDescription());
            assertEquals(0, new BigDecimal("15.00").compareTo(persisted.getAmount()));
        }

        @Test
        @DisplayName("insert(Transaction) must not throw when category or merchant is unset")
        void shouldInsertResolvedTransactionWithoutCategoryOrMerchant() {
            // arrange
            Transaction transaction = Transaction.builder()
                    .account(com.mayureshpatel.pfdataservice.domain.account.Account.builder().id(1L).build())
                    .amount(new BigDecimal("20.00"))
                    .transactionDate(OffsetDateTime.parse("2026-03-10T00:00:00Z"))
                    .description("No Category Or Merchant")
                    .type(TransactionType.INCOME)
                    .build();

            // act & assert -- must not throw
            assertDoesNotThrow(() -> transactionRepository.insert(transaction));
        }
    }

    @Nested
    @DisplayName("PF-823: Reports server-side aggregation (Categories, Cash Flow)")
    class ReportDataAggregation {

        /**
         * Seeds 1,500 EXPENSE transactions for user 1 (well past the 1000-row cap PF-823 fixes),
         * $10 each, one per day starting 2020-01-01 -- category 7 (Groceries), merchant 1 (Whole
         * Foods). A single set-based INSERT rather than 1,500 round trips.
         */
        private void seedBulkGroceryTransactions() {
            jdbcClient.sql("""
                    insert into transactions (account_id, category_id, merchant_id, amount, date, description, type)
                    select 1, 7, 1, 10.00, (date '2020-01-01' + s.n)::timestamptz, 'Bulk Test Txn ' || s.n, 'EXPENSE'
                    from generate_series(0, 1499) as s(n)
                    """).update();
        }

        @Test
        @DisplayName("findCategoryReportData sums every matching transaction, not just the newest 1000")
        void shouldAggregateCategoryTotalsPastThousandRows() {
            // arrange
            seedBulkGroceryTransactions();

            // act -- range covers all 1,500 seeded days plus margin
            List<CategoryReportDataDto> result = transactionRepository.findCategoryReportData(
                    USER_ID,
                    OffsetDateTime.parse("2020-01-01T00:00:00Z"),
                    OffsetDateTime.parse("2024-12-31T00:00:00Z"));

            // assert & verify
            CategoryReportDataDto groceries = result.stream()
                    .filter(r -> r.category() != null && r.category().id().equals(7L))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("expected a Groceries entry, got: " + result));
            assertEquals(1500L, groceries.count(),
                    "a 1000-row cap would silently drop 500 of these -- got count=" + groceries.count());
            assertEquals(0, new BigDecimal("15000.00").compareTo(groceries.total()),
                    "expected 1500 * $10.00, got " + groceries.total());
        }

        @Test
        @DisplayName("findCategoryReportData excludes uncategorized transactions, matching the old "
                + "client-side aggregateByCategory()'s `&& txn.category` check")
        void shouldExcludeUncategorizedFromCategoryReportData() {
            // arrange -- an otherwise-empty future month containing only an uncategorized expense
            jdbcClient.sql("""
                    insert into transactions (account_id, merchant_id, amount, date, description, type)
                    values (1, null, 99.00, '2031-07-01T00:00:00Z', 'Uncategorized Spend', 'EXPENSE')
                    """).update();

            // act
            List<CategoryReportDataDto> result = transactionRepository.findCategoryReportData(
                    USER_ID,
                    OffsetDateTime.parse("2031-07-01T00:00:00Z"),
                    OffsetDateTime.parse("2031-07-02T00:00:00Z"));

            // assert & verify -- the inner join to categories drops the uncategorized row entirely
            assertTrue(result.isEmpty(), "expected no category entries at all, got: " + result);
        }

        @Test
        @DisplayName("findMonthlyIncomeExpense includes the oldest in-range month, not just the newest")
        void shouldIncludeOldestMonthPastThousandRows() {
            // arrange -- the old bug sorted date desc and kept only the newest 1000, silently
            // dropping the oldest ~500 days (all of 2020's January-ish window) from the range
            seedBulkGroceryTransactions();

            // act
            List<MonthlyReportDataDto> result = transactionRepository.findMonthlyIncomeExpense(
                    USER_ID,
                    OffsetDateTime.parse("2020-01-01T00:00:00Z"),
                    OffsetDateTime.parse("2024-12-31T00:00:00Z"));

            // assert & verify -- the oldest seeded month (Jan 2020) must be present and correctly summed
            MonthlyReportDataDto oldestMonth = result.stream()
                    .filter(m -> m.year() == 2020 && m.month() == 1)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("expected a 2020-01 entry, got: " + result));
            assertEquals(0, new BigDecimal("310.00").compareTo(oldestMonth.expense()),
                    "expected 31 days * $10.00 for January 2020, got " + oldestMonth.expense());

            BigDecimal totalAcrossAllMonths = result.stream()
                    .map(MonthlyReportDataDto::expense)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, new BigDecimal("15000.00").compareTo(totalAcrossAllMonths),
                    "sum across every returned month must account for all 1500 seeded transactions, got "
                            + totalAcrossAllMonths);
        }

        @Test
        @DisplayName("findMonthlyIncomeExpense excludes transfers from both income and expense")
        void shouldExcludeTransfersFromMonthlyIncomeExpense() {
            // arrange -- an otherwise-empty future month containing only a transfer pair (the
            // Synovus-pays-off-credit-card pattern: TRANSFER_OUT on the bank side, TRANSFER_IN on
            // the card side), so a non-empty result here can only mean a transfer leaked through
            jdbcClient.sql("""
                    insert into transactions (account_id, merchant_id, amount, date, description, type)
                    values (1, null, 500.00, '2031-05-15T00:00:00Z', 'Card Payoff Out', 'TRANSFER_OUT'),
                           (3, null, 500.00, '2031-05-15T00:00:00Z', 'Card Payoff In', 'TRANSFER_IN')
                    """).update();

            // act
            List<MonthlyReportDataDto> result = transactionRepository.findMonthlyIncomeExpense(
                    USER_ID,
                    OffsetDateTime.parse("2031-05-01T00:00:00Z"),
                    OffsetDateTime.parse("2031-05-31T00:00:00Z"));

            // assert & verify -- with type in ('INCOME','EXPENSE') filtering transfers out before
            // the group by, a month containing only transfer legs produces no row at all
            assertTrue(result.isEmpty(),
                    "a month with only TRANSFER_OUT/TRANSFER_IN legs must produce no income/expense row, got: "
                            + result);
        }
    }
}
