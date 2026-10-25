package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Names time buckets for people and lines a bucket up with its counterpart in the comparison period. */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ExploreLabels {

    private static final int WEEKS_IN_YEAR = 52;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    /**
     * @param groupBy  the grouping
     * @param key      the group's key
     * @param rawLabel the label the query produced
     * @return the label to show: time buckets get readable names, other groupings keep their own
     */
    public static String label(ExploreGroupBy groupBy, String key, String rawLabel) {
        if (key == null) {
            return rawLabel;
        }
        return switch (groupBy) {
            case MONTH -> MONTH.format(LocalDate.parse(key));
            case WEEK -> "Week of " + DAY.format(LocalDate.parse(key));
            case DAY -> DAY.format(LocalDate.parse(key));
            case DAY_OF_WEEK -> DayOfWeek.of(Integer.parseInt(key)).getDisplayName(TextStyle.FULL, Locale.US);
            default -> rawLabel;
        };
    }

    /**
     * Finds the key of the comparison-period bucket that a current bucket should be compared with. Groupings
     * that are not buckets of time, and the day of the week, compare by the same key.
     *
     * @param groupBy the grouping
     * @param key     the current bucket's key
     * @param compare the comparison mode
     * @param days    how many days the current window spans (for the previous period)
     * @return the comparison bucket's key
     */
    public static String comparisonKey(ExploreGroupBy groupBy, String key, ExploreCompare compare, long days) {
        if (key == null || !groupBy.isTime()) {
            return key;
        }
        LocalDate date = LocalDate.parse(key);
        LocalDate shifted = compare == ExploreCompare.SAME_DAYS_LAST_YEAR ? date.minusYears(1) : date.minusDays(days);
        return switch (groupBy) {
            case MONTH -> shifted.withDayOfMonth(1).toString();
            case WEEK -> shifted.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
            default -> shifted.toString();
        };
    }

    /**
     * Like {@link #comparisonKey(ExploreGroupBy, String, ExploreCompare, long)} for results bucketed by
     * month or week whose comparison window shifts by whole calendar units (see {@code ExploreRanges}): the
     * previous period moves back by the number of buckets the range spans, same days last year by a year
     * (months) or 52 weeks (weeks).
     *
     * @param groupBy the grouping
     * @param key     the current bucket's key
     * @param compare the comparison mode
     * @param days    how many days the current window spans (used for day buckets)
     * @param buckets how many month or week buckets the current window spans
     * @return the comparison bucket's key
     */
    public static String comparisonKey(ExploreGroupBy groupBy, String key, ExploreCompare compare, long days, int buckets) {
        if (key == null || (groupBy != ExploreGroupBy.MONTH && groupBy != ExploreGroupBy.WEEK)) {
            return comparisonKey(groupBy, key, compare, days);
        }
        LocalDate date = LocalDate.parse(key);
        boolean month = groupBy == ExploreGroupBy.MONTH;
        if (compare == ExploreCompare.SAME_DAYS_LAST_YEAR) {
            return month ? date.minusYears(1).withDayOfMonth(1).toString() : date.minusWeeks(WEEKS_IN_YEAR).toString();
        }
        return month ? date.minusMonths(buckets).withDayOfMonth(1).toString() : date.minusWeeks(buckets).toString();
    }
}
