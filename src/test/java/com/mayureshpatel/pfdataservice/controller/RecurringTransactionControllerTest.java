package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.service.RecurringTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecurringTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecurringTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecurringTransactionService recurringService;

    @Test
    @WithCustomMockUser
    void getSuggestions_ShouldReturnList() throws Exception {
        RecurringSuggestionDto suggestion = RecurringSuggestionDto.builder()
                .merchantName("Netflix")
                .amount(new BigDecimal("15.99"))
                .frequency(Frequency.MONTHLY)
                .confidenceScore(0.9)
                .build();
        when(recurringService.findSuggestions(1L)).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/v1/recurring/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantName").value("Netflix"));
    }

    @Test
    @WithCustomMockUser
    void getRecurringTransactions_ShouldReturnList() throws Exception {
        RecurringTransactionDto dto = RecurringTransactionDto.builder()
                .id(1L)
                .merchantName("Netflix")
                .amount(new BigDecimal("15.99"))
                .frequency(Frequency.MONTHLY)
                .active(true)
                .build();
        when(recurringService.getRecurringTransactions(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/recurring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantName").value("Netflix"));
    }

    @Test
    @WithCustomMockUser
    void createRecurringTransaction_ShouldReturnCreated() throws Exception {
        RecurringTransactionDto dto = RecurringTransactionDto.builder()
                .merchantName("New")
                .amount(BigDecimal.TEN)
                .frequency(Frequency.MONTHLY)
                .lastDate(LocalDate.now())
                .nextDate(LocalDate.now().plusMonths(1))
                .build();
        RecurringTransactionDto response = RecurringTransactionDto.builder()
                .id(10L)
                .merchantName("New")
                .amount(BigDecimal.TEN)
                .build();
        
        when(recurringService.createRecurringTransaction(eq(1L), any(RecurringTransactionDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithCustomMockUser
    void updateRecurringTransaction_ShouldReturnUpdated() throws Exception {
        RecurringTransactionDto dto = RecurringTransactionDto.builder()
                .merchantName("Updated")
                .amount(BigDecimal.TEN)
                .frequency(Frequency.MONTHLY)
                .lastDate(LocalDate.now())
                .nextDate(LocalDate.now().plusMonths(1))
                .build();
        RecurringTransactionDto response = RecurringTransactionDto.builder()
                .id(1L)
                .merchantName("Updated")
                .amount(BigDecimal.TEN)
                .build();
        
        when(recurringService.updateRecurringTransaction(eq(1L), eq(1L), any(RecurringTransactionDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/recurring/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantName").value("Updated"));
    }

    @Test
    @WithCustomMockUser
    void deleteRecurringTransaction_ShouldReturnNoContent() throws Exception {
        doNothing().when(recurringService).deleteRecurringTransaction(1L, 1L);

        mockMvc.perform(delete("/api/v1/recurring/1"))
                .andExpect(status().isNoContent());
    }
}
