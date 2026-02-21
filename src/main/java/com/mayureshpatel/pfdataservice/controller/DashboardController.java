package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.vendor.VendorTotal;
import com.mayureshpatel.pfdataservice.dto.dashboard.ActionItemDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.CashFlowTrendDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardPulseDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.YtdSummaryDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryBreakdownDto>> getCategoryBreakdown(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(dashboardService.getCategoryBreakdown(userDetails.getId(), startDate, endDate));
        }

        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        return ResponseEntity.ok(dashboardService.getCategoryBreakdown(userDetails.getId(), m, y));
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<VendorTotal>> getVendorBreakdown(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(dashboardService.getMerchantBreakdown(userDetails.getId(), startDate, endDate));
        }

        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        return ResponseEntity.ok(dashboardService.getMerchantBreakdown(userDetails.getId(), m, y));
    }

    @GetMapping("/pulse")
    public ResponseEntity<DashboardPulseDto> getPulse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(dashboardService.getPulse(userDetails.getId(), startDate, endDate));
        }

        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        return ResponseEntity.ok(dashboardService.getPulse(userDetails.getId(), m, y));
    }

    @GetMapping("/trend/cashflow")
    public ResponseEntity<List<CashFlowTrendDto>> getCashFlowTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getCashFlowTrend(userDetails.getId()));
    }

    @GetMapping("/ytd")
    public ResponseEntity<YtdSummaryDto> getYtdSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(dashboardService.getYtdSummary(userDetails.getId(), y));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<ActionItemDto>> getActionItems(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getActionItems(userDetails.getId()));
    }
}
