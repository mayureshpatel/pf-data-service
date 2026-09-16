package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.report.CategoryReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MerchantReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.MonthlyReportDataDto;
import com.mayureshpatel.pfdataservice.dto.report.NetWorthDataPointDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ReportController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("ReportController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class ReportControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("getNetWorth")
    class GetNetWorthTests {

        @Test
        @DisplayName("GET /net-worth should return the series for an explicit date range")
        void getNetWorth_shouldReturnSeriesForExplicitRange() throws Exception {
            // arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 3, 1);
            NetWorthDataPointDto point = new NetWorthDataPointDto(LocalDate.of(2026, 1, 31), new BigDecimal("1000.00"));
            when(reportService.getNetWorthOverTime(USER_ID, start, end)).thenReturn(List.of(point));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/reports/net-worth")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].date").value("2026-01-31"))
                    .andExpect(jsonPath("$[0].netWorth").value(1000.00));

            verify(reportService).getNetWorthOverTime(USER_ID, start, end);
        }

        @Test
        @DisplayName("GET /net-worth should default to the trailing 12 months when no range is given")
        void getNetWorth_shouldDefaultToTrailing12Months() throws Exception {
            // arrange
            LocalDate today = LocalDate.now();
            LocalDate expectedStart = today.minusMonths(11).withDayOfMonth(1);
            when(reportService.getNetWorthOverTime(eq(USER_ID), eq(expectedStart), eq(today))).thenReturn(List.of());

            // act & assert & verify
            mockMvc.perform(get("/api/v1/reports/net-worth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(reportService).getNetWorthOverTime(USER_ID, expectedStart, today);
        }
    }

    @Nested
    @DisplayName("getCategoryReportData")
    class GetCategoryReportDataTests {

        @Test
        @DisplayName("GET /categories should return the breakdown for the requested range")
        void getCategoryReportData_shouldReturnBreakdownForRange() throws Exception {
            // arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 3, 1);
            CategoryReportDataDto row = new CategoryReportDataDto(null, new BigDecimal("250.00"), 5L);
            when(reportService.getCategoryReportData(USER_ID, start, end)).thenReturn(List.of(row));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/reports/categories")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].total").value(250.00))
                    .andExpect(jsonPath("$[0].count").value(5));

            verify(reportService).getCategoryReportData(USER_ID, start, end);
        }

        @Test
        @DisplayName("GET /categories without startDate/endDate should fail with a 400, not a silent default")
        void getCategoryReportData_shouldRequireExplicitRange() throws Exception {
            // act & assert & verify -- unlike net-worth, Reports always has an active range client-side
            mockMvc.perform(get("/api/v1/reports/categories"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("getMerchantReportData")
    class GetMerchantReportDataTests {

        @Test
        @DisplayName("GET /merchants should return the breakdown for the requested range")
        void getMerchantReportData_shouldReturnBreakdownForRange() throws Exception {
            // arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 3, 1);
            MerchantReportDataDto row = new MerchantReportDataDto(1L, "Whole Foods", new BigDecimal("75.00"), 2L, List.of("Groceries"));
            when(reportService.getMerchantReportData(USER_ID, start, end)).thenReturn(List.of(row));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/reports/merchants")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].total").value(75.00))
                    .andExpect(jsonPath("$[0].categories[0]").value("Groceries"));

            verify(reportService).getMerchantReportData(USER_ID, start, end);
        }
    }

    @Nested
    @DisplayName("getMonthlyReportData")
    class GetMonthlyReportDataTests {

        @Test
        @DisplayName("GET /monthly should return the breakdown for the requested range")
        void getMonthlyReportData_shouldReturnBreakdownForRange() throws Exception {
            // arrange
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 3, 1);
            MonthlyReportDataDto row = new MonthlyReportDataDto(2026, 1, new BigDecimal("3000.00"), new BigDecimal("1800.00"));
            when(reportService.getMonthlyReportData(USER_ID, start, end)).thenReturn(List.of(row));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/reports/monthly")
                            .param("startDate", start.toString())
                            .param("endDate", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].income").value(3000.00))
                    .andExpect(jsonPath("$[0].expense").value(1800.00));

            verify(reportService).getMonthlyReportData(USER_ID, start, end);
        }
    }
}
