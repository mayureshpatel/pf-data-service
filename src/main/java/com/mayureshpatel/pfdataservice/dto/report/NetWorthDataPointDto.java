package com.mayureshpatel.pfdataservice.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single point in a net-worth-over-time series (PF-304): the user's total net worth, summed
 * across every account they owned as of the given month-end date.
 *
 * @param date     the month-end date this point represents
 * @param netWorth the sum of all owned accounts' balances as of {@code date}
 */
public record NetWorthDataPointDto(
        LocalDate date,
        BigDecimal netWorth
) {
}
