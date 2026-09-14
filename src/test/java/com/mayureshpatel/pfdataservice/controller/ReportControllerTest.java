package com.mayureshpatel.pfdataservice.controller;

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
}
