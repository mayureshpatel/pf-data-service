package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link RecurringTransactionController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("RecurringTransactionController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class RecurringTransactionControllerTest extends BaseControllerTest {

    private static final Long RECURRING_ID = 1L;

    @Nested
    @DisplayName("getSuggestions")
    class GetSuggestionsTests {

        @Test
        @DisplayName("GET /suggestions should return list of suggestions")
        void getSuggestions_shouldReturnList() throws Exception {
            // Arrange
            when(recurringTransactionService.findSuggestions(USER_ID)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/recurring/suggestions"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(recurringTransactionService).findSuggestions(USER_ID);
        }
    }

    @Nested
    @DisplayName("getRecurringTransactions")
    class GetRecurringTransactionsTests {

        @Test
        @DisplayName("GET should return list of recurring transactions")
        void getRecurringTransactions_shouldReturnList() throws Exception {
            // Arrange
            RecurringTransactionDto dto = RecurringTransactionDto.builder()
                    .id(RECURRING_ID)
                    .amount(new BigDecimal("50.00"))
                    .build();

            when(recurringTransactionService.getRecurringTransactions(USER_ID)).thenReturn(List.of(dto));

            // Act & Assert
            mockMvc.perform(get("/api/v1/recurring"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(RECURRING_ID));

            verify(recurringTransactionService).getRecurringTransactions(USER_ID);
        }
    }

    @Nested
    @DisplayName("createRecurringTransaction")
    class CreateRecurringTransactionTests {

        @Test
        @DisplayName("POST should create a new recurring transaction and return its ID")
        void createRecurringTransaction_shouldReturnId() throws Exception {
            // Arrange
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                    .userId(USER_ID)
                    .accountId(10L)
                    .amount(new BigDecimal("100.00"))
                    .frequency("MONTHLY")
                    .nextDate(LocalDate.now().plusDays(30))
                    .merchantId(20L)
                    .build();

            when(recurringTransactionService.createRecurringTransaction(eq(USER_ID), any(RecurringTransactionCreateRequest.class)))
                    .thenReturn(RECURRING_ID.intValue());

            // Act & Assert
            mockMvc.perform(post("/api/v1/recurring")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(RECURRING_ID.toString()));

            verify(recurringTransactionService).createRecurringTransaction(eq(USER_ID), any(RecurringTransactionCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createRecurringTransaction_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange - missing accountId, amount, frequency, nextDate, merchantId
            RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                    .userId(USER_ID)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/recurring")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", 
                            org.hamcrest.Matchers.containsInAnyOrder("accountId", "amount", "frequency", "nextDate", "merchantId")));
        }
    }

    @Nested
    @DisplayName("updateRecurringTransaction")
    class UpdateRecurringTransactionTests {

        @Test
        @DisplayName("PUT should update recurring transaction and return status")
        void updateRecurringTransaction_shouldReturnStatus() throws Exception {
            // Arrange
            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder()
                    .id(RECURRING_ID)
                    .accountId(10L)
                    .amount(new BigDecimal("150.00"))
                    .frequency("WEEKLY")
                    .nextDate(LocalDate.now().plusDays(7))
                    .merchantId(20L)
                    .build();

            when(recurringTransactionService.updateRecurringTransaction(eq(USER_ID), any(RecurringTransactionUpdateRequest.class)))
                    .thenReturn(1);

            // Act & Assert
            mockMvc.perform(put("/api/v1/recurring")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(recurringTransactionService).updateRecurringTransaction(eq(USER_ID), any(RecurringTransactionUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when validation fails")
        void updateRecurringTransaction_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange - missing ID and other fields
            RecurringTransactionUpdateRequest request = RecurringTransactionUpdateRequest.builder()
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/v1/recurring")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", 
                            org.hamcrest.Matchers.hasItems("id", "accountId", "amount", "frequency", "nextDate", "merchantId")));
        }
    }

    @Nested
    @DisplayName("deleteRecurringTransaction")
    class DeleteRecurringTransactionTests {

        @Test
        @DisplayName("DELETE should remove recurring transaction and return 204 No Content")
        void deleteRecurringTransaction_shouldReturnNoContent() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/recurring/{id}", RECURRING_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(recurringTransactionService).deleteRecurringTransaction(USER_ID, RECURRING_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("DELETE should return 404 Not Found when recurring transaction does not exist")
        void deleteRecurringTransaction_shouldReturn404() throws Exception {
            // Arrange
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Recurring transaction not found"))
                    .when(recurringTransactionService).deleteRecurringTransaction(USER_ID, RECURRING_ID);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/recurring/{id}", RECURRING_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET should return 500 when service fails unexpectedly")
        void getRecurringTransactions_shouldReturn500() throws Exception {
            // Arrange
            when(recurringTransactionService.getRecurringTransactions(anyLong()))
                    .thenThrow(new RuntimeException("Internal error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/recurring"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
