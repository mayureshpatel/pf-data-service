package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link BudgetController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("BudgetController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class BudgetControllerTest extends BaseControllerTest {

    private static final Long BUDGET_ID = 1L;
    private static final String BASE_URL = "/api/v1/budgets";
    private static final String PARAM_MONTH = "month";

    /** {@code GET /api/v1/budgets} returns the service's result for the given {@code month}/{@code year}, and defaults to the current month/year both when the params are simply omitted and when they're explicitly sent as the literal null. */
    @Nested
    @DisplayName("getBudgets")
    class GetBudgetsTests {

        @Test
        @DisplayName("GET should return list of budgets for specific month and year")
        void getBudgets_shouldReturnBudgets() throws Exception {
            // arrange
            int month = 3;
            int year = 2026;
            BudgetDto budgetDto = BudgetDto.builder()
                    .id(BUDGET_ID)
                    .amount(new BigDecimal("500.00"))
                    .month(month)
                    .year(year)
                    .build();

            when(budgetService.getBudgets(USER_ID, month, year)).thenReturn(List.of(budgetDto));

            // act & assert & verify
            mockMvc.perform(get(BASE_URL)
                            .param(PARAM_MONTH, String.valueOf(month))
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(BUDGET_ID));

            verify(budgetService).getBudgets(USER_ID, month, year);
        }

        @Test
        @DisplayName("GET should use current month and year when parameters are missing")
        void getBudgets_shouldUseDefaults() throws Exception {
            // arrange
            LocalDate now = LocalDate.now();
            when(budgetService.getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear())))
                    .thenReturn(List.of());

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());

            verify(budgetService).getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear()));
        }

        @Test
        @DisplayName("GET should use defaults when month and year are explicitly null")
        void getBudgets_shouldUseDefaultsWhenExplicitlyNull() throws Exception {
            // arrange
            LocalDate now = LocalDate.now();
            when(budgetService.getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear())))
                    .thenReturn(List.of());

            // act & assert & verify
            mockMvc.perform(get(BASE_URL)
                            .param(PARAM_MONTH, (String) null)
                            .param("year", (String) null))
                    .andExpect(status().isOk());

            verify(budgetService).getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear()));
        }
    }

    /** {@code GET /api/v1/budgets/status} returns the service's budgeted/spent/remaining breakdown for the given {@code month}/{@code year}, defaulting to the current month/year when omitted. */
    @Nested
    @DisplayName("getBudgetStatus")
    class GetBudgetStatusTests {

        @Test
        @DisplayName("GET /status should return budget status list")
        void getBudgetStatus_shouldReturnStatus() throws Exception {
            // arrange
            int month = 3;
            int year = 2026;
            BudgetStatusDto statusDto = BudgetStatusDto.builder()
                    .budgetedAmount(new BigDecimal("100.00"))
                    .spentAmount(new BigDecimal("40.00"))
                    .remainingAmount(new BigDecimal("60.00"))
                    .build();

            when(budgetService.getBudgetStatus(USER_ID, month, year)).thenReturn(List.of(statusDto));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/budgets/status")
                            .param(PARAM_MONTH, String.valueOf(month))
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].budgetedAmount").value(100.00));

            verify(budgetService).getBudgetStatus(USER_ID, month, year);
        }

        @Test
        @DisplayName("GET /status should use defaults when parameters are missing")
        void getBudgetStatus_shouldUseDefaults() throws Exception {
            // arrange
            LocalDate now = LocalDate.now();
            when(budgetService.getBudgetStatus(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear())))
                    .thenReturn(List.of());

            // act & assert & verify
            mockMvc.perform(get("/api/v1/budgets/status"))
                    .andExpect(status().isOk());

            verify(budgetService).getBudgetStatus(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear()));
        }
    }

    /** {@code GET /api/v1/budgets/all} returns a {@link Page} of budgets, and -- a PF-320 case -- genuinely honors the request's {@code page}/{@code size} query params, confirmed by inspecting the {@link Pageable} actually passed to the service. */
    @Nested
    @DisplayName("getAllBudgets")
    class GetAllBudgetsTests {

        @Test
        @DisplayName("GET /all should return a page of budgets for user")
        void getAllBudgets_shouldReturnPage() throws Exception {
            // arrange
            Page<BudgetDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(budgetService.getAllBudgets(eq(USER_ID), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/budgets/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));

            verify(budgetService).getAllBudgets(eq(USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("PF-320: GET /all should honor page/size query params")
        void getAllBudgets_shouldHonorPageParams() throws Exception {
            // arrange
            Page<BudgetDto> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);
            when(budgetService.getAllBudgets(eq(USER_ID), any(Pageable.class))).thenReturn(page);

            // act & assert & verify
            mockMvc.perform(get("/api/v1/budgets/all").param("page", "2").param("size", "5"))
                    .andExpect(status().isOk());

            verify(budgetService).getAllBudgets(eq(USER_ID), argThat(
                    (Pageable p) -> p.getPageNumber() == 2 && p.getPageSize() == 5));
        }
    }

    /** {@code POST /api/v1/budgets} creates the budget and returns its new id, and returns a 400 naming every violated field at once (a missing {@code categoryId} and an out-of-range {@code month} together) when the request fails validation. */
    @Nested
    @DisplayName("createBudget")
    class CreateBudgetTests {

        @Test
        @DisplayName("POST should create a new budget and return its ID")
        void createBudget_shouldReturnId() throws Exception {
            // arrange
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(USER_ID)
                    .categoryId(10L)
                    .amount(new BigDecimal("200.00"))
                    .month(3)
                    .year(2026)
                    .build();

            when(budgetService.create(eq(USER_ID), any(BudgetCreateRequest.class))).thenReturn(BUDGET_ID.intValue());

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(BUDGET_ID.toString()));

            verify(budgetService).create(eq(USER_ID), any(BudgetCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createBudget_shouldReturn400OnInvalidInput() throws Exception {
            // arrange - missing categoryId
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(USER_ID)
                    .amount(new BigDecimal("200.00"))
                    .month(13) // Invalid month
                    .year(2026)
                    .build();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("categoryId", PARAM_MONTH)));
        }
    }

    /** {@code PUT /api/v1/budgets} updates the budget and returns the affected-row count. */
    @Nested
    @DisplayName("updateBudget")
    class UpdateBudgetTests {

        @Test
        @DisplayName("PUT should update budget and return status")
        void updateBudget_shouldReturnStatus() throws Exception {
            // arrange
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .id(BUDGET_ID)
                    .userId(USER_ID)
                    .amount(new BigDecimal("300.00"))
                    .build();

            when(budgetService.update(eq(USER_ID), any(BudgetUpdateRequest.class))).thenReturn(1);

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(budgetService).update(eq(USER_ID), any(BudgetUpdateRequest.class));
        }
    }

    /** {@code DELETE /api/v1/budgets/{id}} returns 204 on success. */
    @Nested
    @DisplayName("deleteBudget")
    class DeleteBudgetTests {

        @Test
        @DisplayName("DELETE should remove budget and return 204 No Content")
        void deleteBudget_shouldReturnNoContent() throws Exception {
            // act & assert & verify
            mockMvc.perform(delete("/api/v1/budgets/{id}", BUDGET_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(budgetService).delete(USER_ID, BUDGET_ID);
        }
    }

    /** {@code DELETE} returns 404 when the service throws {@link ResourceNotFoundException}; an unexpected exception elsewhere surfaces as a generic 500 via {@code GlobalExceptionHandler}'s catch-all. */
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("DELETE should return 404 Not Found when budget does not exist")
        void deleteBudget_shouldReturn404() throws Exception {
            // arrange
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Budget not found"))
                    .when(budgetService).delete(USER_ID, BUDGET_ID);

            // act & assert & verify
            mockMvc.perform(delete("/api/v1/budgets/{id}", BUDGET_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET should return 500 when service fails")
        void getBudgets_shouldReturn500() throws Exception {
            // arrange
            when(budgetService.getBudgets(anyLong(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Server error"));

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isInternalServerError());
        }
    }
}
