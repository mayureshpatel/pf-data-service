package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.DashboardData;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @WithCustomMockUser(id = 10L)
    void getDashboardData_ShouldReturnDataForAuthenticatedUser() throws Exception {
        DashboardData data = DashboardData.builder()
                .totalIncome(BigDecimal.valueOf(5000))
                .totalExpense(BigDecimal.valueOf(2000))
                .netSavings(BigDecimal.valueOf(3000))
                .categoryBreakdown(Collections.emptyList())
                .build();

        when(transactionService.getDashboardData(eq(10L), eq(1), eq(2026)))
                .thenReturn(data);

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("month", "1")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000));
    }

    @Test
    void getDashboardData_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .param("month", "1")
                        .param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }
}
