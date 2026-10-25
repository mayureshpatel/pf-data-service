package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.service.ExploreRanges.ResolvedRanges;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@code ExploreRanges}: the half-open UTC windows the explorer queries, and the earlier period it compares with (same length, same elapsed days when the period is still in progress). */
@DisplayName("ExploreRanges Unit Tests")
class ExploreRangesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    private static OffsetDateTime utc(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("should treat endDate as inclusive")
    void shouldTreatEndDateAsInclusive() {
        // act
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ExploreCompare.NONE, TODAY);

        // assert & verify
        assertEquals(new DateWindow(utc(2026, 1, 1), utc(2026, 2, 1)), ranges.current());
        assertFalse(ranges.inProgress());
    }

    @Test
    @DisplayName("should return no comparison for NONE")
    void shouldReturnNoComparisonForNone() {
        // act
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ExploreCompare.NONE, TODAY);

        // assert & verify
        assertNull(ranges.comparison());
        assertNull(ranges.comparisonLabel());
    }

    @Test
    @DisplayName("should compare the previous period of the same length")
    void shouldComparePreviousPeriodOfSameLength() {
        // act -- 31 days in January, so the comparison is the 31 days before it
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ExploreCompare.PREVIOUS_PERIOD, TODAY);

        // assert & verify
        assertEquals(new DateWindow(utc(2025, 12, 1), utc(2026, 1, 1)), ranges.comparison());
    }

    @Test
    @DisplayName("should clamp an in-progress range to today and compare the same number of days")
    void shouldClampInProgressRangeAndCompareSameDays() {
        // act -- this year to date: Jan 1 to Oct 25 is 298 days, but the caller asked for the whole year
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), ExploreCompare.SAME_DAYS_LAST_YEAR, TODAY);

        // assert & verify
        assertTrue(ranges.inProgress());
        assertEquals(new DateWindow(utc(2026, 1, 1), utc(2026, 10, 26)), ranges.current());
        assertEquals(new DateWindow(utc(2025, 1, 1), utc(2025, 10, 26)), ranges.comparison());
    }

    @Test
    @DisplayName("should keep the previous period the same length when the current one is in progress")
    void shouldKeepPreviousPeriodSameLengthWhenInProgress() {
        // act -- October so far is 25 days, so the comparison is the 25 days before Oct 1
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), ExploreCompare.PREVIOUS_PERIOD, TODAY);

        // assert & verify
        assertEquals(new DateWindow(utc(2026, 9, 6), utc(2026, 10, 1)), ranges.comparison());
    }

    @Test
    @DisplayName("should compare same days last year")
    void shouldCompareSameDaysLastYear() {
        // act
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), ExploreCompare.SAME_DAYS_LAST_YEAR, TODAY);

        // assert & verify
        assertEquals(new DateWindow(utc(2025, 3, 1), utc(2025, 4, 1)), ranges.comparison());
    }

    @Test
    @DisplayName("should map Feb 29 to Feb 28 last year")
    void shouldMapLeapDayToFeb28() {
        // act -- 2024 is a leap year, 2023 is not
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), ExploreCompare.SAME_DAYS_LAST_YEAR, LocalDate.of(2026, 1, 1));

        // assert & verify
        assertEquals(new DateWindow(utc(2023, 2, 1), utc(2023, 3, 1)), ranges.comparison());
    }

    @Test
    @DisplayName("should label the comparison with real dates")
    void shouldLabelComparisonWithRealDates() {
        // act
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), ExploreCompare.SAME_DAYS_LAST_YEAR, TODAY);

        // assert & verify
        assertEquals("same days last year (Jan 1 to Oct 25, 2025)", ranges.comparisonLabel());
    }

    @Test
    @DisplayName("should have no comparison for a range that has not started yet")
    void shouldHaveNoComparisonForFutureRange() {
        // act
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), ExploreCompare.PREVIOUS_PERIOD, TODAY);

        // assert & verify
        assertNull(ranges.comparison());
        assertEquals(ranges.current().start(), ranges.current().end());
    }

    @Test
    @DisplayName("should compare whole months with whole months for a previous period in monthly buckets")
    void shouldAlignPreviousPeriodToMonths() {
        // act -- Jan 1 to Oct 25 is 10 monthly buckets, so the comparison is the 10 months before
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                ExploreCompare.PREVIOUS_PERIOD, TODAY, ExploreGroupBy.MONTH);

        // assert & verify -- Mar 1 to Dec 25, 2025: the last bucket is the same 25 days of December
        assertEquals(new DateWindow(utc(2025, 3, 1), utc(2025, 12, 26)), ranges.comparison());
    }

    @Test
    @DisplayName("should start the comparison on the same weekday for weekly buckets")
    void shouldAlignSameDaysLastYearToWeeks() {
        // act -- Monday Jul 27 to Sunday Oct 25, 2026 is 13 weeks
        ResolvedRanges ranges = ExploreRanges.resolve(LocalDate.of(2026, 7, 27), LocalDate.of(2026, 10, 25),
                ExploreCompare.SAME_DAYS_LAST_YEAR, TODAY, ExploreGroupBy.WEEK);

        // assert & verify -- 52 weeks earlier is Monday Jul 28, 2025 to Sunday Oct 26, 2025
        assertEquals(new DateWindow(utc(2025, 7, 28), utc(2025, 10, 27)), ranges.comparison());
    }

    @Test
    @DisplayName("should keep the day-based comparison for day buckets and non-time groupings")
    void shouldKeepDayBasedComparisonWithoutCalendarBuckets() {
        // act
        ResolvedRanges plain = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ExploreCompare.PREVIOUS_PERIOD, TODAY);
        ResolvedRanges day = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ExploreCompare.PREVIOUS_PERIOD, TODAY, ExploreGroupBy.DAY);
        ResolvedRanges category = ExploreRanges.resolve(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ExploreCompare.PREVIOUS_PERIOD, TODAY, ExploreGroupBy.CATEGORY);

        // assert & verify
        assertEquals(plain, day);
        assertEquals(plain, category);
    }
}
