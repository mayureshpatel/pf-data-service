package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Refuses explorer ranges that make no sense, so a typo such as year 0002 gets a clear 400 instead of
 * an enormous or empty report. The same rules apply to the explorer, its trend and its transaction list.
 * Failures are {@link IllegalArgumentException}s, which the global handler turns into a 400.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ExploreRangeValidator {

    /** The earliest start date accepted. */
    public static final LocalDate EARLIEST_START = LocalDate.of(2000, 1, 1);

    /** How far past today the end date may reach, in years. */
    public static final int MAX_YEARS_AHEAD = 1;

    /** The most trend buckets (at the granularity the server would pick) one request may span. */
    public static final int MAX_BUCKETS = 360;

    /**
     * Checks a range against the bounds.
     *
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param today     the date treated as today
     * @throws IllegalArgumentException if the start is before 2000-01-01, the end is more than a year past
     *                                  today, the start is after the end, or the range spans too many buckets
     */
    public static void validate(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate.isBefore(EARLIEST_START)) {
            throw new IllegalArgumentException("The start date cannot be before " + EARLIEST_START);
        }
        LocalDate latestEnd = today.plusYears(MAX_YEARS_AHEAD);
        if (endDate.isAfter(latestEnd)) {
            throw new IllegalArgumentException("The end date cannot be after " + latestEnd);
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("The start date cannot be after the end date");
        }
        requireBucketCap(startDate, endDate, today);
    }

    private static void requireBucketCap(LocalDate startDate, LocalDate endDate, LocalDate today) {
        // only the days up to today are bucketed, so a range that is mostly in the future stays small
        LocalDate shownEnd = endDate.isAfter(today) ? today : endDate;
        if (shownEnd.isBefore(startDate)) {
            return;
        }
        ExploreGroupBy granularity = ExploreBuckets.granularityFor(ChronoUnit.DAYS.between(startDate, shownEnd) + 1);
        if (ExploreBuckets.count(granularity, startDate, shownEnd) > MAX_BUCKETS) {
            throw new IllegalArgumentException("The range spans too many " + granularity.name().toLowerCase(java.util.Locale.ROOT)
                    + " buckets (the limit is " + MAX_BUCKETS + "); choose a shorter range");
        }
    }
}
