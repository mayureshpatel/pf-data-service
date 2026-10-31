package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreRequest;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreResultDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreScopeRequest;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTransactionDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTrendDto;
import com.mayureshpatel.pfdataservice.service.ExploreService;
import com.mayureshpatel.pfdataservice.service.NetWorthService;
import com.mayureshpatel.pfdataservice.dto.report.NetWorthDataPointDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.report.networth.NetWorthBreakdownRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Report-oriented views over a user's data, distinct from {@link DashboardController}'s
 * current-period aggregates. Introduced by PF-304 for net-worth-over-time; the home for future
 * report types this epic's own refinement named as candidates.
 */
@Tag(name = "Reports", description = "Report-oriented views over the authenticated user's data")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExploreService exploreService;
    private final NetWorthService netWorthService;

    /**
     * Returns the user's total net worth as of the end of each month in the requested range,
     * defaulting to the trailing 12 months if no range is given.
     *
     * @param userDetails the authenticated user
     * @param startDate   any date in the first month of the requested range
     * @param endDate     any date in the last month of the requested range
     * @return one data point per month-end in the resolved range, oldest first
     */
    @Operation(summary = "Net worth over time", description = "Total net worth as of the end of each month in the requested range, defaulting to the trailing 12 months")
    @ApiResponse(responseCode = "200", description = "Net-worth series returned (possibly empty, if the user owns no accounts)")
    @GetMapping("/net-worth")
    public ResponseEntity<List<NetWorthDataPointDto>> getNetWorth(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusMonths(11).withDayOfMonth(1);

        return ResponseEntity.ok(reportService.getNetWorthOverTime(userDetails.getId(), resolvedStart, resolvedEnd));
    }

    /**
     * Returns the Reports net worth view in one response: month-end assets, debts and net worth, the rows by
     * account type or account, the cards' figures, what moved net worth, and an optional comparison.
     *
     * @param userDetails the authenticated user
     * @param request     the range, grouping, filters and comparison, bound by name (all optional)
     * @return the whole view
     */
    @Operation(summary = "Net worth breakdown", description = "Month-end assets, debts and net worth by account type or account, what moved net worth, and an optional comparison")
    @ApiResponse(responseCode = "200", description = "Breakdown returned")
    @ApiResponse(responseCode = "400", description = "The range is reversed, the account type is unknown, or the month is outside the range")
    @GetMapping("/net-worth/breakdown")
    public ResponseEntity<NetWorthBreakdownDto> getNetWorthBreakdown(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute NetWorthBreakdownRequest request) {
        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(netWorthService.breakdown(
                userDetails.getId(), request.startDateOrDefault(today), request.endDateOrDefault(today),
                request.groupByOrDefault(), request.accountIdsOrEmpty(), request.typeCode(),
                request.compareOrDefault(), request.asOf(), today));
    }

    /**
     * Runs the Reports explorer: one measure over one grouping, under stacked filters, optionally compared
     * with an equivalent earlier period. See {@link ExploreRequest} for the parameters.
     *
     * @param userDetails the authenticated user
     * @param request     the explorer parameters, bound by name
     * @return the groups, totals and comparison
     */
    @Operation(summary = "Explore spending and income", description = "Any measure by any grouping under stacked filters, with an optional comparison period; transfers and adjustments are excluded")
    @ApiResponse(responseCode = "200", description = "Explorer result returned")
    @ApiResponse(responseCode = "400", description = "The measure cannot be grouped that way (net and savings rate group by time only)")
    @GetMapping("/explore")
    public ResponseEntity<ExploreResultDto> explore(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ExploreRequest request) {
        return ResponseEntity.ok(exploreService.explore(
                userDetails.getId(), request.measure(), request.sideOrDefault(), request.groupBy(),
                request.startDate(), request.endDate(), request.compareOrDefault(), request.toFilter()));
    }

    /**
     * Returns the explorer's trend: the chosen measure per day, week or month across the period, with the
     * comparison period's buckets lined up by position when a comparison is on.
     *
     * @param userDetails the authenticated user
     * @param request     the period, measure and filters, bound by name
     * @return the current and comparison buckets
     */
    @Operation(summary = "Explore trend", description = "The chosen measure per day, week or month across the period, under the same filters as the explorer")
    @ApiResponse(responseCode = "200", description = "Trend returned")
    @GetMapping("/explore/trend")
    public ResponseEntity<ExploreTrendDto> exploreTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ExploreScopeRequest request) {
        return ResponseEntity.ok(exploreService.trend(
                userDetails.getId(), request.measure(), request.sideOrDefault(), request.startDate(), request.endDate(),
                request.compareOrDefault(), request.toFilter()));
    }

    /**
     * Pages through the transactions behind an explorer number, newest first: the last step of a drill-down.
     *
     * @param userDetails the authenticated user
     * @param request     the period, measure and filters, bound by name
     * @param page        the zero-based page
     * @param size        the page size (capped at 100)
     * @return the page of transactions
     */
    @Operation(summary = "Explore transactions", description = "The transactions behind an explorer number, newest first, under the same filters")
    @ApiResponse(responseCode = "200", description = "Transactions returned")
    @GetMapping("/explore/transactions")
    public ResponseEntity<Page<ExploreTransactionDto>> exploreTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ExploreScopeRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(exploreService.transactions(
                userDetails.getId(), request.measure(), request.sideOrDefault(), request.startDate(), request.endDate(),
                request.toFilter(), page, size));
    }
}
