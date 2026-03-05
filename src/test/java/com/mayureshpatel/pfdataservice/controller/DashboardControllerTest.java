package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.dashboard.DashboardPulseDto;
import com.mayureshpatel.pfdataservice.dto.dashboard.YtdSummaryDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link DashboardController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("DashboardController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class DashboardControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("getCategoryBreakdown")
    class GetCategoryBreakdownTests {

        @Test
        @DisplayName("GET /categories should return breakdown by month and year")
        void getCategoryBreakdown_shouldReturnByMonthAndYear() throws Exception {
            // Arrange
            int month = 3;
            int year = 2026;
            when(dashboardService.getCategoryBreakdown(USER_ID, month, year)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/categories")
                            .param("month", String.valueOf(month))
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(dashboardService).getCategoryBreakdown(USER_ID, month, year);
        }

        @Test
        @DisplayName("GET /categories should return breakdown by date range")
        void getCategoryBreakdown_shouldReturnByDateRange() throws Exception {
            // Arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);
            when(dashboardService.getCategoryBreakdown(eq(USER_ID), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/categories")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk());

            verify(dashboardService).getCategoryBreakdown(eq(USER_ID), any(OffsetDateTime.class), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("GET /categories should use defaults if only startDate is provided")
        void getCategoryBreakdown_shouldUseDefaultsIfPartialDates() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getCategoryBreakdown(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/categories")
                            .param("startDate", "2026-01-01"))
                    .andExpect(status().isOk());

            verify(dashboardService).getCategoryBreakdown(USER_ID, now.getMonthValue(), now.getYear());
        }

        @Test
        @DisplayName("GET /categories should use defaults if only endDate is provided")
        void getCategoryBreakdown_shouldUseDefaultsIfOnlyEndDate() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getCategoryBreakdown(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/categories")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk());

            verify(dashboardService).getCategoryBreakdown(USER_ID, now.getMonthValue(), now.getYear());
        }
    }

    @Nested
    @DisplayName("getMerchantBreakdown")
    class GetMerchantBreakdownTests {

        @Test
        @DisplayName("GET /merchants should return breakdown by month and year")
        void getMerchantBreakdown_shouldReturnByMonthAndYear() throws Exception {
            // Arrange
            int month = 3;
            int year = 2026;
            when(dashboardService.getMerchantBreakdown(USER_ID, month, year)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/merchants")
                            .param("month", String.valueOf(month))
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk());

            verify(dashboardService).getMerchantBreakdown(USER_ID, month, year);
        }

        @Test
        @DisplayName("GET /merchants should return breakdown by date range")
        void getMerchantBreakdown_shouldReturnByDateRange() throws Exception {
            // Arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);
            when(dashboardService.getMerchantBreakdown(eq(USER_ID), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/merchants")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk());

            verify(dashboardService).getMerchantBreakdown(eq(USER_ID), any(OffsetDateTime.class), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("GET /merchants should use defaults if only endDate is provided")
        void getMerchantBreakdown_shouldUseDefaultsIfPartialDates() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getMerchantBreakdown(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/merchants")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk());

            verify(dashboardService).getMerchantBreakdown(USER_ID, now.getMonthValue(), now.getYear());
        }

        @Test
        @DisplayName("GET /merchants should use current month/year if no params provided")
        void getMerchantBreakdown_shouldUseDefaults() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getMerchantBreakdown(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/merchants"))
                    .andExpect(status().isOk());

            verify(dashboardService).getMerchantBreakdown(USER_ID, now.getMonthValue(), now.getYear());
        }

        @Test
        @DisplayName("GET /merchants should use defaults if only startDate is provided")
        void getMerchantBreakdown_shouldUseDefaultsIfOnlyStartDate() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getMerchantBreakdown(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/merchants")
                            .param("startDate", "2026-01-01"))
                    .andExpect(status().isOk());

            verify(dashboardService).getMerchantBreakdown(USER_ID, now.getMonthValue(), now.getYear());
        }
    }

    @Nested
    @DisplayName("getPulse")
    class GetPulseTests {

        @Test
        @DisplayName("GET /pulse should return pulse by month and year")
        void getPulse_shouldReturnByMonthAndYear() throws Exception {
            // Arrange
            int month = 3;
            int year = 2026;
            when(dashboardService.getPulse(USER_ID, month, year)).thenReturn(DashboardPulseDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/pulse")
                            .param("month", String.valueOf(month))
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk());

            verify(dashboardService).getPulse(USER_ID, month, year);
        }

        @Test
        @DisplayName("GET /pulse should return pulse by date range")
        void getPulse_shouldReturnByDateRange() throws Exception {
            // Arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);
            when(dashboardService.getPulse(eq(USER_ID), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .thenReturn(DashboardPulseDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/pulse")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk());

            verify(dashboardService).getPulse(eq(USER_ID), any(OffsetDateTime.class), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("GET /pulse should use defaults if partial dates are provided")
        void getPulse_shouldUseDefaultsIfPartialDates() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getPulse(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(DashboardPulseDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/pulse")
                            .param("startDate", "2026-01-01"))
                    .andExpect(status().isOk());

            verify(dashboardService).getPulse(USER_ID, now.getMonthValue(), now.getYear());
        }

        @Test
        @DisplayName("GET /pulse should use defaults if only endDate is provided")
        void getPulse_shouldUseDefaultsIfOnlyEndDate() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getPulse(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(DashboardPulseDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/pulse")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk());

            verify(dashboardService).getPulse(USER_ID, now.getMonthValue(), now.getYear());
        }

        @Test
        @DisplayName("GET /pulse should use current month/year if no params provided")
        void getPulse_shouldUseDefaults() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(dashboardService.getPulse(USER_ID, now.getMonthValue(), now.getYear()))
                    .thenReturn(DashboardPulseDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/pulse"))
                    .andExpect(status().isOk());

            verify(dashboardService).getPulse(USER_ID, now.getMonthValue(), now.getYear());
        }
    }

    @Nested
    @DisplayName("getCashFlowTrend")
    class GetCashFlowTrendTests {

        @Test
        @DisplayName("GET /trend/cashflow should return cash flow trend list")
        void getCashFlowTrend_shouldReturnList() throws Exception {
            // Arrange
            when(dashboardService.getCashFlowTrend(USER_ID)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/trend/cashflow"))
                    .andExpect(status().isOk());

            verify(dashboardService).getCashFlowTrend(USER_ID);
        }
    }

    @Nested
    @DisplayName("getYtdSummary")
    class GetYtdSummaryTests {

        @Test
        @DisplayName("GET /ytd should return summary for specific year")
        void getYtdSummary_shouldReturnForYear() throws Exception {
            // Arrange
            int year = 2025;
            when(dashboardService.getYtdSummary(USER_ID, year)).thenReturn(YtdSummaryDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/ytd")
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk());

            verify(dashboardService).getYtdSummary(USER_ID, year);
        }

        @Test
        @DisplayName("GET /ytd should use current year if param is missing")
        void getYtdSummary_shouldUseDefaultYear() throws Exception {
            // Arrange
            int currentYear = LocalDate.now().getYear();
            when(dashboardService.getYtdSummary(USER_ID, currentYear)).thenReturn(YtdSummaryDto.builder().build());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/ytd"))
                    .andExpect(status().isOk());

            verify(dashboardService).getYtdSummary(USER_ID, currentYear);
        }
    }

    @Nested
    @DisplayName("getActionItems")
    class GetActionItemsTests {

        @Test
        @DisplayName("GET /actions should return action items list")
        void getActionItems_shouldReturnList() throws Exception {
            // Arrange
            when(dashboardService.getActionItems(USER_ID)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/actions"))
                    .andExpect(status().isOk());

            verify(dashboardService).getActionItems(USER_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("GET should return 500 when dashboard service fails unexpectedly")
        void getPulse_shouldReturn500WhenServiceFails() throws Exception {
            // Arrange
            when(dashboardService.getPulse(anyLong(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Computation error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/dashboard/pulse"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"));
        }
    }
}
