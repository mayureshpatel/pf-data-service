package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link BudgetController}.
 */
@WebMvcTest(BudgetController.class)
@DisplayName("BudgetController Unit Tests")
class BudgetControllerTest extends BaseControllerTest {

    private static final long BUDGET_ID = 10L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/budgets should return list of budgets for current month")
    void getBudgets_shouldReturnListOfBudgets() throws Exception {
        // Arrange
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        BudgetDto budgetDto = new BudgetDto(BUDGET_ID, null, null, new BigDecimal("500.00"), month, year);
        
        when(budgetService.getBudgets(USER_ID, month, year)).thenReturn(List.of(budgetDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(BUDGET_ID))
                .andExpect(jsonPath("$[0].amount").value(500.00));

        verify(budgetService).getBudgets(USER_ID, month, year);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/budgets should return list of budgets for specific month/year")
    void getBudgets_withParams_shouldReturnListOfBudgets() throws Exception {
        // Arrange
        int month = 1;
        int year = 2025;
        BudgetDto budgetDto = new BudgetDto(BUDGET_ID, null, null, new BigDecimal("500.00"), month, year);
        
        when(budgetService.getBudgets(USER_ID, month, year)).thenReturn(List.of(budgetDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets")
                        .param("month", String.valueOf(month))
                        .param("year", String.valueOf(year)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(month))
                .andExpect(jsonPath("$[0].year").value(year));

        verify(budgetService).getBudgets(USER_ID, month, year);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/budgets/status should return budget status")
    void getBudgetStatus_shouldReturnBudgetStatus() throws Exception {
        // Arrange
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        
        Category category = new Category();
        category.setId(100L);
        category.setName("Groceries");
        
        BudgetStatusDto statusDto = new BudgetStatusDto(com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper.toDto(category), new BigDecimal("500.00"), new BigDecimal("200.00"), new BigDecimal("300.00"), 60.0);
        
        when(budgetService.getBudgetStatus(USER_ID, month, year)).thenReturn(List.of(statusDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.name").value("Groceries"))
                .andExpect(jsonPath("$[0].budgetedAmount").value(500.00))
                .andExpect(jsonPath("$[0].spentAmount").value(200.00));

        verify(budgetService).getBudgetStatus(USER_ID, month, year);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/budgets/all should return all budgets")
    void getAllBudgets_shouldReturnAllBudgets() throws Exception {
        // Arrange
        BudgetDto budgetDto = new BudgetDto(BUDGET_ID, null, null, new BigDecimal("500.00"), 1, 2025);
        
        when(budgetService.getAllBudgets(USER_ID)).thenReturn(List.of(budgetDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BUDGET_ID));

        verify(budgetService).getAllBudgets(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/budgets should save budget")
    void setBudget_shouldSaveBudget() throws Exception {
        // Arrange
        BudgetDto requestDto = new BudgetDto(null, null, null, new BigDecimal("600.00"), 2, 2025);
        BudgetDto responseDto = new BudgetDto(BUDGET_ID, null, null, new BigDecimal("600.00"), 2, 2025);
        
        when(budgetService.save(eq(USER_ID), any(BudgetDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/budgets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BUDGET_ID))
                .andExpect(jsonPath("$.amount").value(600.00));

        verify(budgetService).save(eq(USER_ID), any(BudgetDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/budgets/{id} should delete budget")
    void deleteBudget_shouldDeleteBudget() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/budgets/{id}", BUDGET_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(budgetService).delete(USER_ID, BUDGET_ID);
    }

    @Test
    @DisplayName("GET /api/v1/budgets should return 401 when not authenticated")
    void getBudgets_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/budgets should return 401 when not authenticated")
    void setBudget_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/budgets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
