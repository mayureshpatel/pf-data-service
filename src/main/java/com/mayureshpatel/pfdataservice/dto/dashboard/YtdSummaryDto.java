package com.mayureshpatel.pfdataservice.dto.dashboard;

import java.math.BigDecimal;

/**
 * Represents year-to-date (YTD) summary data for financial analysis.
 *
 * @param year           year for which the summary is calculated
 * @param totalIncome    total income for the year
 * @param totalExpense   total expense for the year
 * @param netSavings     net savings for the year
 * @param avgSavingsRate average savings rate for the year
 */
public record YtdSummaryDto(
        int year,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings,
        BigDecimal avgSavingsRate
) {
}
