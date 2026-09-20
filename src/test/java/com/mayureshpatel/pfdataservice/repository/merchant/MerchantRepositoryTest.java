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
    private static final Long MERCHANT_WHOLE_FOODS = 1L;
    private static final Long MERCHANT_AMAZON = 2L;
    private static final Long MERCHANT_CAFE = 4L; // "My Favorite Cafe", baseline's own merchant

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find a merchant by ID")
        void shouldFindById() {
            // act
            Optional<Merchant> result = repository.findById(MERCHANT_WHOLE_FOODS);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("Whole Foods", result.get().getName());
            assertEquals(USER_1, result.get().getUserId());
        }

        @Test
        @DisplayName("should find all merchants for a user")
        void shouldFindAllByUserId() {
            // act -- baseline seeds 4 merchants for USER_1 (Whole Foods, Amazon, Shell, My Favorite Cafe)
            List<Merchant> result = repository.findAllByUserId(USER_1);

            // assert & verify
            assertEquals(4, result.size());
            assertTrue(result.stream().anyMatch(m -> m.getName().equals("My Favorite Cafe")));
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
            assertEquals(4, result.getTotalElements());
            assertEquals(4, result.getTotalPages());
        }

        @Test
        @DisplayName("PF-845: should filter by a case-insensitive search term matched against name or city")
        void shouldFindAllByUserIdWithSearchByName() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20);

            // act
            Page<Merchant> matches = repository.findAllByUserId(USER_1, "favorite", pageable);
            Page<Merchant> noMatches = repository.findAllByUserId(USER_1, "nonexistent-merchant-xyz", pageable);

            // assert & verify
            assertEquals(1, matches.getTotalElements());
            assertEquals("My Favorite Cafe", matches.getContent().get(0).getName());
            assertTrue(noMatches.getContent().isEmpty());
            assertEquals(0, noMatches.getTotalElements());
        }

        @Test
        @DisplayName("PF-845: should also match a case-insensitive search term against city")
        void shouldFindAllByUserIdWithSearchByCity() {
            // arrange
            repository.insert(MerchantCreateRequest.builder()
                    .userId(USER_1).name("PF-845 City Search Merchant").city("Roswell").build());

            // act
            Page<Merchant> result = repository.findAllByUserId(USER_1, "roswell", PageRequest.of(0, 20));

            // assert & verify
            assertEquals(1, result.getTotalElements());
            assertEquals("PF-845 City Search Merchant", result.getContent().get(0).getName());
        }

        @Test
        @DisplayName("PF-220: should find a merchant by id when the requesting user owns it")
        void shouldFindByIdAndUserId() {
            // act
            Optional<Merchant> result = repository.findByIdAndUserId(MERCHANT_CAFE, USER_1);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("My Favorite Cafe", result.get().getName());
        }

        @Test
        @DisplayName("PF-220: should not find another user's merchant via findByIdAndUserId")
        void shouldNotFindAnotherUsersMerchantByIdAndUserId() {
            // act
            Optional<Merchant> result = repository.findByIdAndUserId(MERCHANT_CAFE, USER_2);

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

            assertEquals(MERCHANT_WHOLE_FOODS, breakdown.representativeMerchantId());
            assertTrue(breakdown.total().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new user-owned merchant")
        void shouldInsert() {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(USER_1)
                    .name("New Shop")
                    .build();

            // act
            Long id = repository.insert(request);

            // assert & verify
            assertNotNull(id);
            assertTrue(id > 0);
            Merchant inserted = repository.findById(id).orElseThrow();
            assertEquals("New Shop", inserted.getName());
            assertNull(inserted.getCity());
        }

        @Test
        @DisplayName("PF-845: should insert and round-trip every location field")
        void shouldInsertWithLocation() {
            // arrange
            MerchantCreateRequest request = MerchantCreateRequest.builder()
                    .userId(USER_1)
                    .name("Located Shop")
                    .city("Atlanta")
                    .state("GA")
                    .postalCode("30301")
                    .country("USA")
                    .build();

            // act
            Long id = repository.insert(request);

            // assert & verify
            Merchant inserted = repository.findById(id).orElseThrow();
            assertEquals("Atlanta", inserted.getCity());
            assertEquals("GA", inserted.getState());
            assertEquals("30301", inserted.getPostalCode());
            assertEquals("USA", inserted.getCountry());
        }

        @Test
        @DisplayName("should update an existing merchant's name and location")
        void shouldUpdate() {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(MERCHANT_CAFE)
                    .name("Updated Cafe")
                    .city("Atlanta")
                    .build();

            // act
            int rows = repository.update(request, USER_1);

            // assert & verify
            assertEquals(1, rows);
            Merchant updated = repository.findById(MERCHANT_CAFE).orElseThrow();
            assertEquals("Updated Cafe", updated.getName());
            assertEquals("Atlanta", updated.getCity());
        }

        @Test
        @DisplayName("PF-220: should affect 0 rows, and not modify the record, when called with a userId that "
                + "doesn't own the merchant")
        void shouldNotUpdateAnotherUsersMerchant() {
            // arrange
            MerchantUpdateRequest request = MerchantUpdateRequest.builder()
                    .id(MERCHANT_CAFE)
                    .name("Malicious Rename")
                    .build();

            // act
            int rows = repository.update(request, USER_2);

            // assert & verify
            assertEquals(0, rows);
            Merchant unchanged = repository.findById(MERCHANT_CAFE).orElseThrow();
            assertEquals("My Favorite Cafe", unchanged.getName());
        }

        @Test
        @DisplayName("should hard delete a merchant")
        void shouldDelete() {
            // act
            int rows = repository.delete(MERCHANT_CAFE, USER_1);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findById(MERCHANT_CAFE).isEmpty());
        }

        @Test
        @DisplayName("PF-845: should affect 0 rows, and not delete the record, when called with a userId "
                + "that doesn't own the merchant")
        void shouldNotDeleteAnotherUsersMerchant() {
            // act
            int rows = repository.delete(MERCHANT_CAFE, USER_2);

            // assert & verify
            assertEquals(0, rows);
            assertTrue(repository.findById(MERCHANT_CAFE).isPresent());
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
            assertEquals(MERCHANT_AMAZON, result.get(0).representativeMerchantId());
            assertEquals(List.of(), result.get(0).categories(),
                    "array_remove must strip the null placeholder array_agg would otherwise contribute");
        }
    }
}
