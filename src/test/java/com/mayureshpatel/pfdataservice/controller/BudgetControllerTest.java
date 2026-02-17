package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetService budgetService;

    @Test
    @WithMockUser
    void setBudget_ValidData_ShouldReturnOk() throws Exception {
        // Given
        BudgetDto budgetDto = new BudgetDto(null, 1L, null, new BigDecimal("500.00"), 1, 2024);
        BudgetDto responseDto = new BudgetDto(1L, 1L, "Groceries", new BigDecimal("500.00"), 1, 2024);

        when(budgetService.setBudget(eq(1L), any(BudgetDto.class))).thenReturn(responseDto);

        // When/Then
        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.year").value(2024));
    }

    @Test
    @WithMockUser
    void getBudgets_ShouldReturnBudgetList() throws Exception {
        // Given
        BudgetDto budgetDto = new BudgetDto(1L, 1L, "Groceries", new BigDecimal("500.00"), 1, 2024);
        when(budgetService.getBudgets(1L, 1, 2024)).thenReturn(List.of(budgetDto));

        // When/Then
        mockMvc.perform(get("/api/v1/budgets")
                        .param("month", "1")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(500.00));
    }

    @Test
    @WithMockUser
    void getAllBudgets_ShouldReturnAllBudgets() throws Exception {
        // Given
        BudgetDto budgetDto = new BudgetDto(1L, 1L, "Groceries", new BigDecimal("500.00"), 1, 2024);
        when(budgetService.getAllBudgets(1L)).thenReturn(List.of(budgetDto));

        // When/Then
        mockMvc.perform(get("/api/v1/budgets/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser
    void deleteBudget_ValidId_ShouldReturnNoContent() throws Exception {
        // When/Then
        mockMvc.perform(delete("/api/v1/budgets/1"))
                .andExpect(status().isNoContent());
    }
}
