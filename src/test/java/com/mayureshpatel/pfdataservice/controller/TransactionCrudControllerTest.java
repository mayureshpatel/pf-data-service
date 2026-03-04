package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.transaction.CategoryTransactionsDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
import com.mayureshpatel.pfdataservice.repository.transaction.specification.TransactionSpecification.TransactionFilter;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TransactionCrudController Unit Tests")
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
class TransactionCrudControllerTest extends BaseControllerTest {

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
        com.mayureshpatel.pfdataservice.dto.account.AccountDto account = new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank");
        TransactionDto requestDto = TransactionDto.builder()
                .description("New Transaction")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.EXPENSE)
                .date(java.time.OffsetDateTime.now())
                .account(account)
                .build();

        TransactionDto responseDto = TransactionDto.builder()
                .id(100L)
                .description("New Transaction")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.EXPENSE)
                .date(java.time.OffsetDateTime.now())
                .account(account)
                .build();

        when(transactionService.createTransaction(eq(USER_ID), any(TransactionDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
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
        com.mayureshpatel.pfdataservice.dto.account.AccountDto account = new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank");
        TransactionDto requestDto = TransactionDto.builder()
                .description("Updated Transaction")
                .amount(new BigDecimal("75.00"))
                .type(TransactionType.EXPENSE)
                .date(java.time.OffsetDateTime.now())
                .account(account)
                .build();

        TransactionDto responseDto = TransactionDto.builder()
                .id(transactionId)
                .description("Updated Transaction")
                .amount(new BigDecimal("75.00"))
                .type(TransactionType.EXPENSE)
                .date(java.time.OffsetDateTime.now())
                .account(account)
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
        com.mayureshpatel.pfdataservice.dto.account.AccountDto account = new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank");
        TransactionDto dto = TransactionDto.builder()
                .id(1L)
                .description("Bulk Update")
                .amount(new BigDecimal("75.00"))
                .type(TransactionType.EXPENSE)
                .date(java.time.OffsetDateTime.now())
                .account(account)
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

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/transactions should return paginated transactions")
    void getTransactions_shouldReturnPaginatedTransactions() throws Exception {
        TransactionDto dto = TransactionDto.builder().id(1L).description("Test").build();
        Page<TransactionDto> page = new PageImpl<>(List.of(dto));

        when(transactionService.getTransactions(eq(USER_ID), any(TransactionFilter.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].description").value("Test"));

        verify(transactionService).getTransactions(eq(USER_ID), any(TransactionFilter.class), any(Pageable.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/transactions with filter params should pass filter to service")
    void getTransactions_withFilters_shouldPassFilterToService() throws Exception {
        Page<TransactionDto> page = new PageImpl<>(Collections.emptyList());
        when(transactionService.getTransactions(eq(USER_ID), any(TransactionFilter.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .param("accountId", "5")
                        .param("type", "EXPENSE")
                        .param("description", "coffee")
                        .param("minAmount", "10.00")
                        .param("maxAmount", "100.00"))
                .andExpect(status().isOk());

        verify(transactionService).getTransactions(eq(USER_ID), any(TransactionFilter.class), any(Pageable.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/transactions/count-by-category should return category counts")
    void getCountByCategory_shouldReturnCounts() throws Exception {
        CategoryDto cat = new CategoryDto(1L, USER_ID, "Groceries", CategoryType.EXPENSE, null, "cart", "#00FF00");
        CategoryTransactionsDto dto = new CategoryTransactionsDto(cat, 15);

        when(transactionService.getCountByCategory(USER_ID)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/transactions/count-by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.name").value("Groceries"))
                .andExpect(jsonPath("$[0].transactionCount").value(15));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/transactions/existing-categories should return categories")
    void getExistingCategories_shouldReturnCategories() throws Exception {
        CategoryDto cat = new CategoryDto(1L, USER_ID, "Groceries", CategoryType.EXPENSE, null, "cart", "#00FF00");

        when(transactionService.getCategoriesWithTransactions(USER_ID)).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/transactions/existing-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Groceries"));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/transactions/existing-merchants should return merchants")
    void getExistingMerchants_shouldReturnMerchants() throws Exception {
        MerchantDto merchant = new MerchantDto(1L, USER_ID, "KROGER #431", "Kroger");

        when(transactionService.getMerchantsWithTransactions(USER_ID)).thenReturn(List.of(merchant));

        mockMvc.perform(get("/api/v1/transactions/existing-merchants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cleanName").value("Kroger"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions should return 401 when not authenticated")
    void getTransactions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
