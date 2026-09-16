package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(MerchantRepository.class)
@DisplayName("MerchantRepository Integration Tests (PostgreSQL)")
class MerchantRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MerchantRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Long MERCHANT_WHOLEFOODS = 1L; // Global

    /**
     * PF-320: incidental test setup (e.g. "grab USER_1's first merchant to update/delete it")
     * still needs the full, unpaginated list -- {@link Pageable#unpaged()} is the real Spring
     * Data mechanism for that, not a parallel test-only convenience method.
     */
    private Merchant firstMerchantFor(Long userId) {
        return repository.findAllByUserId(userId, null, Pageable.unpaged()).getContent().get(0);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find global merchant by ID")
        void shouldFindById() {
            // act
            Optional<Merchant> result = repository.findById(MERCHANT_WHOLEFOODS);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("Whole Foods", result.get().getCleanName());
            assertNull(result.get().getUserId());
        }

        @Test
        @DisplayName("should find all merchants for user (including null/global ones if supported by query logic)")
        void shouldFindAllByUserId() {
            // Note: The query only filters by user_id = :userId
            // act
            List<Merchant> result = repository.findAllByUserId(USER_1);

            // assert & verify
            // Baseline has 1 custom merchant for USER_1
            assertEquals(1, result.size());
            assertEquals("LOCAL CAFE", result.get(0).getOriginalName());
        }

        @Test
        @DisplayName("PF-320: should return a Page honoring the requested page size and total count")
        void shouldFindAllByUserIdPaged() {
            // arrange
            Pageable pageable = PageRequest.of(0, 1);

            // act
            Page<Merchant> result = repository.findAllByUserId(USER_1, null, pageable);

            // assert & verify
            assertEquals(1, result.getContent().size());
            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getTotalPages());
        }

        @Test
        @DisplayName("PF-320: should filter by a case-insensitive search term matched against either name column")
        void shouldFindAllByUserIdWithSearch() {
            // arrange -- USER_1's baseline merchant is "My Favorite Cafe" / "LOCAL CAFE"
            Pageable pageable = PageRequest.of(0, 20, Sort.by("cleanName"));

            // act
            Page<Merchant> matches = repository.findAllByUserId(USER_1, "favorite", pageable);
            Page<Merchant> noMatches = repository.findAllByUserId(USER_1, "nonexistent-merchant-xyz", pageable);

            // assert & verify
            assertEquals(1, matches.getTotalElements());
            assertEquals("My Favorite Cafe", matches.getContent().get(0).getCleanName());
            assertTrue(noMatches.getContent().isEmpty());
            assertEquals(0, noMatches.getTotalElements());
        }

        @Test
        @DisplayName("should find merchants by exact clean name")
        void shouldFindByCleanName() {
            // act
            List<Merchant> result = repository.findAllByCleanName("Whole Foods");

            // assert & verify
            assertFalse(result.isEmpty());
            assertEquals(MERCHANT_WHOLEFOODS, result.get(0).getId());
        }

        @Test
        @DisplayName("should find a user-scoped merchant by clean name")
        void shouldFindAllByCleanNameAndUserId() {
            // arrange & act
            List<Merchant> result = repository.findAllByCleanNameAndUserId("My Favorite Cafe", USER_1);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("LOCAL CAFE", result.get(0).getOriginalName());
        }

        @Test
        @DisplayName("should not find a global merchant via findAllByCleanNameAndUserId (user_id column doesn't match NULL)")
        void shouldNotFindGlobalMerchantByCleanNameAndUserId() {
            // arrange & act -- "Whole Foods" is a global (user_id NULL) baseline merchant
            List<Merchant> result = repository.findAllByCleanNameAndUserId("Whole Foods", USER_1);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should find merchants matching any of a list of clean names, scoped to user")
        void shouldFindAllByCleanNamesAndUserId() {
            // arrange & act
            List<Merchant> result = repository.findAllByCleanNamesAndUserId(
                    List.of("My Favorite Cafe", "Some Clean Name That Does Not Exist"), USER_1);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("My Favorite Cafe", result.get(0).getCleanName());
        }

        @Test
        @DisplayName("should return empty list for an empty clean names list")
        void shouldReturnEmptyForEmptyCleanNamesList() {
            // arrange & act
            List<Merchant> result = repository.findAllByCleanNamesAndUserId(List.of(), USER_1);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-840: should find a merchant by its light-normalized original name")
        void shouldFindAllByNormalizedOriginalNameAndUserId() {
            // arrange & act -- USER_1's baseline merchant has original_name "LOCAL CAFE"
            List<Merchant> result = repository.findAllByNormalizedOriginalNameAndUserId("local cafe", USER_1);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("LOCAL CAFE", result.get(0).getOriginalName());
        }

        @Test
        @DisplayName("PF-840: should match regardless of case or whitespace differences, via the same "
                + "case-fold + whitespace-collapse the parameter is expected to already carry")
        void shouldFindAllByNormalizedOriginalNameAndUserIdWithWhitespaceVariance() {
            // arrange -- insert a row whose raw original_name has extra/irregular whitespace
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(USER_1)
                    .originalName("COFFEE  HOUSE   777")
                    .cleanName("")
                    .build();
            repository.insert(request);

            // act
            List<Merchant> result = repository.findAllByNormalizedOriginalNameAndUserId("coffee house 777", USER_1);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("COFFEE  HOUSE   777", result.get(0).getOriginalName());
        }

        @Test
        @DisplayName("PF-840: should not match a genuinely different original name")
        void shouldNotFindAllByNormalizedOriginalNameAndUserIdForDifferentText() {
            // arrange & act
            List<Merchant> result = repository.findAllByNormalizedOriginalNameAndUserId("some other merchant entirely", USER_1);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-840: should not find another user's merchant via the normalized-original-name lookup")
        void shouldNotFindAnotherUsersMerchantByNormalizedOriginalName() {
            // arrange & act -- USER_1's baseline merchant, looked up as USER_2
            List<Merchant> result = repository.findAllByNormalizedOriginalNameAndUserId("local cafe", USER_2);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-840: should find merchants matching any of a list of normalized original names, scoped to user")
        void shouldFindAllByNormalizedOriginalNamesAndUserId() {
            // arrange & act
            List<Merchant> result = repository.findAllByNormalizedOriginalNamesAndUserId(
                    List.of("local cafe", "some clean name that does not exist"), USER_1);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("LOCAL CAFE", result.get(0).getOriginalName());
        }

        @Test
        @DisplayName("PF-840: should return empty list for an empty normalized original names list")
        void shouldReturnEmptyForEmptyNormalizedOriginalNamesList() {
            // arrange & act
            List<Merchant> result = repository.findAllByNormalizedOriginalNamesAndUserId(List.of(), USER_1);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-220: should find a merchant by id when the requesting user owns it")
        void shouldFindByIdAndUserId() {
            // arrange
            Long ownedMerchantId = firstMerchantFor(USER_1).getId();

            // act
            Optional<Merchant> result = repository.findByIdAndUserId(ownedMerchantId, USER_1);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("My Favorite Cafe", result.get().getCleanName());
        }

        @Test
        @DisplayName("PF-220: should not find another user's merchant via findByIdAndUserId")
        void shouldNotFindAnotherUsersMerchantByIdAndUserId() {
            // arrange -- USER_1's own merchant, looked up as USER_2
            Long user1MerchantId = firstMerchantFor(USER_1).getId();

            // act
            Optional<Merchant> result = repository.findByIdAndUserId(user1MerchantId, USER_2);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-220: should not find a global merchant via findByIdAndUserId (user_id column doesn't match NULL)")
        void shouldNotFindGlobalMerchantByIdAndUserId() {
            // arrange & act
            Optional<Merchant> result = repository.findByIdAndUserId(MERCHANT_WHOLEFOODS, USER_1);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Aggregations")
    class AggregationTests {
        @Test
        @DisplayName("should calculate merchant totals from baseline transactions")
        void shouldFindMerchantTotals() {
            // arrange
            // From baseline: USER_1 has Grocery Run transactions at Whole Foods (ID 1)
            OffsetDateTime start = LocalDate.of(2025, 9, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = LocalDate.of(2026, 3, 31).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            // act
            List<MerchantBreakdownDto> result = repository.findMerchantTotals(USER_1, start, end);

            // assert & verify
            assertFalse(result.isEmpty());
            MerchantBreakdownDto breakdown = result.stream()
                    .filter(b -> b.displayName().equals("Whole Foods"))
                    .findFirst()
                    .orElseThrow();

            assertTrue(breakdown.total().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("PF-841: two merchant rows sharing one clean name aggregate into a single "
                + "grouped row, summing both totals -- not two separate, identically-labeled rows")
        void shouldGroupMerchantTotalsByDisplayNameNotRowId() {
            // arrange -- two distinct merchant rows (different original_name), same clean_name,
            // each with their own transaction, in a date range isolated from every other test's data
            Long groupedA = repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).originalName("PF-841 GROUPED STORE A").cleanName("PF-841 Grouped Merchant").build());
            Long groupedB = repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).originalName("PF-841 GROUPED STORE B").cleanName("PF-841 Grouped Merchant").build());
            jdbcClient.sql("""
                    insert into transactions (account_id, merchant_id, amount, date, description, type)
                    values (1, :groupedA, 10.00, '2033-01-01T00:00:00Z', 'grouped a', 'EXPENSE'),
                           (1, :groupedB, 15.00, '2033-01-02T00:00:00Z', 'grouped b', 'EXPENSE')
                    """).param("groupedA", groupedA).param("groupedB", groupedB).update();

            OffsetDateTime start = OffsetDateTime.parse("2033-01-01T00:00:00Z");
            OffsetDateTime end = OffsetDateTime.parse("2033-01-03T00:00:00Z");

            // act
            List<MerchantBreakdownDto> result = repository.findMerchantTotals(USER_1, start, end);

            // assert & verify -- one row for the shared clean name, total = both transactions summed
            List<MerchantBreakdownDto> grouped = result.stream()
                    .filter(b -> b.displayName().equals("PF-841 Grouped Merchant"))
                    .toList();
            assertEquals(1, grouped.size(), "expected exactly one aggregated row, got: " + grouped);
            assertEquals(0, new BigDecimal("25.00").compareTo(grouped.get(0).total()));
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new user-specific merchant")
        void shouldInsert() {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(USER_1)
                    .originalName("NEW SHOP 999")
                    .cleanName("New Shop")
                    .build();

            // act
            Long id = repository.insert(request);

            // assert & verify
            assertNotNull(id);
            assertTrue(id > 0);
            List<Merchant> all = repository.findAllByUserId(USER_1);
            assertTrue(all.stream().anyMatch(m -> m.getCleanName().equals("New Shop")));
        }

        @Test
        @DisplayName("should update an existing merchant's clean name")
        void shouldUpdate() {
            // arrange
            Merchant custom = firstMerchantFor(USER_1);
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(custom.getId())
                    .cleanName("Updated Cafe")
                    .build();

            // act
            int rows = repository.update(request, USER_1);

            // assert & verify
            assertEquals(1, rows);
            Merchant updated = repository.findById(custom.getId()).orElseThrow();
            assertEquals("Updated Cafe", updated.getCleanName());
            assertEquals("LOCAL CAFE", updated.getOriginalName()); // Should remain unchanged
        }

        @Test
        @DisplayName("PF-220: should affect 0 rows, and not modify the record, when called with a userId that "
                + "doesn't own the merchant -- this is the actual fix for the IDOR (the SQL previously accepted "
                + "a userId parameter but never used it in the WHERE clause)")
        void shouldNotUpdateAnotherUsersMerchant() {
            // arrange
            Merchant custom = firstMerchantFor(USER_1);
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(custom.getId())
                    .cleanName("Malicious Rename")
                    .build();

            // act
            int rows = repository.update(request, USER_2);

            // assert & verify
            assertEquals(0, rows);
            Merchant unchanged = repository.findById(custom.getId()).orElseThrow();
            assertEquals("My Favorite Cafe", unchanged.getCleanName());
        }

        @Test
        @DisplayName("should hard delete a merchant")
        void shouldDelete() {
            // arrange
            Merchant custom = firstMerchantFor(USER_1);

            // act
            int rows = repository.delete(custom.getId(), USER_1);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findById(custom.getId()).isEmpty());
        }

        @Test
        @DisplayName("PF-222: should affect 0 rows, and not delete the record, when called with a userId "
                + "that doesn't own the merchant -- this method had no caller anywhere until merchant merge "
                + "became its first, so it's scoped by user_id from the start rather than exposing the same "
                + "class of gap PF-220 had to fix on update() after the fact")
        void shouldNotDeleteAnotherUsersMerchant() {
            // arrange
            Merchant custom = firstMerchantFor(USER_1);

            // act
            int rows = repository.delete(custom.getId(), USER_2);

            // assert & verify
            assertEquals(0, rows);
            assertTrue(repository.findById(custom.getId()).isPresent());
        }

        @Test
        @DisplayName("should insert multiple merchants in one call and return them with generated IDs")
        void shouldInsertAllAndReturn() {
            // arrange
            List<MerchantCreateRequest> requests = List.of(
                    MerchantCreateRequest.builder().userId(USER_1).originalName("NEW SHOP A").cleanName("Shop A").build(),
                    MerchantCreateRequest.builder().userId(USER_1).originalName("NEW SHOP B").cleanName("Shop B").build()
            );

            // act
            List<Merchant> result = repository.insertAllAndReturn(requests);

            // assert & verify
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(m -> m.getId() != null && m.getId() > 0));
            assertTrue(result.stream().anyMatch(m -> m.getOriginalName().equals("NEW SHOP A") && m.getCleanName().equals("Shop A")));
            assertTrue(result.stream().anyMatch(m -> m.getOriginalName().equals("NEW SHOP B") && m.getCleanName().equals("Shop B")));
        }

        @Test
        @DisplayName("should return empty list when inserting an empty request list")
        void shouldHandleEmptyInsertAllAndReturn() {
            // act
            List<Merchant> result = repository.insertAllAndReturn(List.of());

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("PF-823: Reports server-side aggregation (Merchants)")
    class ReportDataAggregation {

        @Test
        @DisplayName("findMerchantReportData sums every matching transaction, not just the newest 1000")
        void shouldAggregateMerchantTotalsPastThousandRows() {
            // arrange -- 1,500 EXPENSE transactions for user 1, $10 each, one per day starting
            // 2020-01-01 -- account 1, category 7 (Groceries), merchant 1 (Whole Foods). A single
            // set-based INSERT rather than 1,500 round trips.
            jdbcClient.sql("""
                    insert into transactions (account_id, category_id, merchant_id, amount, date, description, type)
                    select 1, 7, 1, 10.00, (date '2020-01-01' + s.n)::timestamptz, 'Bulk Test Txn ' || s.n, 'EXPENSE'
                    from generate_series(0, 1499) as s(n)
                    """).update();

            // act -- range covers all 1,500 seeded days plus margin
            List<MerchantReportDataDto> result = repository.findMerchantReportData(
                    USER_1,
                    OffsetDateTime.parse("2020-01-01T00:00:00Z"),
                    OffsetDateTime.parse("2024-12-31T00:00:00Z"));

            // assert & verify
            MerchantReportDataDto wholeFoods = result.stream()
                    .filter(r -> r.displayName().equals("Whole Foods"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("expected a Whole Foods entry, got: " + result));
            assertEquals(1500L, wholeFoods.count(),
                    "a 1000-row cap would silently drop 500 of these -- got count=" + wholeFoods.count());
            assertEquals(0, new BigDecimal("15000.00").compareTo(wholeFoods.total()),
                    "expected 1500 * $10.00, got " + wholeFoods.total());
            assertTrue(wholeFoods.categories().contains("Groceries"),
                    "expected the associated category name to be aggregated, got: " + wholeFoods.categories());
        }

        @Test
        @DisplayName("findMerchantReportData reports an empty categories list, not a null placeholder, "
                + "when every transaction for a merchant is uncategorized")
        void shouldReportEmptyCategoriesListWhenAllUncategorized() {
            // arrange -- merchant 2 (Amazon) with a single uncategorized expense on an otherwise
            // empty future date, isolated from baseline data
            jdbcClient.sql("""
                    insert into transactions (account_id, merchant_id, amount, date, description, type)
                    values (1, 2, 42.00, '2031-06-01T00:00:00Z', 'Uncategorized Amazon Order', 'EXPENSE')
                    """).update();

            // act
            List<MerchantReportDataDto> result = repository.findMerchantReportData(
                    USER_1,
                    OffsetDateTime.parse("2031-06-01T00:00:00Z"),
                    OffsetDateTime.parse("2031-06-02T00:00:00Z"));

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(List.of(), result.get(0).categories(),
                    "array_remove must strip the null placeholder array_agg would otherwise contribute");
        }

        @Test
        @DisplayName("PF-841: merchants sharing one clean name aggregate into a single grouped row "
                + "(summed total/count), while distinct blank-clean-name merchants stay separate -- "
                + "grouping by clean_name alone would wrongly merge the blank ones together")
        void shouldGroupSharedCleanNameButKeepDistinctBlankRowsSeparate() {
            // arrange -- two rows sharing one clean name, plus two distinct rows with blank
            // clean_name (the normal state for a freshly-imported, not-yet-reviewed merchant per
            // PF-840) -- everything isolated to its own date range and merchant identities
            Long groupedA = repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).originalName("PF-841 REPORT GROUPED A").cleanName("PF-841 Report Grouped").build());
            Long groupedB = repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).originalName("PF-841 REPORT GROUPED B").cleanName("PF-841 Report Grouped").build());
            Long blankA = repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).originalName("PF-841 REPORT BLANK A").cleanName("").build());
            Long blankB = repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).originalName("PF-841 REPORT BLANK B").cleanName("").build());

            jdbcClient.sql("""
                    insert into transactions (account_id, merchant_id, amount, date, description, type)
                    values (1, :groupedA, 20.00, '2034-01-01T00:00:00Z', 'grouped a', 'EXPENSE'),
                           (1, :groupedB, 30.00, '2034-01-02T00:00:00Z', 'grouped b', 'EXPENSE'),
                           (1, :blankA,   5.00,  '2034-01-01T00:00:00Z', 'blank a', 'EXPENSE'),
                           (1, :blankB,   7.00,  '2034-01-01T00:00:00Z', 'blank b', 'EXPENSE')
                    """)
                    .param("groupedA", groupedA).param("groupedB", groupedB)
                    .param("blankA", blankA).param("blankB", blankB)
                    .update();

            // act
            List<MerchantReportDataDto> result = repository.findMerchantReportData(
                    USER_1,
                    OffsetDateTime.parse("2034-01-01T00:00:00Z"),
                    OffsetDateTime.parse("2034-01-03T00:00:00Z"));

            // assert & verify -- one aggregated row for the shared clean name...
            List<MerchantReportDataDto> grouped = result.stream()
                    .filter(r -> r.displayName().equals("PF-841 Report Grouped"))
                    .toList();
            assertEquals(1, grouped.size(), "expected exactly one aggregated row, got: " + grouped);
            assertEquals(0, new BigDecimal("50.00").compareTo(grouped.get(0).total()));
            assertEquals(2L, grouped.get(0).count());

            // ...but the two blank-clean-name merchants must NOT be merged into each other --
            // each keeps its own original_name as its fallback display name, in its own row.
            List<MerchantReportDataDto> blanks = result.stream()
                    .filter(r -> r.displayName().equals("PF-841 REPORT BLANK A") || r.displayName().equals("PF-841 REPORT BLANK B"))
                    .toList();
            assertEquals(2, blanks.size(), "blank-clean-name merchants must stay in separate rows, got: " + blanks);
        }
    }
}
