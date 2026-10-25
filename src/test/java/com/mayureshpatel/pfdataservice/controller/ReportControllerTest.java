package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreCompare;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreFilter;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreMeasure;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreResultDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreRowDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreSide;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTransactionDto;
import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreTrendDto;
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
import static org.mockito.ArgumentMatchers.any;
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

    private static final String PARAM_START_DATE = "startDate";
    private static final String PARAM_END_DATE = "endDate";
    private static final String EXPLORE_URL = "/api/v1/reports/explore";
    private static final String PARAM_MEASURE = "measure";
    private static final String MEASURE_SPENDING = "SPENDING";
    private static final String PARAM_GROUP_BY = "groupBy";
    private static final String JAN_1 = "2026-01-01";
    private static final String JAN_31 = "2026-01-31";

    /** {@code GET /api/v1/reports/net-worth} returns the service's series for an explicit {@code startDate}/{@code endDate}, and -- unlike the other report endpoints below -- defaults to the trailing 12 months (from the 1st of that starting month through today) when no range is given at all. */
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
                            .param(PARAM_START_DATE, start.toString())
                            .param(PARAM_END_DATE, end.toString()))
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

    /** {@code GET /api/v1/reports/categories} returns the service's breakdown for an explicit range, and -- unlike {@link GetNetWorthTests}'s net-worth endpoint -- requires {@code startDate}/{@code endDate} explicitly, failing with a 400 rather than silently defaulting one, since the Reports page always has an active range client-side. */
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
                            .param(PARAM_START_DATE, start.toString())
                            .param(PARAM_END_DATE, end.toString()))
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

    /** {@code GET /api/v1/reports/merchants} returns the service's breakdown for the requested range, including each merchant's associated category names. */
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
                            .param(PARAM_START_DATE, start.toString())
                            .param(PARAM_END_DATE, end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].total").value(75.00))
                    .andExpect(jsonPath("$[0].categories[0]").value("Groceries"));

            verify(reportService).getMerchantReportData(USER_ID, start, end);
        }
    }

    /** {@code GET /api/v1/reports/monthly} returns the service's income/expense breakdown for the requested range. */
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
                            .param(PARAM_START_DATE, start.toString())
                            .param(PARAM_END_DATE, end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].income").value(3000.00))
                    .andExpect(jsonPath("$[0].expense").value(1800.00));

            verify(reportService).getMonthlyReportData(USER_ID, start, end);
        }
    }

    /** {@code GET /api/v1/reports/explore} binds every parameter, defaults side to spending and compare to none, and rejects a measure/grouping pair that makes no sense. */
    @Nested
    @DisplayName("explore")
    class ExploreTests {

        private ExploreResultDto result() {
            return new ExploreResultDto(
                    List.of(new ExploreRowDto("5", "Food", new BigDecimal("100.00"), new BigDecimal("80.00"), 2L)),
                    new BigDecimal("100.00"), new BigDecimal("80.00"), 2L, 38L,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "same days last year (Jan 1 to Jan 31, 2025)", false);
        }

        @Test
        @DisplayName("GET /explore should bind every parameter and return the result")
        void explore_shouldBindEveryParameter() throws Exception {
            // arrange
            ExploreFilter filter = new ExploreFilter(List.of(1L, 2L), 7L, false, null, true, 3L, false, new BigDecimal("5"), new BigDecimal("500"));
            when(exploreService.explore(eq(USER_ID), eq(ExploreMeasure.COUNT), eq(ExploreSide.INCOME), eq(ExploreGroupBy.MERCHANT),
                    eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)), eq(ExploreCompare.SAME_DAYS_LAST_YEAR), eq(filter)))
                    .thenReturn(result());

            // act & assert & verify
            mockMvc.perform(get(EXPLORE_URL)
                            .param(PARAM_MEASURE, "COUNT").param("side", "INCOME").param(PARAM_GROUP_BY, "MERCHANT")
                            .param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31)
                            .param("compare", "SAME_DAYS_LAST_YEAR")
                            .param("accountIds", "1", "2").param("categoryId", "7").param("noMerchant", "true")
                            .param("tagId", "3").param("minAmount", "5").param("maxAmount", "500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows", hasSize(1)))
                    .andExpect(jsonPath("$.rows[0].label").value("Food"))
                    .andExpect(jsonPath("$.rows[0].comparisonValue").value(80.00))
                    .andExpect(jsonPath("$.excludedCount").value(38))
                    .andExpect(jsonPath("$.comparisonLabel").value("same days last year (Jan 1 to Jan 31, 2025)"));
        }

        @Test
        @DisplayName("GET /explore should default side to SPENDING and compare to NONE")
        void explore_shouldApplyDefaults() throws Exception {
            // arrange
            when(exploreService.explore(eq(USER_ID), eq(ExploreMeasure.SPENDING), eq(ExploreSide.SPENDING), eq(ExploreGroupBy.CATEGORY),
                    any(LocalDate.class), any(LocalDate.class), eq(ExploreCompare.NONE), eq(ExploreFilter.none())))
                    .thenReturn(result());

            // act & assert & verify
            mockMvc.perform(get(EXPLORE_URL)
                            .param(PARAM_MEASURE, MEASURE_SPENDING).param(PARAM_GROUP_BY, "CATEGORY")
                            .param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /explore should return 400 for an invalid measure/grouping pair")
        void explore_shouldReturn400ForInvalidPair() throws Exception {
            // arrange
            when(exploreService.explore(any(), eq(ExploreMeasure.NET), any(), eq(ExploreGroupBy.CATEGORY), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Net only groups by time"));

            // act & assert & verify
            mockMvc.perform(get(EXPLORE_URL)
                            .param(PARAM_MEASURE, "NET").param(PARAM_GROUP_BY, "CATEGORY")
                            .param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /explore should return 400 for an unknown grouping")
        void explore_shouldReturn400ForUnknownGrouping() throws Exception {
            // act & assert & verify
            mockMvc.perform(get(EXPLORE_URL)
                            .param(PARAM_MEASURE, MEASURE_SPENDING).param(PARAM_GROUP_BY, "NOPE")
                            .param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31))
                    .andExpect(status().isBadRequest());
        }
    }

    /** The trend and transaction-list endpoints take the explorer's filters, and the list is paged. */
    @Nested
    @DisplayName("explore trend and transactions")
    class ExploreTrendAndTransactionsTests {

        private static final String TREND_URL = "/api/v1/reports/explore/trend";
        private static final String TRANSACTIONS_URL = "/api/v1/reports/explore/transactions";

        @Test
        @DisplayName("GET /explore/trend should return current and comparison buckets without needing a grouping")
        void trend_shouldReturnBuckets() throws Exception {
            // arrange
            ExploreTrendDto trend = new ExploreTrendDto(
                    List.of(new ExploreTrendDto.Bucket(LocalDate.of(2026, 1, 1), new BigDecimal("100.00"), true)),
                    List.of(new ExploreTrendDto.Bucket(LocalDate.of(2025, 1, 1), new BigDecimal("80.00"), false)));
            when(exploreService.trend(eq(USER_ID), eq(ExploreMeasure.SPENDING), eq(ExploreSide.SPENDING),
                    any(LocalDate.class), any(LocalDate.class), eq(ExploreCompare.SAME_DAYS_LAST_YEAR), eq(ExploreFilter.none())))
                    .thenReturn(trend);

            // act & assert & verify
            mockMvc.perform(get(TREND_URL)
                            .param(PARAM_MEASURE, MEASURE_SPENDING).param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31)
                            .param("compare", "SAME_DAYS_LAST_YEAR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.current[0].start").value("2026-01-01"))
                    .andExpect(jsonPath("$.current[0].partial").value(true))
                    .andExpect(jsonPath("$.comparison[0].value").value(80.00));
        }

        @Test
        @DisplayName("GET /explore/transactions should return a page of the transactions behind the number")
        void transactions_shouldReturnAPage() throws Exception {
            // arrange
            ExploreTransactionDto row = new ExploreTransactionDto(9L, LocalDate.of(2026, 1, 5), "WHOLEFDS", "Whole Foods", "Checking",
                    "Groceries", new BigDecimal("84.12"), com.mayureshpatel.pfdataservice.domain.transaction.TransactionType.EXPENSE);
            when(exploreService.transactions(eq(USER_ID), eq(ExploreMeasure.SPENDING), eq(ExploreSide.SPENDING),
                    any(LocalDate.class), any(LocalDate.class), any(ExploreFilter.class), eq(1), eq(20)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(row),
                            org.springframework.data.domain.PageRequest.of(1, 20), 21));

            // act & assert & verify
            mockMvc.perform(get(TRANSACTIONS_URL)
                            .param(PARAM_MEASURE, MEASURE_SPENDING).param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31)
                            .param("categoryId", "7").param("page", "1").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].merchantName").value("Whole Foods"))
                    .andExpect(jsonPath("$.content[0].amount").value(84.12));
        }

        @Test
        @DisplayName("GET /explore should require a grouping")
        void explore_shouldRequireAGrouping() throws Exception {
            // act & assert & verify
            mockMvc.perform(get(EXPLORE_URL).param(PARAM_MEASURE, MEASURE_SPENDING).param(PARAM_START_DATE, JAN_1).param(PARAM_END_DATE, JAN_31))
                    .andExpect(status().isBadRequest());
        }
    }
}
