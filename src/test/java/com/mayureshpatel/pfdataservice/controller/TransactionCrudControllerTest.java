package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.SecurityService;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import com.mayureshpatel.pfdataservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionCrudController.class)
@DisplayName("TransactionCrudController Unit Tests")
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
class TransactionCrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean(name = "ss")
    private SecurityService securityService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private static final long USER_ID = 1L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/transactions/suggestions/transfers should return suggestions")
    void getTransferSuggestions_shouldReturnSuggestions() throws Exception {
        // Arrange
        TransactionDto source = TransactionDto.builder().id(1L).amount(new BigDecimal("100.00")).build();
        TransactionDto target = TransactionDto.builder().id(2L).amount(new BigDecimal("100.00")).build();
        TransferSuggestionDto suggestion = new TransferSuggestionDto(source, target, 0.95);
        
        when(transactionService.findPotentialTransfers(USER_ID)).thenReturn(List.of(suggestion));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/suggestions/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceTransaction.id").value(1L))
                .andExpect(jsonPath("$[0].confidenceScore").value(0.95));

        verify(transactionService).findPotentialTransfers(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/transactions/mark-as-transfer should call service")
    void markAsTransfer_shouldCallService() throws Exception {
        // Arrange
        List<Long> ids = List.of(1L, 2L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/mark-as-transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk());

        verify(transactionService).markAsTransfer(USER_ID, ids);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/transactions should create transaction")
    void createTransaction_shouldCreateTransaction() throws Exception {
        // Arrange
        TransactionDto requestDto = TransactionDto.builder()
                .description("New Transaction")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.EXPENSE)
                .build();
        
        TransactionDto responseDto = TransactionDto.builder()
                .id(100L)
                .description("New Transaction")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.EXPENSE)
                .build();

        when(transactionService.createTransaction(eq(USER_ID), any(TransactionDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.description").value("New Transaction"));

        verify(transactionService).createTransaction(eq(USER_ID), any(TransactionDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("PUT /api/v1/transactions/{id} should update transaction")
    void updateTransaction_shouldUpdateTransaction() throws Exception {
        // Arrange
        Long transactionId = 100L;
        TransactionDto requestDto = TransactionDto.builder()
                .description("Updated Transaction")
                .amount(new BigDecimal("75.00"))
                .type(TransactionType.EXPENSE)
                .build();

        TransactionDto responseDto = TransactionDto.builder()
                .id(transactionId)
                .description("Updated Transaction")
                .amount(new BigDecimal("75.00"))
                .type(TransactionType.EXPENSE)
                .build();

        when(securityService.isTransactionOwner(eq(transactionId), any())).thenReturn(true);
        when(transactionService.updateTransaction(eq(USER_ID), eq(transactionId), any(TransactionDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/transactions/{id}", transactionId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Transaction"));

        verify(transactionService).updateTransaction(eq(USER_ID), eq(transactionId), any(TransactionDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/transactions/{id} should delete transaction")
    void deleteTransaction_shouldDeleteTransaction() throws Exception {
        // Arrange
        Long transactionId = 100L;
        when(securityService.isTransactionOwner(eq(transactionId), any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/{id}", transactionId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(USER_ID, transactionId);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/transactions/{id} should return 403 when not owner")
    void deleteTransaction_shouldReturn403WhenNotOwner() throws Exception {
        // Arrange
        Long transactionId = 100L;
        when(securityService.isTransactionOwner(eq(transactionId), any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/{id}", transactionId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("PATCH /api/v1/transactions/bulk should update transactions")
    void updateTransactionsBulk_shouldUpdateTransactions() throws Exception {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
                .id(1L)
                .description("Bulk Update")
                .build();
        List<TransactionDto> request = List.of(dto);
        
        when(transactionService.updateTransactions(eq(USER_ID), any())).thenReturn(request);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/transactions/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Bulk Update"));

        verify(transactionService).updateTransactions(eq(USER_ID), any());
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/transactions/bulk should delete transactions")
    void deleteTransactionsBulk_shouldDeleteTransactions() throws Exception {
        // Arrange
        List<Long> ids = List.of(1L, 2L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransactions(USER_ID, ids);
    }
}
