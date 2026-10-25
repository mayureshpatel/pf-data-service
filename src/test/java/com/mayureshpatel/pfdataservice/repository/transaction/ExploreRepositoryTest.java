package com.mayureshpatel.pfdataservice.repository.transaction;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTransactionDto;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository.ExploreRowData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@code ExploreRepository} against a real PostgreSQL instance. Every test inserts its own
 * transactions in 2031, far from the baseline fixture's dates, so the expected totals are exact.
 */
@Import(ExploreRepository.class)
@DisplayName("ExploreRepository Integration Tests (PostgreSQL)")
class ExploreRepositoryTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final String EXPENSE = "EXPENSE";
    private static final String INCOME = "INCOME";
    private static final long CHECKING = 1L;
    private static final long SAVINGS = 2L;
    private static final long OTHER_USERS_ACCOUNT = 4L;
    private static final long FOOD = 2L;
    private static final long GROCERIES = 7L;
    private static final long DINING_OUT = 8L;
    private static final long HOUSING = 1L;
    private static final long RENT = 6L;
    private static final long INCOME_PARENT = 4L;
    private static final long SALARY = 10L;
    private static final String AMOUNT_100 = "100.00";
    private static final String AMOUNT_30 = "30.00";
    private static final String AMOUNT_20 = "20.00";
    private static final String AMOUNT_1500 = "1500.00";
    private static final String AMOUNT_3000 = "3000.00";
    private static final String AMOUNT_150 = "150.00";
    private static final DateWindow YEAR_2031 = new DateWindow(utc(2031, 1, 1), utc(2032, 1, 1));

    @Autowired
    private ExploreRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private static OffsetDateTime utc(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private long insert(long accountId, Long categoryId, Long merchantId, String type, String amount, OffsetDateTime date) {
        return jdbcClient.sql("""
                        insert into transactions (account_id, category_id, merchant_id, amount, date, description, type)
                        values (:accountId, :categoryId, :merchantId, :amount, :date, 'explore test', :type)
                        returning id
                        """)
                .param("accountId", accountId)
                .param("categoryId", categoryId)
                .param("merchantId", merchantId)
                .param("amount", new BigDecimal(amount))
                .param("date", date)
                .param("type", type)
                .query(Long.class)
                .single();
    }

    private void tag(long transactionId, long tagId) {
        jdbcClient.sql("insert into transaction_tags (transaction_id, tag_id) values (:t, :g)")
                .param("t", transactionId)
                .param("g", tagId)
                .update();
    }

    private ExploreRowData row(List<ExploreRowData> rows, String key) {
        return rows.stream().filter(r -> java.util.Objects.equals(key, r.key())).findFirst().orElseThrow(
                () -> new AssertionError("no row with key " + key + " in " + rows));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    /** Groceries 100 and 50 (Whole Foods, tags Vacation+Work and Vacation), Dining Out 30 (no merchant), an uncategorized 20 (Amazon, savings), Rent 1500, a salary of 3000, a transfer pair, an adjustment, a deleted row and another user's row. */
    @BeforeEach
    void seed() {
        long t1 = insert(CHECKING, GROCERIES, 1L, EXPENSE, AMOUNT_100, utc(2031, 3, 5));
        long t2 = insert(CHECKING, GROCERIES, 1L, EXPENSE, "50.00", utc(2031, 3, 20));
        insert(CHECKING, DINING_OUT, null, EXPENSE, AMOUNT_30, utc(2031, 4, 2));
        insert(SAVINGS, null, 2L, EXPENSE, AMOUNT_20, utc(2031, 4, 10));
        insert(CHECKING, RENT, null, EXPENSE, AMOUNT_1500, utc(2031, 4, 1));
        insert(CHECKING, SALARY, null, INCOME, AMOUNT_3000, utc(2031, 3, 1));
        insert(CHECKING, null, null, "TRANSFER_OUT", "500.00", utc(2031, 3, 10));
        insert(SAVINGS, null, null, "TRANSFER_IN", "500.00", utc(2031, 3, 10));
        insert(CHECKING, null, null, "ADJUSTMENT", "10.00", utc(2031, 3, 11));
        long deleted = insert(CHECKING, GROCERIES, 1L, EXPENSE, "999.00", utc(2031, 3, 12));
        jdbcClient.sql("update transactions set deleted_at = now() where id = :id").param("id", deleted).update();
        insert(OTHER_USERS_ACCOUNT, null, null, EXPENSE, "777.00", utc(2031, 3, 13));
        tag(t1, 1L);
        tag(t1, 2L);
        tag(t2, 1L);
    }

    @Test
    @DisplayName("should total expense and income per category with parents rolled up")
    void shouldRollUpCategoriesToParents() {
        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        assertMoney("180.00", row(rows, String.valueOf(FOOD)).expense());
        assertEquals(3L, row(rows, String.valueOf(FOOD)).expenseCount());
        assertMoney(AMOUNT_1500, row(rows, String.valueOf(HOUSING)).expense());
        assertMoney(AMOUNT_3000, row(rows, String.valueOf(INCOME_PARENT)).income());
        assertEquals("Food", row(rows, String.valueOf(FOOD)).label());
    }

    @Test
    @DisplayName("should return Uncategorized as its own group")
    void shouldReturnUncategorizedGroup() {
        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        ExploreRowData uncategorized = row(rows, null);
        assertEquals("Uncategorized", uncategorized.label());
        assertMoney(AMOUNT_20, uncategorized.expense());
    }

    @Test
    @DisplayName("should list subcategories under a parent filter")
    void shouldListSubcategoriesUnderParentFilter() {
        // arrange
        ExploreFilter food = new ExploreFilter(List.of(), FOOD, false, null, false, null, false, null, null);

        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.SUBCATEGORY, food, YEAR_2031);

        // assert & verify
        assertEquals(2, rows.size());
        assertMoney(AMOUNT_150, row(rows, String.valueOf(GROCERIES)).expense());
        assertMoney(AMOUNT_30, row(rows, String.valueOf(DINING_OUT)).expense());
    }

    @Test
    @DisplayName("should group by merchant with No merchant as its own group")
    void shouldGroupByMerchantWithNoMerchantGroup() {
        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.MERCHANT, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        assertMoney(AMOUNT_150, row(rows, "1").expense());
        assertMoney(AMOUNT_20, row(rows, "2").expense());
        ExploreRowData none = row(rows, null);
        assertEquals("No merchant", none.label());
        assertMoney("1530.00", none.expense());
        assertMoney(AMOUNT_3000, none.income());
    }

    @Test
    @DisplayName("should count a transaction under each of its tags and under No tag when it has none")
    void shouldCountATransactionUnderEachTag() {
        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.TAG, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        assertMoney(AMOUNT_150, row(rows, "1").expense());
        assertEquals(2L, row(rows, "1").expenseCount());
        assertMoney(AMOUNT_100, row(rows, "2").expense());
        assertEquals("No tag", row(rows, null).label());
        assertMoney("1550.00", row(rows, null).expense());
    }

    @Test
    @DisplayName("should group by account")
    void shouldGroupByAccount() {
        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.ACCOUNT, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        assertMoney("1680.00", row(rows, String.valueOf(CHECKING)).expense());
        assertMoney(AMOUNT_20, row(rows, String.valueOf(SAVINGS)).expense());
        assertEquals("Main Checking", row(rows, String.valueOf(CHECKING)).label());
    }

    @Test
    @DisplayName("should bucket by month, week and day of week in UTC")
    void shouldBucketByTime() {
        // act
        List<ExploreRowData> months = repository.findGroups(USER_1, ExploreGroupBy.MONTH, ExploreFilter.none(), YEAR_2031);
        List<ExploreRowData> weeks = repository.findGroups(USER_1, ExploreGroupBy.WEEK, ExploreFilter.none(), YEAR_2031);
        List<ExploreRowData> days = repository.findGroups(USER_1, ExploreGroupBy.DAY_OF_WEEK, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        assertEquals(List.of("2031-03-01", "2031-04-01"), months.stream().map(ExploreRowData::key).toList());
        assertMoney(AMOUNT_150, row(months, "2031-03-01").expense());
        assertMoney("1550.00", row(months, "2031-04-01").expense());
        LocalDate firstMonday = LocalDate.of(2031, 3, 5).with(java.time.DayOfWeek.MONDAY);
        assertMoney(AMOUNT_100, row(weeks, firstMonday.toString()).expense());
        int wednesday = LocalDate.of(2031, 3, 5).getDayOfWeek().getValue();
        // Mar 5 and Apr 2, 2031 are both Wednesdays: 100 + 30
        assertMoney("130.00", row(days, String.valueOf(wednesday)).expense());
    }

    @Test
    @DisplayName("should exclude transfers, adjustments, soft-deleted rows and other users' rows")
    void shouldExcludeWhatDoesNotCount() {
        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY, ExploreFilter.none(), YEAR_2031);

        // assert & verify
        BigDecimal expense = rows.stream().map(ExploreRowData::expense).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal income = rows.stream().map(ExploreRowData::income).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney("1700.00", expense);
        assertMoney(AMOUNT_3000, income);
        assertTrue(repository.findGroups(USER_2, ExploreGroupBy.CATEGORY, ExploreFilter.none(), YEAR_2031).stream()
                .allMatch(r -> r.expense().compareTo(new BigDecimal("777.00")) == 0));
    }

    @Test
    @DisplayName("should apply every filter together")
    void shouldApplyEveryFilterTogether() {
        // act
        List<ExploreRowData> byAccountAndNoMerchant = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY,
                new ExploreFilter(List.of(CHECKING), FOOD, false, null, true, null, false, null, null), YEAR_2031);
        List<ExploreRowData> byTag = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY,
                new ExploreFilter(List.of(), FOOD, false, null, false, 1L, false, null, null), YEAR_2031);
        List<ExploreRowData> byAmount = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY,
                new ExploreFilter(List.of(), null, false, null, false, null, false, new BigDecimal("60"), new BigDecimal("200")), YEAR_2031);
        List<ExploreRowData> byMerchant = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY,
                new ExploreFilter(List.of(), null, false, 2L, false, null, false, null, null), YEAR_2031);
        List<ExploreRowData> byNoTag = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY,
                new ExploreFilter(List.of(), HOUSING, false, null, false, null, true, null, null), YEAR_2031);
        List<ExploreRowData> byUncategorized = repository.findGroups(USER_1, ExploreGroupBy.MERCHANT,
                new ExploreFilter(List.of(), null, true, null, false, null, false, null, null), YEAR_2031);

        // assert & verify
        assertMoney(AMOUNT_30, row(byAccountAndNoMerchant, String.valueOf(FOOD)).expense());
        assertMoney(AMOUNT_150, row(byTag, String.valueOf(FOOD)).expense());
        assertEquals(2L, row(byTag, String.valueOf(FOOD)).expenseCount());
        assertEquals(1, byAmount.size());
        assertMoney(AMOUNT_100, byAmount.get(0).expense());
        assertMoney(AMOUNT_20, row(byMerchant, null).expense());
        assertMoney(AMOUNT_1500, row(byNoTag, String.valueOf(HOUSING)).expense());
        assertEquals(1, byUncategorized.size());
    }

    @Test
    @DisplayName("should respect the half-open window at the boundaries")
    void shouldRespectHalfOpenWindow() {
        // arrange -- t1 sits exactly at the start (included), t2 exactly at the end (excluded)
        DateWindow window = new DateWindow(utc(2031, 3, 5), utc(2031, 3, 20));

        // act
        List<ExploreRowData> rows = repository.findGroups(USER_1, ExploreGroupBy.CATEGORY, ExploreFilter.none(), window);

        // assert & verify
        assertEquals(1, rows.size());
        assertMoney(AMOUNT_100, rows.get(0).expense());
    }

    @Test
    @DisplayName("should count the excluded transfers and adjustments")
    void shouldCountExcluded() {
        // act & assert & verify
        assertEquals(3L, repository.countExcluded(USER_1, ExploreFilter.none(), YEAR_2031));
        assertEquals(2L, repository.countExcluded(USER_1,
                new ExploreFilter(List.of(CHECKING), null, false, null, false, null, false, null, null), YEAR_2031));
        assertEquals(0L, repository.countExcluded(USER_2, ExploreFilter.none(), YEAR_2031));
    }

    @Test
    @DisplayName("should list exactly the transactions behind a group's number, newest first")
    void shouldListTheTransactionsBehindANumber() {
        // arrange
        ExploreFilter groceries = new ExploreFilter(List.of(), GROCERIES, false, null, false, null, false, null, null);

        // act
        org.springframework.data.domain.Page<ExploreTransactionDto> page = repository.findTransactions(
                USER_1, groceries, YEAR_2031, List.of(EXPENSE), org.springframework.data.domain.PageRequest.of(0, 20));
        List<ExploreRowData> group = repository.findGroups(USER_1, ExploreGroupBy.SUBCATEGORY, groceries, YEAR_2031);

        // assert & verify
        assertEquals(2, page.getTotalElements());
        assertEquals(List.of(new BigDecimal("50.00"), new BigDecimal("100.00")),
                page.getContent().stream().map(t -> t.amount().setScale(2)).toList());
        BigDecimal listed = page.getContent().stream().map(ExploreTransactionDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, listed.compareTo(row(group, String.valueOf(GROCERIES)).expense()));
        ExploreTransactionDto newest = page.getContent().get(0);
        assertEquals("Whole Foods", newest.merchantName());
        assertEquals("Main Checking", newest.accountName());
        assertEquals("Groceries", newest.categoryName());
        assertEquals(LocalDate.of(2031, 3, 20), newest.date());
    }

    @Test
    @DisplayName("should page the transaction list and exclude transfers, adjustments, deleted and other users' rows")
    void shouldPageAndExcludeInTheTransactionList() {
        // act
        org.springframework.data.domain.Page<ExploreTransactionDto> first = repository.findTransactions(
                USER_1, ExploreFilter.none(), YEAR_2031, List.of(EXPENSE, INCOME), org.springframework.data.domain.PageRequest.of(0, 3));
        org.springframework.data.domain.Page<ExploreTransactionDto> second = repository.findTransactions(
                USER_1, ExploreFilter.none(), YEAR_2031, List.of(EXPENSE, INCOME), org.springframework.data.domain.PageRequest.of(1, 3));

        // assert & verify
        assertEquals(6, first.getTotalElements());
        assertEquals(3, first.getContent().size());
        assertEquals(3, second.getContent().size());
        assertTrue(first.getContent().get(0).date().isAfter(second.getContent().get(2).date().minusDays(1)));
        assertTrue(java.util.stream.Stream.concat(first.getContent().stream(), second.getContent().stream())
                .noneMatch(t -> t.type() == com.mayureshpatel.pfdataservice.domain.transaction.TransactionType.TRANSFER_IN
                        || t.type() == com.mayureshpatel.pfdataservice.domain.transaction.TransactionType.ADJUSTMENT
                        || t.amount().compareTo(new BigDecimal("999.00")) == 0
                        || t.amount().compareTo(new BigDecimal("777.00")) == 0));
    }
}
