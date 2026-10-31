package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.math.BigDecimal;

/**
 * The cards' figures at the as-of month end.
 *
 * @param netWorth               assets minus debts
 * @param assets                 the summed balances of asset-type accounts
 * @param debts                  the amount owed, positive
 * @param liquidCash             the balances of checking, savings and cash accounts
 * @param averageMonthlySpending expenses over the three calendar months up to the range end, divided by three
 * @param monthsCovered          liquid cash divided by the average monthly spending, or null when that spending is zero
 */
public record NetWorthSummaryDto(BigDecimal netWorth, BigDecimal assets, BigDecimal debts, BigDecimal liquidCash,
                                 BigDecimal averageMonthlySpending, BigDecimal monthsCovered) {
}
