package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totals at the end of the comparison window.
 *
 * @param label    plain-language name, such as "a year ago"
 * @param start    the first day of the comparison window
 * @param end      the last day of the comparison window
 * @param netWorth net worth at the window's end
 * @param assets   assets at the window's end
 * @param debts    the amount owed at the window's end
 */
public record NetWorthComparisonDto(String label, LocalDate start, LocalDate end, BigDecimal netWorth,
                                    BigDecimal assets, BigDecimal debts) {
}
