package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.DateWindow;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Turns the explorer's inclusive date range into the half-open UTC windows its queries use, and works
 * out the earlier period to compare with. A period still in progress is clamped to today, and the
 * comparison covers the same number of days, so a partial year is never set against a full one.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ExploreRanges {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter DAY_WITH_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    /**
     * The windows the explorer queries.
     *
     * @param current         the shown period (clamped to today when it is still in progress)
     * @param comparison      the earlier period to compare with, or null
     * @param comparisonLabel a readable description of the comparison with its real dates, or null
     * @param inProgress      whether the requested range runs past today
     */
    public record ResolvedRanges(DateWindow current, DateWindow comparison, String comparisonLabel, boolean inProgress) {
    }

    /**
     * Resolves the windows for a range.
     *
     * @param startDate the first day, inclusive
     * @param endDate   the last day, inclusive
     * @param compare   which earlier period to compare with
     * @param today     the current date (a parameter so tests control it)
     * @return the current and comparison windows
     */
    public static ResolvedRanges resolve(LocalDate startDate, LocalDate endDate, ExploreCompare compare, LocalDate today) {
        boolean inProgress = endDate.isAfter(today);
        LocalDate lastDay = inProgress ? today : endDate;
        if (lastDay.isBefore(startDate)) {
            DateWindow empty = new DateWindow(startOfDay(startDate), startOfDay(startDate));
            return new ResolvedRanges(empty, null, null, inProgress);
        }

        DateWindow current = new DateWindow(startOfDay(startDate), startOfDay(lastDay.plusDays(1)));
        if (compare == null || compare == ExploreCompare.NONE) {
            return new ResolvedRanges(current, null, null, inProgress);
        }

        long days = ChronoUnit.DAYS.between(startDate, lastDay) + 1;
        LocalDate comparisonStart = compare == ExploreCompare.PREVIOUS_PERIOD ? startDate.minusDays(days) : startDate.minusYears(1);
        LocalDate comparisonLast = compare == ExploreCompare.PREVIOUS_PERIOD ? startDate.minusDays(1) : lastDay.minusYears(1);
        DateWindow comparison = new DateWindow(startOfDay(comparisonStart), startOfDay(comparisonLast.plusDays(1)));
        return new ResolvedRanges(current, comparison, label(compare, comparisonStart, comparisonLast), inProgress);
    }

    private static String label(ExploreCompare compare, LocalDate start, LocalDate last) {
        String name = compare == ExploreCompare.PREVIOUS_PERIOD ? "previous period" : "same days last year";
        return name + " (" + DAY.format(start) + " to " + DAY_WITH_YEAR.format(last) + ")";
    }

    private static OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}
