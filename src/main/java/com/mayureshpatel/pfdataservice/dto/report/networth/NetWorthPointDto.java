package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Net worth at one month end.
 *
 * @param date     the month end this point stands for
 * @param assets   the summed balances of asset-type accounts
 * @param debts    the amount owed: minus the summed balances of non-asset accounts (positive when something is owed)
 * @param netWorth {@code assets - debts}, the sum of every included balance
 * @param partial  true when the point is for a month that is not over yet (balances are then as of today)
 */
public record NetWorthPointDto(LocalDate date, BigDecimal assets, BigDecimal debts, BigDecimal netWorth, boolean partial) {
}
