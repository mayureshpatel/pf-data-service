package com.mayureshpatel.pfdataservice.domain.transaction;

public enum Frequency {
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    /**
     * Gets the Frequency enum from the code.
     *
     * @param code the code to get the Frequency enum from
     * @return the Frequency enum
     */
    public static Frequency fromCode(String code) {
        return Frequency.valueOf(code.toUpperCase());
    }
}
