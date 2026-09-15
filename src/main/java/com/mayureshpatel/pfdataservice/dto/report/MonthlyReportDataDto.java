package com.mayureshpatel.pfdataservice.dto.report;

import java.math.BigDecimal;

/**
 * A single month's income vs. expense totals within a Reports date range (PF-823). Transfers
 * ({@code TRANSFER}/{@code TRANSFER_IN}/{@code TRANSFER_OUT}) are excluded from both sides, same
 * as every other aggregate income/expense query in this codebase (e.g.
 * {@code TransactionQueries.FIND_MONTHLY_SUMS}) -- a transfer between the user's own accounts is
 * neither real income nor real spending.
 *
 * @param year    the calendar year, in the account owner's data (UTC-anchored)
 * @param month   the calendar month (1-12)
 * @param income  the sum of INCOME amounts posted in this month
 * @param expense the sum of EXPENSE amounts posted in this month, as a positive magnitude
 */
public record MonthlyReportDataDto(
        int year,
        int month,
        BigDecimal income,
        BigDecimal expense
) {
}
