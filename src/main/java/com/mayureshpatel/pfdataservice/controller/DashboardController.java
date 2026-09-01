package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.ActionItemDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.CashFlowTrendDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardPulseDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.YtdSummaryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Aggregated data for the dashboard: spending breakdowns, income/expense pulse, cash-flow trend,
 * year-to-date summary, and actionable items. Most endpoints accept either a month/year pair or
 * an explicit startDate/endDate range -- if both are given, the explicit date range wins.
 */
@Tag(name = "Dashboard", description = "Aggregated spending, income, and trend data for the dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    private static final ZoneId UTC_ZONE = ZoneOffset.UTC;

    /**
     * Returns spending grouped by category for a period, defaulting to the current month.
     *
     * @param userDetails the authenticated user
     * @param month       the period month (1-12), ignored if startDate/endDate are given
     * @param year        the period year, ignored if startDate/endDate are given
     * @param startDate   an explicit period start, overrides month/year if paired with endDate
     * @param endDate     an explicit period end, overrides month/year if paired with startDate
     * @return the category breakdown for the resolved period
     */
    @Operation(summary = "Category spending breakdown", description = "Spending grouped by category for a period, defaulting to the current month")
    @ApiResponse(responseCode = "200", description = "Breakdown returned (possibly empty)")
    @ApiResponse(responseCode = "400", description = "month or year outside the allowed range")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryBreakdownDto>> getCategoryBreakdown(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            OffsetDateTime start = startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime();
            OffsetDateTime end = endDate.atTime(23, 59, 59).atZone(UTC_ZONE).toOffsetDateTime();
            return ResponseEntity.ok(dashboardService.getCategoryBreakdown(userDetails.getId(), start, end));
        }

        LocalDate now = LocalDate.now();
        int montValue = month != null ? month : now.getMonthValue();
        int yearValue = year != null ? year : now.getYear();

        return ResponseEntity.ok(dashboardService.getCategoryBreakdown(userDetails.getId(), montValue, yearValue));
    }

    /**
     * Returns spending grouped by merchant for a period, defaulting to the current month.
     *
     * @param userDetails the authenticated user
     * @param month       the period month (1-12), ignored if startDate/endDate are given
     * @param year        the period year, ignored if startDate/endDate are given
     * @param startDate   an explicit period start, overrides month/year if paired with endDate
     * @param endDate     an explicit period end, overrides month/year if paired with startDate
     * @return the merchant breakdown for the resolved period
     */
    @Operation(summary = "Merchant spending breakdown", description = "Spending grouped by merchant for a period, defaulting to the current month")
    @ApiResponse(responseCode = "200", description = "Breakdown returned (possibly empty)")
    @ApiResponse(responseCode = "400", description = "month or year outside the allowed range")
    @GetMapping("/merchants")
    public ResponseEntity<List<MerchantBreakdownDto>> getMerchantBreakdown(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            OffsetDateTime start = startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime();
            OffsetDateTime end = endDate.atTime(23, 59, 59).atZone(UTC_ZONE).toOffsetDateTime();
            return ResponseEntity.ok(dashboardService.getMerchantBreakdown(userDetails.getId(), start, end));
        }

        LocalDate now = LocalDate.now();
        int monthValue = month != null ? month : now.getMonthValue();
        int yearValue = year != null ? year : now.getYear();

        return ResponseEntity.ok(dashboardService.getMerchantBreakdown(userDetails.getId(), monthValue, yearValue));
    }

    /**
     * Returns the income/expense "pulse" for a period compared to the equivalent previous
     * period, defaulting to the current month vs. the prior month.
     *
     * @param userDetails the authenticated user
     * @param month       the period month (1-12), ignored if startDate/endDate are given
     * @param year        the period year, ignored if startDate/endDate are given
     * @param startDate   an explicit period start, overrides month/year if paired with endDate
     * @param endDate     an explicit period end, overrides month/year if paired with startDate
     * @return current vs. previous period income, expense, and savings rate
     */
    @Operation(summary = "Income/expense pulse", description = "Current vs. previous period income, expense, and savings rate")
    @ApiResponse(responseCode = "200", description = "Pulse data returned")
    @ApiResponse(responseCode = "400", description = "month or year outside the allowed range")
    @GetMapping("/pulse")
    public ResponseEntity<DashboardPulseDto> getPulse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            OffsetDateTime start = startDate.atStartOfDay(UTC_ZONE).toOffsetDateTime();
            OffsetDateTime end = endDate.atTime(23, 59, 59).atZone(UTC_ZONE).toOffsetDateTime();
            return ResponseEntity.ok(dashboardService.getPulse(userDetails.getId(), start, end));
        }

        LocalDate now = LocalDate.now();
        int monthValue = month != null ? month : now.getMonthValue();
        int yearValue = year != null ? year : now.getYear();

        return ResponseEntity.ok(dashboardService.getPulse(userDetails.getId(), monthValue, yearValue));
    }

    /**
     * Returns monthly income/expense totals for the trailing 12 months, with gap months filled
     * in as zero so the series has no missing points.
     *
     * @param userDetails the authenticated user
     * @return the 12-month cash-flow trend
     */
    @Operation(summary = "Cash-flow trend", description = "Monthly income/expense totals for the trailing 12 months, zero-filled for continuity")
    @ApiResponse(responseCode = "200", description = "Trend returned")
    @GetMapping("/trend/cashflow")
    public ResponseEntity<List<CashFlowTrendDto>> getCashFlowTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getCashFlowTrend(userDetails.getId()));
    }

    /**
     * Returns the year-to-date income/expense/savings summary for a year, defaulting to the
     * current year.
     *
     * @param userDetails the authenticated user
     * @param year        the year to summarize
     * @return the year-to-date summary
     */
    @Operation(summary = "Year-to-date summary", description = "Year-to-date income, expense, and savings, defaulting to the current year")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @ApiResponse(responseCode = "400", description = "year outside the allowed range")
    @GetMapping("/ytd")
    public ResponseEntity<YtdSummaryDto> getYtdSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @Min(2000) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardService.getYtdSummary(userDetails.getId(), y));
    }

    /**
     * Returns suggested follow-up actions for the user (e.g. potential transfers to confirm,
     * uncategorized spending to review).
     *
     * @param userDetails the authenticated user
     * @return the current action items
     */
    @Operation(summary = "List action items", description = "Suggested follow-up actions, e.g. potential transfers to confirm or uncategorized spending to review")
    @ApiResponse(responseCode = "200", description = "Action items returned (possibly empty)")
    @GetMapping("/actions")
    public ResponseEntity<List<ActionItemDto>> getActionItems(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getActionItems(userDetails.getId()));
    }
}
