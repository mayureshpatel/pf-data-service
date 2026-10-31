package com.mayureshpatel.pfdataservice.dto.report.networth;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The query parameters of {@code GET /api/v1/reports/net-worth/breakdown}, bound by name. Every parameter is
 * optional.
 *
 * @param startDate  any date in the first month (default: the first of the month 11 months before the end)
 * @param endDate    any date in the last month (default: today)
 * @param groupBy    group by account type or by account (default type)
 * @param accountIds restrict to these accounts (default all)
 * @param typeCode   restrict to one account type, for a drill (default all)
 * @param compare    which earlier window to compare with (default none)
 * @param asOf       the month the summary and balances are taken at, as {@code yyyy-MM} (default the last month)
 */
public record NetWorthBreakdownRequest(
        LocalDate startDate,
        LocalDate endDate,
        NetWorthGroupBy groupBy,
        List<Long> accountIds,
        String typeCode,
        NetWorthCompare compare,
        @DateTimeFormat(pattern = "yyyy-MM") YearMonth asOf
) {

    /**
     * @param today today's date
     * @return the end date, defaulting to today
     */
    public LocalDate endDateOrDefault(LocalDate today) {
        return endDate == null ? today : endDate;
    }

    /**
     * @param today today's date
     * @return the start date, defaulting to the first of the month 11 months before the end
     */
    public LocalDate startDateOrDefault(LocalDate today) {
        return startDate == null ? endDateOrDefault(today).minusMonths(11).withDayOfMonth(1) : startDate;
    }

    /**
     * @return the grouping, defaulting to account type
     */
    public NetWorthGroupBy groupByOrDefault() {
        return groupBy == null ? NetWorthGroupBy.TYPE : groupBy;
    }

    /**
     * @return the comparison, defaulting to none
     */
    public NetWorthCompare compareOrDefault() {
        return compare == null ? NetWorthCompare.NONE : compare;
    }

    /**
     * @return the account filter, empty when none was given
     */
    public List<Long> accountIdsOrEmpty() {
        return accountIds == null ? List.of() : accountIds;
    }
}
