package com.mayureshpatel.pfdataservice.dto.report.explore;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How coarse the explorer trend's buckets are. The server picks it from the length of the period and
 * returns it with the trend, so a client never has to infer it from the spacing of the buckets (which
 * cannot be done for a single bucket).
 */
public enum TrendGranularity {
    /** One bucket per day. */
    DAY,
    /** One bucket per week, starting on Monday. */
    WEEK,
    /** One bucket per calendar month. */
    MONTH;

    /**
     * @return the lowercase name used in the JSON response ({@code day}, {@code week} or {@code month})
     */
    @JsonValue
    public String json() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * @param groupBy a time grouping
     * @return the matching granularity; anything that is not week or month is treated as day
     */
    public static TrendGranularity of(ExploreGroupBy groupBy) {
        return switch (groupBy) {
            case WEEK -> WEEK;
            case MONTH -> MONTH;
            default -> DAY;
        };
    }
}
