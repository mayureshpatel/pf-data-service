package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.math.BigDecimal;

/**
 * What happened in one account over the range. {@code closingBalance - openingBalance} always equals
 * {@code deposits - withdrawals + interestAndAdjustments}.
 *
 * @param openingBalance         the balance at the month end before the range
 * @param closingBalance         the balance at the end of the range
 * @param deposits               income and transfers in
 * @param withdrawals            expenses and transfers out, as a positive amount
 * @param interestAndAdjustments the signed sum of every other transaction type (adjustments and plain transfers)
 */
public record NetWorthStoryDto(BigDecimal openingBalance, BigDecimal closingBalance, BigDecimal deposits,
                               BigDecimal withdrawals, BigDecimal interestAndAdjustments) {
}
