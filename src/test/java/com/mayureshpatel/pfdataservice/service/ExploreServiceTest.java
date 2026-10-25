package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreMeasure;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreResultDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreRowDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreSide;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.ExploreRepository.ExploreRowData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies {@code ExploreService}: how grouped income/expense rows become any measure, how a comparison period is joined, and that totals never double count (tags). */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExploreService Unit Tests")
class ExploreServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);
    private static final String AMOUNT_300 = "300.00";
    private static final String AMOUNT_100 = "100.00";
    private static final String AMOUNT_50 = "50.00";
    private static final String DAY_2026_01_01 = "2026-01-01";
    private static final String AMOUNT_80 = "80.00";
    private static final String AMOUNT_10 = "10.00";
    private static final String DAY_2026_03_01 = "2026-03-01";
    private static final ExploreFilter NO_FILTER = ExploreFilter.none();

    @Mock private ExploreRepository repository;

    @InjectMocks private ExploreService service;

    private static ExploreRowData data(String key, String label, String income, String expense, long incomeCount, long expenseCount) {
        return new ExploreRowData(key, label, new BigDecimal(income), new BigDecimal(expense), incomeCount, expenseCount);
    }

    private ExploreResultDto run(ExploreMeasure measure, ExploreSide side, ExploreGroupBy groupBy, ExploreCompare compare) {
        return service.explore(USER_ID, measure, side, groupBy, JAN_1, JAN_31, compare, NO_FILTER, TODAY);
    }

    private ExploreRowDto only(ExploreResultDto result) {
        assertEquals(1, result.rows().size());
        return result.rows().get(0);
    }

    private void assertValue(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    /** A category with 300 income over 3 transactions and 100 expense over 2. */
    private void stubOneCategory() {
        when(repository.findGroups(eq(USER_ID), any(), any(), any()))
                .thenReturn(List.of(data("5", "Food", AMOUNT_300, AMOUNT_100, 3, 2)));
    }

    @Test
    @DisplayName("should compute spending, income, count and average from income, expense and counts")
    void shouldComputeEachMeasure() {
        // arrange
        stubOneCategory();

        // act & assert & verify
        assertValue(AMOUNT_100, only(run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE)).value());
        assertValue(AMOUNT_300, only(run(ExploreMeasure.INCOME, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE)).value());
        assertValue("2", only(run(ExploreMeasure.COUNT, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE)).value());
        assertValue("3", only(run(ExploreMeasure.COUNT, ExploreSide.INCOME, ExploreGroupBy.CATEGORY, ExploreCompare.NONE)).value());
        assertValue(AMOUNT_50, only(run(ExploreMeasure.AVERAGE, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE)).value());
        assertValue(AMOUNT_100, only(run(ExploreMeasure.AVERAGE, ExploreSide.INCOME, ExploreGroupBy.CATEGORY, ExploreCompare.NONE)).value());
    }

    @Test
    @DisplayName("should compute net and savings rate over time")
    void shouldComputeNetAndSavingsRateOverTime() {
        // arrange
        when(repository.findGroups(eq(USER_ID), any(), any(), any()))
                .thenReturn(List.of(data(DAY_2026_01_01, DAY_2026_01_01, AMOUNT_300, AMOUNT_100, 3, 2)));

        // act
        ExploreRowDto net = only(run(ExploreMeasure.NET, ExploreSide.SPENDING, ExploreGroupBy.MONTH, ExploreCompare.NONE));
        ExploreRowDto rate = only(run(ExploreMeasure.SAVINGS_RATE, ExploreSide.SPENDING, ExploreGroupBy.MONTH, ExploreCompare.NONE));

        // assert & verify
        assertValue("200.00", net.value());
        assertValue("66.67", rate.value()); // same rounding as the Dashboard: divide to 4 places, then times 100
    }

    @Test
    @DisplayName("should return a null savings rate when income is zero")
    void shouldReturnNullSavingsRateWithoutIncome() {
        // arrange
        when(repository.findGroups(eq(USER_ID), any(), any(), any()))
                .thenReturn(List.of(data(DAY_2026_01_01, DAY_2026_01_01, "0.00", AMOUNT_50, 0, 1)));

        // act
        ExploreResultDto result = run(ExploreMeasure.SAVINGS_RATE, ExploreSide.SPENDING, ExploreGroupBy.MONTH, ExploreCompare.NONE);

        // assert & verify
        assertNull(only(result).value());
        assertNull(result.total());
    }

    @Test
    @DisplayName("should reject Net and Savings rate for a non-time grouping")
    void shouldRejectTimeOnlyMeasuresForOtherGroupings() {
        // act & assert & verify
        assertThrows(IllegalArgumentException.class,
                () -> run(ExploreMeasure.NET, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE));
        assertThrows(IllegalArgumentException.class,
                () -> run(ExploreMeasure.SAVINGS_RATE, ExploreSide.SPENDING, ExploreGroupBy.MERCHANT, ExploreCompare.NONE));
    }

    @Test
    @DisplayName("should join comparison rows by key, including keys only in the comparison")
    void shouldJoinComparisonRowsByKey() {
        // arrange
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.CATEGORY), any(), any())).thenReturn(
                List.of(data("1", "A", "0", AMOUNT_100, 0, 1), data("2", "B", "0", AMOUNT_50, 0, 1)),
                List.of(data("1", "A", "0", AMOUNT_80, 0, 1), data("3", "C", "0", "40.00", 0, 1)));

        // act
        ExploreResultDto result = run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.PREVIOUS_PERIOD);

        // assert & verify
        assertEquals(List.of("A", "B", "C"), result.rows().stream().map(ExploreRowDto::label).toList());
        assertValue(AMOUNT_80, result.rows().get(0).comparisonValue());
        assertValue("0", result.rows().get(1).comparisonValue());
        assertValue("0", result.rows().get(2).value());
        assertValue("40.00", result.rows().get(2).comparisonValue());
        assertValue("150.00", result.total());
        assertValue("120.00", result.comparisonTotal());
    }

    @Test
    @DisplayName("should compare the same elapsed days for a period in progress and say so")
    void shouldCompareSameElapsedDaysForPeriodInProgress() {
        // arrange
        when(repository.findGroups(eq(USER_ID), any(), any(), any())).thenReturn(List.of());

        // act -- the whole of 2026 was requested, but only Jan 1 to Oct 25 has happened
        ExploreResultDto result = service.explore(USER_ID, ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY,
                JAN_1, LocalDate.of(2026, 12, 31), ExploreCompare.SAME_DAYS_LAST_YEAR, NO_FILTER, TODAY);

        // assert & verify
        assertTrue(result.inProgress());
        assertEquals(LocalDate.of(2025, 1, 1), result.comparisonStart());
        assertEquals(LocalDate.of(2025, 10, 25), result.comparisonEnd());
        assertEquals("same days last year (Jan 1 to Oct 25, 2025)", result.comparisonLabel());
        ArgumentCaptor<DateWindow> windows = ArgumentCaptor.forClass(DateWindow.class);
        verify(repository, org.mockito.Mockito.times(2)).findGroups(eq(USER_ID), any(), any(), windows.capture());
        assertEquals(windows.getAllValues().get(0).end().toLocalDate(), LocalDate.of(2026, 10, 26));
    }

    @Test
    @DisplayName("should omit rows with no activity on the chosen side")
    void shouldOmitRowsWithoutActivityOnTheChosenSide() {
        // arrange -- the second row is income only
        when(repository.findGroups(eq(USER_ID), any(), any(), any())).thenReturn(
                List.of(data("1", "A", "0", AMOUNT_10, 0, 1), data("2", "B", "500.00", "0", 1, 0)));

        // act
        ExploreResultDto spending = run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE);
        ExploreResultDto income = run(ExploreMeasure.INCOME, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE);

        // assert & verify
        assertEquals(List.of("A"), spending.rows().stream().map(ExploreRowDto::label).toList());
        assertEquals(List.of("B"), income.rows().stream().map(ExploreRowDto::label).toList());
    }

    @Test
    @DisplayName("should report the excluded count and the transaction total")
    void shouldReportExcludedCount() {
        // arrange
        stubOneCategory();
        when(repository.countExcluded(eq(USER_ID), any(), any())).thenReturn(38L);

        // act
        ExploreResultDto result = run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE);

        // assert & verify
        assertEquals(38L, result.excludedCount());
        assertEquals(2L, result.totalCount());
        assertNull(result.comparisonStart());
        assertNull(result.comparisonLabel());
    }

    @Test
    @DisplayName("should not double count a transaction that carries two tags in the total")
    void shouldNotDoubleCountTagsInTheTotal() {
        // arrange -- one 100 expense with two tags appears under both tag rows
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.TAG), any(), any())).thenReturn(
                List.of(data("1", "Vacation", "0", AMOUNT_100, 0, 1), data("2", "Work", "0", AMOUNT_100, 0, 1)));
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.ACCOUNT), any(), any())).thenReturn(
                List.of(data("9", "Checking", "0", AMOUNT_100, 0, 1)));

        // act
        ExploreResultDto result = run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.TAG, ExploreCompare.NONE);

        // assert & verify
        assertEquals(2, result.rows().size());
        assertValue(AMOUNT_100, result.total());
        assertEquals(1L, result.totalCount());
    }

    @Test
    @DisplayName("should sort rows by value, largest first, with empty values last")
    void shouldSortByValueDescending() {
        // arrange
        when(repository.findGroups(eq(USER_ID), any(), any(), any())).thenReturn(List.of(
                data("1", "Small", "0", AMOUNT_10, 0, 1), data("2", "Big", "0", "90.00", 0, 1), data("3", "Mid", "0", AMOUNT_50, 0, 1)));

        // act
        ExploreResultDto result = run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.CATEGORY, ExploreCompare.NONE);

        // assert & verify
        assertEquals(List.of("Big", "Mid", "Small"), result.rows().stream().map(ExploreRowDto::label).toList());
    }

    @Test
    @DisplayName("should name time buckets for people")
    void shouldNameTimeBuckets() {
        // arrange
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.MONTH), any(), any()))
                .thenReturn(List.of(data(DAY_2026_03_01, DAY_2026_03_01, "0", AMOUNT_10, 0, 1)));
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.DAY_OF_WEEK), any(), any()))
                .thenReturn(List.of(data("3", "3", "0", AMOUNT_10, 0, 1)));
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.WEEK), any(), any()))
                .thenReturn(List.of(data("2026-03-02", "2026-03-02", "0", AMOUNT_10, 0, 1)));

        // act & assert & verify
        assertEquals("Mar 2026", only(run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.MONTH, ExploreCompare.NONE)).label());
        assertEquals("Wednesday", only(run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.DAY_OF_WEEK, ExploreCompare.NONE)).label());
        assertEquals("Week of Mar 2, 2026", only(run(ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.WEEK, ExploreCompare.NONE)).label());
    }

    @Test
    @DisplayName("should line a month up with the same month last year when comparing same days last year")
    void shouldAlignMonthsWithTheSameMonthLastYear() {
        // arrange
        when(repository.findGroups(eq(USER_ID), eq(ExploreGroupBy.MONTH), any(), any())).thenReturn(
                List.of(data(DAY_2026_03_01, DAY_2026_03_01, "0", AMOUNT_100, 0, 1)),
                List.of(data("2025-03-01", "2025-03-01", "0", AMOUNT_80, 0, 1)));

        // act
        ExploreResultDto result = service.explore(USER_ID, ExploreMeasure.SPENDING, ExploreSide.SPENDING, ExploreGroupBy.MONTH,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), ExploreCompare.SAME_DAYS_LAST_YEAR, NO_FILTER, TODAY);

        // assert & verify
        assertValue(AMOUNT_80, only(result).comparisonValue());
    }
}
