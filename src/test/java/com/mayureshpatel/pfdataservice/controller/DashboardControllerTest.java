package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.ActionItemDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.CashFlowTrendDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardPulseDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.YtdSummaryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest extends BaseControllerTest {

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/pulse should return dashboard pulse")
    void getPulse_shouldReturnDashboardPulse() throws Exception {
        DashboardPulseDto pulseDto = new DashboardPulseDto(
                new BigDecimal("5000.00"), new BigDecimal("4800.00"),
                new BigDecimal("1200.00"), new BigDecimal("1100.00"),
                new BigDecimal("76.0"), new BigDecimal("77.0")
        );
        when(dashboardService.getPulse(eq(USER_ID), anyInt(), anyInt())).thenReturn(pulseDto);

        mockMvc.perform(get("/api/v1/dashboard/pulse")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentIncome").value(5000.00))
                .andExpect(jsonPath("$.currentExpense").value(1200.00));

        verify(dashboardService).getPulse(eq(USER_ID), eq(1), eq(2025));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/trend/cashflow should return cash flow trend")
    void getCashFlow_shouldReturnCashFlowTrend() throws Exception {
        CashFlowTrendDto trendDto = new CashFlowTrendDto(1, 2025, new BigDecimal("1000.00"), new BigDecimal("800.00"));
        when(dashboardService.getCashFlowTrend(USER_ID)).thenReturn(List.of(trendDto));

        mockMvc.perform(get("/api/v1/dashboard/trend/cashflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].year").value(2025));

        verify(dashboardService).getCashFlowTrend(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/categories should return category breakdown")
    void getSpendingByCategory_shouldReturnCategoryBreakdown() throws Exception {
        CategoryDto catDto = new CategoryDto(1L, null, "Food", com.mayureshpatel.pfdataservice.domain.category.CategoryType.EXPENSE, null, "icon", "color");
        CategoryBreakdownDto breakdown = new CategoryBreakdownDto(catDto, new BigDecimal("450.00"));
        when(dashboardService.getCategoryBreakdown(eq(USER_ID), anyInt(), anyInt())).thenReturn(List.of(breakdown));

        mockMvc.perform(get("/api/v1/dashboard/categories")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.name").value("Food"))
                .andExpect(jsonPath("$[0].total").value(450.00));

        verify(dashboardService).getCategoryBreakdown(eq(USER_ID), eq(1), eq(2025));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/vendors should return merchant breakdown")
    void getSpendingByMerchant_shouldReturnMerchantBreakdown() throws Exception {
        MerchantDto merchDto = new MerchantDto(1L, null, "Amazon", "Amazon");
        MerchantBreakdownDto breakdown = new MerchantBreakdownDto(merchDto, new BigDecimal("300.00"));
        when(dashboardService.getMerchantBreakdown(eq(USER_ID), anyInt(), anyInt())).thenReturn(List.of(breakdown));

        mockMvc.perform(get("/api/v1/dashboard/vendors")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant.cleanName").value("Amazon"))
                .andExpect(jsonPath("$[0].total").value(300.00));

        verify(dashboardService).getMerchantBreakdown(eq(USER_ID), eq(1), eq(2025));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/actions should return list of action items")
    void getActionItems_shouldReturnActionItems() throws Exception {
        ActionItemDto item = new ActionItemDto(ActionItemDto.ActionType.UNCATEGORIZED, 5L, "Uncategorized expenses found", "/transactions?category=null");
        when(dashboardService.getActionItems(USER_ID)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/dashboard/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Uncategorized expenses found"));

        verify(dashboardService).getActionItems(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/ytd should return YTD summary")
    void getYtdSummary_shouldReturnYtdSummary() throws Exception {
        YtdSummaryDto summary = new YtdSummaryDto(2025, new BigDecimal("12000.00"), new BigDecimal("10000.00"), new BigDecimal("2000.00"), new BigDecimal("16.6"));
        when(dashboardService.getYtdSummary(eq(USER_ID), anyInt())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/ytd")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(12000.00));

        verify(dashboardService).getYtdSummary(eq(USER_ID), eq(2025));
    }
}
