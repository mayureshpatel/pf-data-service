//package com.mayureshpatel.pfdataservice.controller;
//
//import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
//import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
//import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
//import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@DisplayName("RecurringTransactionController Unit Tests")
//class RecurringTransactionControllerTest extends BaseControllerTest {
//
//    private static final long RECURRING_ID = 200L;
//
//    @Test
//    @WithCustomMockUser(id = USER_ID)
//    @DisplayName("GET /api/v1/recurring/suggestions should return suggestions")
//    void getSuggestions_shouldReturnSuggestions() throws Exception {
//        // Arrange
//        RecurringSuggestionDto suggestionDto = new RecurringSuggestionDto(null, new BigDecimal("15.99"), Frequency.MONTHLY, null, null, 5, 0.95);
//        when(recurringTransactionService.findSuggestions(USER_ID)).thenReturn(List.of(suggestionDto));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/v1/recurring/suggestions"))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$[0].amount").value(15.99));
//
//        verify(recurringTransactionService).findSuggestions(USER_ID);
//    }
//
//    @Test
//    @WithCustomMockUser(id = USER_ID)
//    @DisplayName("GET /api/v1/recurring should return list of recurring transactions")
//    void getRecurringTransactions_shouldReturnList() throws Exception {
//        // Arrange
//        RecurringTransactionDto recurringDto = new RecurringTransactionDto(RECURRING_ID, null, null, null, new BigDecimal("100.00"), Frequency.MONTHLY, LocalDate.now(), LocalDate.now().plusMonths(1), true);
//        when(recurringTransactionService.getRecurringTransactions(USER_ID)).thenReturn(List.of(recurringDto));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/v1/recurring"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].id").value(RECURRING_ID))
//                .andExpect(jsonPath("$[0].amount").value(100.00));
//
//        verify(recurringTransactionService).getRecurringTransactions(USER_ID);
//    }
//
//    @Test
//    @WithCustomMockUser(id = USER_ID)
//    @DisplayName("POST /api/v1/recurring should create recurring transaction")
//    void createRecurringTransaction_shouldCreate() throws Exception {
//        // Arrange
//        com.mayureshpatel.pfdataservice.dto.account.AccountDto account = new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank");
//        RecurringTransactionDto requestDto = new RecurringTransactionDto(null, null, account, null, new BigDecimal("50.00"), Frequency.WEEKLY, null, null, true);
//        RecurringTransactionDto responseDto = new RecurringTransactionDto(RECURRING_ID, null, account, null, new BigDecimal("50.00"), Frequency.WEEKLY, null, null, true);
//
//        when(recurringTransactionService.createRecurringTransaction(eq(USER_ID), any(RecurringTransactionDto.class))).thenReturn(responseDto);
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/recurring")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").value(RECURRING_ID))
//                .andExpect(jsonPath("$.amount").value(50.00));
//
//        verify(recurringTransactionService).createRecurringTransaction(eq(USER_ID), any(RecurringTransactionDto.class));
//    }
//
//    @Test
//    @WithCustomMockUser(id = USER_ID)
//    @DisplayName("PUT /api/v1/recurring/{id} should update recurring transaction")
//    void updateRecurringTransaction_shouldUpdate() throws Exception {
//        // Arrange
//        com.mayureshpatel.pfdataservice.dto.account.AccountDto account = new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank");
//        RecurringTransactionDto requestDto = new RecurringTransactionDto(RECURRING_ID, null, account, null, new BigDecimal("120.00"), Frequency.MONTHLY, null, null, true);
//
//        when(recurringTransactionService.updateRecurringTransaction(eq(USER_ID), eq(RECURRING_ID), any(RecurringTransactionDto.class))).thenReturn(requestDto);
//
//        // Act & Assert
//        mockMvc.perform(put("/api/v1/recurring/{id}", RECURRING_ID)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.amount").value(120.00));
//
//        verify(recurringTransactionService).updateRecurringTransaction(eq(USER_ID), eq(RECURRING_ID), any(RecurringTransactionDto.class));
//    }
//
//    @Test
//    @WithCustomMockUser(id = USER_ID)
//    @DisplayName("DELETE /api/v1/recurring/{id} should delete recurring transaction")
//    void deleteRecurringTransaction_shouldDelete() throws Exception {
//        // Act & Assert
//        mockMvc.perform(delete("/api/v1/recurring/{id}", RECURRING_ID)
//                        .with(csrf()))
//                .andExpect(status().isNoContent());
//
//        verify(recurringTransactionService).deleteRecurringTransaction(USER_ID, RECURRING_ID);
//    }
//
//    @Test
//    @DisplayName("GET /api/v1/recurring should return 401 when not authenticated")
//    void getRecurring_unauthenticated_returns401() throws Exception {
//        mockMvc.perform(get("/api/v1/recurring"))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/recurring should return 401 when not authenticated")
//    void createRecurring_unauthenticated_returns401() throws Exception {
//        mockMvc.perform(post("/api/v1/recurring")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}"))
//                .andExpect(status().isUnauthorized());
//    }
//}
