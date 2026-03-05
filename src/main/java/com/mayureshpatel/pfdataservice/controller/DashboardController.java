package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.ActionItemDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.CashFlowTrendDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardPulseDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.YtdSummaryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.DashboardService;
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

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    private static final ZoneId UTC_ZONE = ZoneOffset.UTC;

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

    @GetMapping("/trend/cashflow")
    public ResponseEntity<List<CashFlowTrendDto>> getCashFlowTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getCashFlowTrend(userDetails.getId()));
    }

    @GetMapping("/ytd")
    public ResponseEntity<YtdSummaryDto> getYtdSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @Min(2000) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardService.getYtdSummary(userDetails.getId(), y));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<ActionItemDto>> getActionItems(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getActionItems(userDetails.getId()));
    }
}
