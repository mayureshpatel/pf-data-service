package com.mayureshpatel.pfdataservice.domain.transaction;

import java.time.LocalDate;
import java.util.Locale;

/** How often a {@link RecurringTransaction}'s {@code nextDate} advances once it fires. */
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
        return Frequency.valueOf(code.toUpperCase(Locale.ROOT));
    }

    /**
     * Advances a date by one period of this frequency, by real calendar increments (a month, a
     * quarter, a year), not fixed day counts.
     *
     * @param date the date to advance
     * @return the date one period later
     */
    public LocalDate advance(LocalDate date) {
        return switch (this) {
            case WEEKLY -> date.plusWeeks(1);
            case BI_WEEKLY -> date.plusWeeks(2);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY -> date.plusYears(1);
        };
    }
}
