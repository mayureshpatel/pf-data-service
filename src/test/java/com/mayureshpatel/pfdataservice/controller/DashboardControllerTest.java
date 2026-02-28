package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.ActionItemDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.CashFlowTrendDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardPulseDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.YtdSummaryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.DashboardService;
import com.mayureshpatel.pfdataservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private static final long USER_ID = 1L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/categories should return breakdown")
    void getCategoryBreakdown_shouldReturnBreakdown() throws Exception {
        // Arrange
        CategoryDto category = new CategoryDto(1L, null, "Food", null, null, "icon", "color");
        CategoryBreakdownDto dto = new CategoryBreakdownDto(category, new BigDecimal("100.00"));
        
        when(dashboardService.getCategoryBreakdown(eq(USER_ID), anyInt(), anyInt())).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard/categories")
                        .param("month", "1")
                        .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.name").value("Food"));

        verify(dashboardService).getCategoryBreakdown(eq(USER_ID), eq(1), eq(2023));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/vendors should return breakdown")
    void getVendorBreakdown_shouldReturnBreakdown() throws Exception {
        // Arrange
        MerchantDto merchant = new MerchantDto(1L, null, "Target", "Target");
        MerchantBreakdownDto dto = new MerchantBreakdownDto(merchant, new BigDecimal("50.00"));
        
        when(dashboardService.getMerchantBreakdown(eq(USER_ID), anyInt(), anyInt())).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard/vendors")
                        .param("month", "1")
                        .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant.cleanName").value("Target"));

        verify(dashboardService).getMerchantBreakdown(eq(USER_ID), eq(1), eq(2023));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/pulse should return pulse data")
    void getPulse_shouldReturnPulse() throws Exception {
        // Arrange
        DashboardPulseDto dto = new DashboardPulseDto(
            new BigDecimal("5000.00"), 
            BigDecimal.ZERO, 
            new BigDecimal("3000.00"), 
            BigDecimal.ZERO, 
            new BigDecimal("40.0"), 
            BigDecimal.ZERO
        );
        
        when(dashboardService.getPulse(eq(USER_ID), anyInt(), anyInt())).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard/pulse")
                        .param("month", "1")
                        .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentIncome").value(5000.00));
        
        verify(dashboardService).getPulse(eq(USER_ID), eq(1), eq(2023));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/trend/cashflow should return trend")
    void getCashFlowTrend_shouldReturnTrend() throws Exception {
        // Arrange
        CashFlowTrendDto dto = new CashFlowTrendDto(1, 2023, new BigDecimal("5000.00"), new BigDecimal("3000.00"));
        
        when(dashboardService.getCashFlowTrend(USER_ID)).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard/trend/cashflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(1));

        verify(dashboardService).getCashFlowTrend(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/ytd should return summary")
    void getYtdSummary_shouldReturnSummary() throws Exception {
        // Arrange
        YtdSummaryDto dto = new YtdSummaryDto(
            2023, 
            new BigDecimal("60000.00"), 
            new BigDecimal("36000.00"), 
            new BigDecimal("24000.00"), 
            new BigDecimal("40.0")
        );
        
        when(dashboardService.getYtdSummary(eq(USER_ID), anyInt())).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard/ytd")
                        .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(60000.00));

        verify(dashboardService).getYtdSummary(eq(USER_ID), eq(2023));
    }
    
    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/dashboard/actions should return action items")
    void getActionItems_shouldReturnActionItems() throws Exception {
        // Arrange
        ActionItemDto dto = new ActionItemDto(
            ActionItemDto.ActionType.UNCATEGORIZED,
            5L,
            "You have 5 uncategorized transactions",
            "/transactions?category=uncategorized"
        );
        
        when(dashboardService.getActionItems(USER_ID)).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("UNCATEGORIZED"));

        verify(dashboardService).getActionItems(USER_ID);
    }
}
