package com.mayureshpatel.pfdataservice.dto.report.explore;

/** How the Reports explorer groups its rows. */
public enum ExploreGroupBy {
    CATEGORY,
    SUBCATEGORY,
    MERCHANT,
    ACCOUNT,
    TAG,
    MONTH,
    WEEK,
    DAY_OF_WEEK,
    DAY;

    /**
     * @return whether this grouping buckets by time
     */
    public boolean isTime() {
        return this == MONTH || this == WEEK || this == DAY;
    }
}
