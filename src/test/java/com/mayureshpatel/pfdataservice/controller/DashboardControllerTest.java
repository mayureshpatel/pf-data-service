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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest extends BaseControllerTest {

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/pulse should return dashboard pulse")
    void getPulse_shouldReturnDashboardPulse() throws Exception {
        DashboardPulseDto pulseDto = new DashboardPulseDto(new BigDecimal("5000.00"), new BigDecimal("1200.00"), new BigDecimal("45.5"), new BigDecimal("150.00"), List.of());
        when(dashboardService.getPulse(USER_ID)).thenReturn(pulseDto);

        mockMvc.perform(get("/api/v1/dashboard/pulse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth").value(5000.00))
                .andExpect(jsonPath("$.monthlySpending").value(1200.00));

        verify(dashboardService).getPulse(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/cash-flow should return cash flow trend")
    void getCashFlow_shouldReturnCashFlowTrend() throws Exception {
        CashFlowTrendDto trendDto = new CashFlowTrendDto(List.of("Jan", "Feb"), List.of(new BigDecimal("1000.00")), List.of(new BigDecimal("800.00")));
        when(dashboardService.getCashFlowTrend(eq(USER_ID), anyInt())).thenReturn(trendDto);

        mockMvc.perform(get("/api/v1/dashboard/cash-flow").param("months", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels[0]").value("Jan"));

        verify(dashboardService).getCashFlowTrend(eq(USER_ID), eq(6));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/spending/category should return category breakdown")
    void getSpendingByCategory_shouldReturnCategoryBreakdown() throws Exception {
        CategoryDto catDto = new CategoryDto(1L, null, "Food", "EXPENSE", "icon", "color", null);
        CategoryBreakdownDto breakdown = new CategoryBreakdownDto(catDto, new BigDecimal("450.00"), 35.0);
        when(dashboardService.getSpendingByCategory(eq(USER_ID), anyInt())).thenReturn(List.of(breakdown));

        mockMvc.perform(get("/api/v1/dashboard/spending/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.name").value("Food"))
                .andExpect(jsonPath("$[0].amount").value(450.00));

        verify(dashboardService).getSpendingByCategory(eq(USER_ID), eq(1));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/spending/merchant should return merchant breakdown")
    void getSpendingByMerchant_shouldReturnMerchantBreakdown() throws Exception {
        MerchantDto merchDto = new MerchantDto(1L, null, "Amazon", "Amazon");
        MerchantBreakdownDto breakdown = new MerchantBreakdownDto(merchDto, new BigDecimal("300.00"), 20.0);
        when(dashboardService.getSpendingByMerchant(eq(USER_ID), anyInt())).thenReturn(List.of(breakdown));

        mockMvc.perform(get("/api/v1/dashboard/spending/merchant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant.cleanName").value("Amazon"));

        verify(dashboardService).getSpendingByMerchant(eq(USER_ID), eq(1));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/action-items should return list of action items")
    void getActionItems_shouldReturnActionItems() throws Exception {
        ActionItemDto item = new ActionItemDto("High Spending", "Warning", "You spent more on dining this month", "warning");
        when(dashboardService.getActionItems(USER_ID)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/dashboard/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("High Spending"));

        verify(dashboardService).getActionItems(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/ytd-summary should return YTD summary")
    void getYtdSummary_shouldReturnYtdSummary() throws Exception {
        YtdSummaryDto summary = new YtdSummaryDto(new BigDecimal("12000.00"), new BigDecimal("10000.00"), new BigDecimal("2000.00"), 16.6);
        when(dashboardService.getYtdSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/ytd-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(12000.00));

        verify(dashboardService).getYtdSummary(USER_ID);
    }
}
