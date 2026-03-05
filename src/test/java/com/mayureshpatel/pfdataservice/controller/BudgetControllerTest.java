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

    @Nested
    @DisplayName("getBudgets")
    class GetBudgetsTests {

        @Test
        @DisplayName("GET should return list of budgets for specific month and year")
        void getBudgets_shouldReturnBudgets() throws Exception {
            // Arrange
            int month = 3;
            int year = 2026;
            BudgetDto budgetDto = BudgetDto.builder()
                    .id(BUDGET_ID)
                    .amount(new BigDecimal("500.00"))
                    .month(month)
                    .year(year)
                    .build();

            when(budgetService.getBudgets(USER_ID, month, year)).thenReturn(List.of(budgetDto));

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets")
                            .param("month", String.valueOf(month))
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
            // Arrange
            LocalDate now = LocalDate.now();
            when(budgetService.getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear())))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets"))
                    .andExpect(status().isOk());

            verify(budgetService).getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear()));
        }

        @Test
        @DisplayName("GET should use defaults when month and year are explicitly null")
        void getBudgets_shouldUseDefaultsWhenExplicitlyNull() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(budgetService.getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear())))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets")
                            .param("month", (String) null)
                            .param("year", (String) null))
                    .andExpect(status().isOk());

            verify(budgetService).getBudgets(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear()));
        }
    }

    @Nested
    @DisplayName("getBudgetStatus")
    class GetBudgetStatusTests {

        @Test
        @DisplayName("GET /status should return budget status list")
        void getBudgetStatus_shouldReturnStatus() throws Exception {
            // Arrange
            int month = 3;
            int year = 2026;
            BudgetStatusDto statusDto = BudgetStatusDto.builder()
                    .budgetedAmount(new BigDecimal("100.00"))
                    .spentAmount(new BigDecimal("40.00"))
                    .remainingAmount(new BigDecimal("60.00"))
                    .build();

            when(budgetService.getBudgetStatus(USER_ID, month, year)).thenReturn(List.of(statusDto));

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets/status")
                            .param("month", String.valueOf(month))
                            .param("year", String.valueOf(year)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].budgetedAmount").value(100.00));

            verify(budgetService).getBudgetStatus(USER_ID, month, year);
        }

        @Test
        @DisplayName("GET /status should use defaults when parameters are missing")
        void getBudgetStatus_shouldUseDefaults() throws Exception {
            // Arrange
            LocalDate now = LocalDate.now();
            when(budgetService.getBudgetStatus(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear())))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets/status"))
                    .andExpect(status().isOk());

            verify(budgetService).getBudgetStatus(eq(USER_ID), eq(now.getMonthValue()), eq(now.getYear()));
        }
    }

    @Nested
    @DisplayName("getAllBudgets")
    class GetAllBudgetsTests {

        @Test
        @DisplayName("GET /all should return all budgets for user")
        void getAllBudgets_shouldReturnAll() throws Exception {
            // Arrange
            when(budgetService.getAllBudgets(USER_ID)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets/all"))
                    .andExpect(status().isOk());

            verify(budgetService).getAllBudgets(USER_ID);
        }
    }

    @Nested
    @DisplayName("createBudget")
    class CreateBudgetTests {

        @Test
        @DisplayName("POST should create a new budget and return its ID")
        void createBudget_shouldReturnId() throws Exception {
            // Arrange
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(USER_ID)
                    .categoryId(10L)
                    .amount(new BigDecimal("200.00"))
                    .month(3)
                    .year(2026)
                    .build();

            when(budgetService.create(eq(USER_ID), any(BudgetCreateRequest.class))).thenReturn(BUDGET_ID.intValue());

            // Act & Assert
            mockMvc.perform(post("/api/v1/budgets")
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
            // Arrange - missing categoryId
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(USER_ID)
                    .amount(new BigDecimal("200.00"))
                    .month(13) // Invalid month
                    .year(2026)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/budgets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("categoryId", "month")));
        }
    }

    @Nested
    @DisplayName("updateBudget")
    class UpdateBudgetTests {

        @Test
        @DisplayName("PUT should update budget and return status")
        void updateBudget_shouldReturnStatus() throws Exception {
            // Arrange
            BudgetUpdateRequest request = BudgetUpdateRequest.builder()
                    .id(BUDGET_ID)
                    .userId(USER_ID)
                    .amount(new BigDecimal("300.00"))
                    .build();

            when(budgetService.update(eq(USER_ID), any(BudgetUpdateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(put("/api/v1/budgets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(budgetService).update(eq(USER_ID), any(BudgetUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("deleteBudget")
    class DeleteBudgetTests {

        @Test
        @DisplayName("DELETE should remove budget and return 204 No Content")
        void deleteBudget_shouldReturnNoContent() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/budgets/{id}", BUDGET_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(budgetService).delete(USER_ID, BUDGET_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("DELETE should return 404 Not Found when budget does not exist")
        void deleteBudget_shouldReturn404() throws Exception {
            // Arrange
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Budget not found"))
                    .when(budgetService).delete(USER_ID, BUDGET_ID);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/budgets/{id}", BUDGET_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET should return 500 when service fails")
        void getBudgets_shouldReturn500() throws Exception {
            // Arrange
            when(budgetService.getBudgets(anyLong(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Server error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/budgets"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
