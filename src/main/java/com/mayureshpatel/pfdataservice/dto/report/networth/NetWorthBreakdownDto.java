package com.mayureshpatel.pfdataservice.dto.report.networth;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the Reports net worth view shows, in one response.
 *
 * @param points     one point per month end in the range, oldest first
 * @param opening    the point for the month end before the range
 * @param asOf       the month end the summary and the groups' balances are taken at
 * @param summary    the cards' figures
 * @param moved      the saved-versus-other split of the change
 * @param comparison totals for the comparison window, or null when not comparing
 * @param groups     the rows: assets first, largest first, then debts
 */
public record NetWorthBreakdownDto(List<NetWorthPointDto> points, NetWorthPointDto opening, LocalDate asOf,
                                   NetWorthSummaryDto summary, NetWorthMovedDto moved,
                                   NetWorthComparisonDto comparison, List<NetWorthGroupDto> groups) {
}
