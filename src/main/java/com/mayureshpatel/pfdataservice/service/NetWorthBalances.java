package com.mayureshpatel.pfdataservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The month-end balance math behind the net worth view. A balance at a month end is the account's current
 * balance minus every net flow dated after that month; flows are keyed by the month they happened in, so a
 * transaction made on the last day of a month belongs to that month.
 */
public final class NetWorthBalances {

    private NetWorthBalances() {
    }

    /**
     * The last day of every month from {@code startDate}'s month to {@code endDate}'s month, inclusive.
     *
     * @param startDate any date in the first month
     * @param endDate   any date in the last month
     * @return the month ends, oldest first
     */
    public static List<LocalDate> monthEnds(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> ends = new ArrayList<>();
        YearMonth month = YearMonth.from(startDate);
        YearMonth last = YearMonth.from(endDate);
        while (!month.isAfter(last)) {
            ends.add(month.atEndOfMonth());
            month = month.plusMonths(1);
        }
        return ends;
    }

    /**
     * An account's balance at a month end.
     *
     * @param currentBalance the account's stored current balance
     * @param netFlowByMonth the account's signed net flow per month
     * @param monthEnd       the month end to compute the balance at
     * @param today          today's date; a month end on or after it takes the current balance as it is
     * @return the balance at {@code monthEnd}
     */
    public static BigDecimal balanceAt(BigDecimal currentBalance, Map<YearMonth, BigDecimal> netFlowByMonth,
                                       LocalDate monthEnd, LocalDate today) {
        if (!monthEnd.isBefore(today)) {
            return currentBalance;
        }
        YearMonth month = YearMonth.from(monthEnd);
        BigDecimal later = BigDecimal.ZERO;
        for (Map.Entry<YearMonth, BigDecimal> flow : netFlowByMonth.entrySet()) {
            if (flow.getKey().isAfter(month)) {
                later = later.add(flow.getValue());
            }
        }
        return currentBalance.subtract(later);
    }

    /**
     * Whether an account had been created by the given date, so an account made partway through a range does
     * not contribute a balance to month ends before it existed.
     *
     * @param createdAt when the account was created, or null when unknown (treated as always existing)
     * @param date      the date to check
     * @return true when the account existed on {@code date}
     */
    public static boolean existedAsOf(OffsetDateTime createdAt, LocalDate date) {
        return createdAt == null || !createdAt.toLocalDate().isAfter(date);
    }
}
