package com.mayureshpatel.pfdataservice.dto.report.explore;

/** What the Reports explorer measures. Net and savings rate only make sense over time. */
public enum ExploreMeasure {
    SPENDING,
    INCOME,
    NET,
    SAVINGS_RATE,
    COUNT,
    AVERAGE;

    /**
     * @return whether this measure may only be grouped by a time bucket (month, week, day)
     */
    public boolean timeOnly() {
        return this == NET || this == SAVINGS_RATE;
    }
}
