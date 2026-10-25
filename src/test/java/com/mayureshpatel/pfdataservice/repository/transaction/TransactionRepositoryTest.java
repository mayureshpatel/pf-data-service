package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDescriptionDto;
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
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code TransactionRepository} -- the largest and most heavily-queried repository in this codebase -- against a real PostgreSQL instance (via {@link BaseRepositoryTest}'s Testcontainers setup and a shared baseline fixture), exercising dynamic filtering, aggregation, and batch writes directly rather than mocking them. */
@Import({TransactionRepository.class, TransactionReportRepository.class})
@DisplayName("TransactionRepository Integration Tests (PostgreSQL)")
class TransactionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionReportRepository transactionReportRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long USER_ID = 1L;
    private static final String AMOUNT = "42.00";

    /**
     * {@code findAll(TransactionSpecification, Pageable)} builds its {@code WHERE} clause
     * dynamically from whichever {@link TransactionSpecification.TransactionFilter} fields are
     * non-null: type, amount range, an inclusive start/end date range (confirmed to include
     * activity later in the day on the end date itself, not just before midnight), and
     * case-insensitive substring matches on category name, description, and merchant name. The
     * literal string {@code "null"} is a special sentinel for the category filter meaning
     * "uncategorized," and the {@code TRANSFER} pseudo-type expands into an {@code IN} clause
     * matching both {@code TRANSFER_IN} and {@code TRANSFER_OUT} rows. A PF-308 case: filtering by
     * tag id uses an {@code EXISTS} subquery rather than a {@code JOIN}, so a transaction carrying
     * multiple tags isn't duplicated in the results, and every result's own {@code tags} list is
     * populated -- empty, not null, when none are assigned. Soft-deleted transactions never appear,
     * pagination caps page content at the requested size with stable, non-overlapping consecutive
     * pages, and an attempted SQL-injection payload in the {@link Sort} property/direction is
     * handled safely rather than crashing or executing. Beyond the dynamic specification, this
     * class also covers several fixed-query finder methods: {@code findByUserId} (joins across all
     * of a user's accounts, not just one), {@code findRecentNonTransferTransactions}, {@code
     * findExpensesSince}, {@code findExistingForDuplicateCheck}, {@code findAllById} (user-scoped,
     * empty-input-safe), and {@code getCategoriesWithTransactions}/{@code
     * getMerchantsWithTransactions} (each excluding parent categories or merchant-less transactions
     * respectively -- the merchants one is a confirmed regression test: the underlying query
     * previously had no {@code merchants.id is not null} guard, so a user with any merchant-less
     * transaction got a literal null element in the list, which {@code MerchantDtoMapper} would
     * have passed straight through into the JSON response as a null array entry).
     */
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
                    .allMatch(t -> t.getMerchant() != null && "Whole Foods".equals(t.getMerchant().getName())));
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
            assertTrue(result.stream().anyMatch(c -> "Rent".equals(c.getName())));
            assertTrue(result.stream().anyMatch(c -> "Groceries".equals(c.getName())));
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
            assertTrue(result.stream().anyMatch(m -> "Whole Foods".equals(m.getName())));
            assertTrue(result.stream().anyMatch(m -> "Shell".equals(m.getName())));
            assertTrue(result.stream().anyMatch(m -> "My Favorite Cafe".equals(m.getName())));
        }
    }

    /**
     * {@code getSumByDateRange}/{@code findCategoryTotals}/{@code findMonthlySums}/{@code
     * getUncategorizedExpenseTotals}/{@code getNetFlowAfterDate} each aggregate real baseline
     * transaction amounts correctly over a date range or category. A PF-848 case: {@code
     * findTransferInOnCreditCardAccounts} finds TRANSFER_IN rows only on credit-card accounts,
     * never on a checking account carrying the same (mis-typed) value. A PF-828 regression case:
     * {@code getSumByDateRange}'s end boundary doesn't round a sub-microsecond-precision instant up
     * into the next day -- Postgres's {@code timestamptz} is only microsecond-resolution and
     * *rounds* (not truncates) anything finer, so a naively-constructed 9-digit-nanosecond "end of
     * Dec 31" boundary would silently become midnight Jan 1 and pull a next-year transaction into
     * the prior year's sum; this test uses the maximum exact-microsecond value instead and confirms
     * live via {@code psql} which literal actually round-trips unchanged.
     */
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
            BigDecimal sum = transactionReportRepository.getSumByDateRange(USER_ID, start, end, TransactionType.INCOME);

            // assert & verify
            // Based on baseline: 1002 is 500.00 INCOME
            assertEquals(0, new BigDecimal("500.00").compareTo(sum));
        }

        @Test
        @DisplayName("bug regression: an end boundary finer than microsecond precision must not roll "
                + "over into the next day (PF-828) -- Postgres's timestamptz only has microsecond "
                + "resolution and rounds rather than truncates, so a 9-digit-nanosecond boundary meant "
                + "as \"end of Dec 31\" silently becomes midnight Jan 1, pulling a next-year transaction "
                + "into the prior year's sum")
        void shouldNotRoundYearEndBoundaryIntoNextDay() {
            // arrange -- a transaction dated exactly midnight UTC on Jan 1 of the following year,
            // the same shape every transaction in this app's real data takes (midnight-UTC-anchored)
            TransactionCreateRequest nextYearMidnight = TransactionCreateRequest.builder()
                    .accountId(1L)
                    .amount(new BigDecimal("26.99"))
                    .transactionDate(OffsetDateTime.parse("2025-01-01T00:00:00Z"))
                    .description("HLU*HULUPLUS")
                    .type(TransactionType.EXPENSE.name())
                    .build();
            transactionRepository.insert(nextYearMidnight);

            OffsetDateTime start = OffsetDateTime.parse("2024-01-01T00:00:00Z");
            // the maximum value representable at microsecond precision -- an exact multiple of
            // 1000ns, so it round-trips through Postgres unchanged. 999_999_999 (9 digits) would
            // round up to 2025-01-01T00:00:00Z instead, confirmed live via psql.
            OffsetDateTime end = OffsetDateTime.parse("2024-12-31T23:59:59.999999000Z");

            // act
            BigDecimal sum = transactionReportRepository.getSumByDateRange(USER_ID, start, end, TransactionType.EXPENSE);

            // assert & verify -- baseline has no other 2024 EXPENSE transactions for this user; any
            // non-zero result means the Jan-1-2025 transaction leaked into the 2024 range
            assertEquals(0, BigDecimal.ZERO.compareTo(sum));
        }

        @Test
        @DisplayName("should find category totals")
        void shouldFindCategoryTotals() {
            // arrange
            OffsetDateTime start = LocalDate.of(2026, 3, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // act
            List<CategoryBreakdownDto> result = transactionReportRepository.findCategoryTotals(USER_ID, start, end);

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.stream()
                    .filter(b -> b.category() != null)
                    .anyMatch(b -> "Dining Out".equals(b.category().name())));
        }

        @Test
        @DisplayName("should find TRANSFER_IN rows on credit-card accounts only (PF-848)")
        void shouldFindTransferInOnCreditCardAccountsOnly() {
            // arrange -- account 3 is the baseline's CREDIT_CARD account, account 1 is CHECKING
            TransactionCreateRequest onCreditCard = TransactionCreateRequest.builder()
                    .accountId(3L)
                    .amount(new BigDecimal(AMOUNT))
                    .transactionDate(OffsetDateTime.parse("2026-03-05T00:00:00Z"))
                    .description("Mis-typed refund")
                    .type(TransactionType.TRANSFER_IN.name())
                    .build();
            Long creditCardTxnId = (long) transactionRepository.insert(onCreditCard);

            TransactionCreateRequest onChecking = TransactionCreateRequest.builder()
                    .accountId(1L)
                    .amount(new BigDecimal(AMOUNT))
                    .transactionDate(OffsetDateTime.parse("2026-03-05T00:00:00Z"))
                    .description("Should not be found")
                    .type(TransactionType.TRANSFER_IN.name())
                    .build();
            transactionRepository.insert(onChecking);

            // act
            List<Transaction> result = transactionRepository.findTransferInOnCreditCardAccounts(USER_ID);

            // assert & verify
            List<Long> ids = result.stream().map(Transaction::getId).toList();
            assertTrue(ids.contains(creditCardTxnId));
            assertTrue(result.stream().allMatch(t -> t.getAccount().getId().equals(3L)));
        }

        @Test
        @DisplayName("should find monthly sums for cash flow trend")
        void shouldFindMonthlySums() {
            // act
            List<Object[]> result = transactionReportRepository.findMonthlySums(USER_ID, LocalDate.of(2025, 9, 1));

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
            BigDecimal result = transactionReportRepository.getUncategorizedExpenseTotals(USER_ID);

            // assert & verify
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("should sum uncategorized expenses once one exists")
        void shouldGetUncategorizedExpenseTotals() {
            // arrange
            TransactionCreateRequest uncategorizedExpense = TransactionCreateRequest.builder()
                    .accountId(1L)
                    .amount(new BigDecimal(AMOUNT))
                    .transactionDate(OffsetDateTime.parse("2026-03-05T00:00:00Z"))
                    .description("Uncategorized Purchase")
                    .type(TransactionType.EXPENSE.name())
                    .build();
            transactionRepository.insert(uncategorizedExpense);

            // act
            BigDecimal result = transactionReportRepository.getUncategorizedExpenseTotals(USER_ID);

            // assert & verify
            assertEquals(0, new BigDecimal(AMOUNT).compareTo(result));
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

    /** {@code getCountByCategory}/{@code countByAccountId}/{@code countByCategoryId} each count real baseline transactions correctly for the given category or account. */
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
                    .anyMatch(c -> "Groceries".equals(c.category().name())));
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

    /**
     * {@code findById(id, userId)}/{@code update}/{@code deleteById(id, userId)} are all
     * ownership-scoped; the single-argument {@code findById(id)}/{@code deleteById(id)} overloads
     * are deliberately unsupported, matching the insecure-overload convention established
     * elsewhere. {@code updateAll}/{@code insertAll}/{@code deleteAll} operate on a batch in one
     * call -- {@code insertAll} is confirmed to correctly chunk a batch past its internal 500-row
     * limit (tested with 1,050 rows), and both {@code insertAll} and {@code deleteAll} handle an
     * empty or null-id-containing input without failing. A fixed bug: the single-entity {@code
     * insert(Transaction)} overload (distinct from {@code insert(TransactionCreateRequest)} --
     * different parameter type, not an override of it) previously had no implementation of its own,
     * so {@code TransactionService#createTransaction}'s call silently bound to {@code
     * JdbcRepository}'s throwing default and failed every single-transaction create; it now
     * persists a fully-resolved {@link Transaction} (account/category/merchant already set, as
     * {@code TransactionService} builds it) and returns its real generated id, and tolerates a
     * transaction with no category or merchant set without throwing.
     */
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

    /**
     * {@code findWeightedDefaultCategoryId} (PF-815, backing {@code MerchantDefaultCategoryStrategy})
     * scores each of a merchant's categorized transactions by exponential recency decay, sums per
     * category, and returns the highest-scoring category -- but only once it has at least
     * {@code minOccurrences} matching transactions. Merchant 2 (Amazon) carries zero transactions
     * in the baseline fixture, so every test here seeds its own in isolation without baseline
     * interference.
     */
    @Nested
    @DisplayName("PF-815: findWeightedDefaultCategoryId")
    class WeightedDefaultCategoryTests {

        private static final Long MERCHANT_AMAZON = 2L;
        private static final Long CATEGORY_GROCERIES = 7L;
        private static final Long CATEGORY_DINING_OUT = 8L;
        private static final long HALF_LIFE_SECONDS = 180L * 24 * 60 * 60;

        @Test
        @DisplayName("should prefer a recent minority category over an older, more frequent one")
        void shouldWeightByRecencyNotJustFrequency() {
            // arrange -- 5 old (300-day) Groceries transactions outnumber 2 recent (5-day) Dining
            // Out transactions, but the recent pair's decay-weighted score is still higher: a plain
            // most-common-category (mode) rule would pick Groceries here, proving this is genuinely
            // recency-weighted, not mode in disguise
            jdbcClient.sql("""
                    insert into transactions (account_id, category_id, merchant_id, amount, date, description, type)
                    select 1, :groceries, :merchant, 20.00, now() - interval '300 days', 'Old Groceries ' || s.n, 'EXPENSE'
                    from generate_series(1, 5) as s(n)
                    """)
                    .param("groceries", CATEGORY_GROCERIES)
                    .param("merchant", MERCHANT_AMAZON)
                    .update();
            jdbcClient.sql("""
                    insert into transactions (account_id, category_id, merchant_id, amount, date, description, type)
                    select 1, :diningOut, :merchant, 15.00, now() - interval '5 days', 'Recent Dining ' || s.n, 'EXPENSE'
                    from generate_series(1, 2) as s(n)
                    """)
                    .param("diningOut", CATEGORY_DINING_OUT)
                    .param("merchant", MERCHANT_AMAZON)
                    .update();

            // act
            Optional<Long> result = transactionRepository.findWeightedDefaultCategoryId(
                    MERCHANT_AMAZON, USER_ID, HALF_LIFE_SECONDS, 2);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(CATEGORY_DINING_OUT, result.get(),
                    "recent transactions should outweigh older, more numerous ones");
        }

        @Test
        @DisplayName("should return empty when no category meets the minimum occurrence threshold")
        void shouldReturnEmptyBelowOccurrenceThreshold() {
            // arrange -- merchant 4 (My Favorite Cafe) has exactly 1 baseline transaction (id 1000)
            Optional<Long> result = transactionRepository.findWeightedDefaultCategoryId(
                    4L, USER_ID, HALF_LIFE_SECONDS, 2);

            // assert & verify
            assertTrue(result.isEmpty(), "a single occurrence must not meet a minOccurrences=2 floor");
        }

        @Test
        @DisplayName("should ignore transactions with no category assigned")
        void shouldExcludeUncategorizedTransactions() {
            // arrange -- every seeded transaction for this merchant is uncategorized
            jdbcClient.sql("""
                    insert into transactions (account_id, category_id, merchant_id, amount, date, description, type)
                    select 1, null, :merchant, 10.00, now() - interval '1 day', 'Uncategorized ' || s.n, 'EXPENSE'
                    from generate_series(1, 3) as s(n)
                    """)
                    .param("merchant", MERCHANT_AMAZON)
                    .update();

            // act
            Optional<Long> result = transactionRepository.findWeightedDefaultCategoryId(
                    MERCHANT_AMAZON, USER_ID, HALF_LIFE_SECONDS, 2);

            // assert & verify
            assertTrue(result.isEmpty(), "uncategorized transactions must never produce a default");
        }

        @Test
        @DisplayName("should return empty for a merchant with no transactions at all")
        void shouldReturnEmptyForMerchantWithNoTransactions() {
            // act -- merchant 2 (Amazon) has zero transactions in the baseline fixture
            Optional<Long> result = transactionRepository.findWeightedDefaultCategoryId(
                    MERCHANT_AMAZON, USER_ID, HALF_LIFE_SECONDS, 2);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    /** {@code findDescriptions} lists the user's distinct transaction descriptions (grouped by their light-normalized form, the same key description links match on) with how many live transactions use each and which merchant, if any, each is linked to. */
    @Nested
    @DisplayName("findDescriptions")
    class FindDescriptionsTests {

        private static final String SEARCH = "zzq";
        private static final String COSTCO = "ZZQ Costco";
        private static final String UNLINKED = "ZZQ Unlinked";
        private static final String LINKED = "ZZQ Linked";
        private static final String LINKED_KEY = "zzq linked";
        private static final Long USER_2 = 2L;
        private static final Long MERCHANT_AMAZON = 2L;

        private void insertTransaction(long accountId, String description) {
            jdbcClient.sql("""
                    insert into transactions (account_id, amount, date, description, type)
                    values (:accountId, 10.00, '2030-01-01T00:00:00Z', :description, 'EXPENSE')
                    """)
                    .param("accountId", accountId)
                    .param("description", description)
                    .update();
        }

        private void insertLink(long userId, long merchantId, String description, String normalized) {
            jdbcClient.sql("""
                    insert into merchant_description_links (user_id, merchant_id, description, normalized_description)
                    values (:userId, :merchantId, :description, :normalized)
                    """)
                    .param("userId", userId)
                    .param("merchantId", merchantId)
                    .param("description", description)
                    .param("normalized", normalized)
                    .update();
        }

        @Test
        @DisplayName("should group descriptions that differ only in case or spacing and sum their counts")
        void shouldGroupByNormalizedDescription() {
            // arrange
            insertTransaction(1L, "ZZQ Chick-fil-A #1");
            insertTransaction(1L, "ZZQ Chick-fil-A #1");
            insertTransaction(2L, "ZZQ CHICK-FIL-A  #1");

            // act
            Page<TransactionDescriptionDto> result = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, false, PageRequest.of(0, 10));

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals(3L, result.getContent().get(0).transactionCount());
            assertEquals("ZZQ Chick-fil-A #1", result.getContent().get(0).description());
        }

        @Test
        @DisplayName("should fold leading and trailing tabs and line breaks the same way the link key does")
        void shouldFoldSurroundingWhitespaceLikeTheLinkKey() {
            // arrange -- "ZZQ Trail\r" and "ZZQ Trail" are one description to MerchantService.lightNormalize
            insertTransaction(1L, "ZZQ Trail\r");
            insertTransaction(1L, "ZZQ Trail");
            insertLink(USER_ID, MERCHANT_AMAZON, "ZZQ Trail", "zzq trail");

            // act
            List<TransactionDescriptionDto> result = transactionRepository.findDescriptions(
                    USER_ID, "zzq trail", false, PageRequest.of(0, 10)).getContent();

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).transactionCount());
            assertEquals(MERCHANT_AMAZON, result.get(0).merchantId());
        }

        @Test
        @DisplayName("should not count soft-deleted transactions")
        void shouldIgnoreSoftDeleted() {
            // arrange
            insertTransaction(1L, "ZZQ Gone");
            jdbcClient.sql("update transactions set deleted_at = now() where description = 'ZZQ Gone'").update();

            // act
            Page<TransactionDescriptionDto> result = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, false, PageRequest.of(0, 10));

            // assert & verify
            assertEquals(0, result.getTotalElements());
        }

        @Test
        @DisplayName("should not include another user's transactions or links")
        void shouldBeOwnerScoped() {
            // arrange
            insertTransaction(1L, COSTCO);
            insertTransaction(4L, COSTCO);
            insertTransaction(4L, COSTCO);
            jdbcClient.sql("insert into merchants (id, user_id, name) values (900, :userId, 'Other Costco')")
                    .param("userId", USER_2)
                    .update();
            insertLink(USER_2, 900L, COSTCO, "zzq costco");

            // act
            Page<TransactionDescriptionDto> result = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, false, PageRequest.of(0, 10));

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals(1L, result.getContent().get(0).transactionCount());
            assertNull(result.getContent().get(0).merchantId());
        }

        @Test
        @DisplayName("should return the linked merchant id and name, or null when unlinked")
        void shouldReturnLinkedMerchant() {
            // arrange
            insertTransaction(1L, LINKED);
            insertTransaction(1L, UNLINKED);
            insertLink(USER_ID, MERCHANT_AMAZON, LINKED_KEY, LINKED_KEY);

            // act
            List<TransactionDescriptionDto> result = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, false, PageRequest.of(0, 10)).getContent();

            // assert & verify
            TransactionDescriptionDto linked = result.stream().filter(d -> LINKED.equals(d.description())).findFirst().orElseThrow();
            TransactionDescriptionDto unlinked = result.stream().filter(d -> UNLINKED.equals(d.description())).findFirst().orElseThrow();
            assertEquals(MERCHANT_AMAZON, linked.merchantId());
            assertEquals("Amazon", linked.merchantName());
            assertNull(unlinked.merchantId());
            assertNull(unlinked.merchantName());
        }

        @Test
        @DisplayName("should filter by case-insensitive search and treat LIKE wildcards literally")
        void shouldSearchAndEscapeWildcards() {
            // arrange
            insertTransaction(1L, "ZZQ Fifty% Off");
            insertTransaction(1L, "ZZQ Fiftyx Off");

            // act
            Page<TransactionDescriptionDto> literal = transactionRepository.findDescriptions(
                    USER_ID, "FIFTY%", false, PageRequest.of(0, 10));
            Page<TransactionDescriptionDto> wildcard = transactionRepository.findDescriptions(
                    USER_ID, "%", false, PageRequest.of(0, 10));

            // assert & verify
            assertEquals(1, literal.getTotalElements());
            assertEquals("ZZQ Fifty% Off", literal.getContent().get(0).description());
            assertEquals(List.of("ZZQ Fifty% Off"), wildcard.getContent().stream()
                    .map(TransactionDescriptionDto::description).filter(d -> d.startsWith("ZZQ")).toList());
        }

        @Test
        @DisplayName("should return only unlinked rows when unlinkedOnly, with totals reflecting the filter")
        void shouldFilterUnlinkedOnly() {
            // arrange
            insertTransaction(1L, LINKED);
            insertTransaction(1L, UNLINKED);
            insertLink(USER_ID, MERCHANT_AMAZON, LINKED_KEY, LINKED_KEY);

            // act
            Page<TransactionDescriptionDto> result = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, true, PageRequest.of(0, 10));

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals(UNLINKED, result.getContent().get(0).description());
        }

        @Test
        @DisplayName("should order by transaction count descending then description, and page")
        void shouldOrderAndPage() {
            // arrange
            insertTransaction(1L, "ZZQ B");
            insertTransaction(1L, "ZZQ A");
            insertTransaction(1L, "ZZQ C");
            insertTransaction(1L, "ZZQ C");

            // act
            Page<TransactionDescriptionDto> first = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, false, PageRequest.of(0, 2));
            Page<TransactionDescriptionDto> second = transactionRepository.findDescriptions(
                    USER_ID, SEARCH, false, PageRequest.of(1, 2));

            // assert & verify
            assertEquals(3, first.getTotalElements());
            assertEquals(List.of("ZZQ C", "ZZQ A"), first.getContent().stream().map(TransactionDescriptionDto::description).toList());
            assertEquals(List.of("ZZQ B"), second.getContent().stream().map(TransactionDescriptionDto::description).toList());
        }
    }
}
