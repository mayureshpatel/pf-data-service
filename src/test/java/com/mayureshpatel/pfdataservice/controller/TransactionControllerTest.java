package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link TransactionController}.
 */
@EnableMethodSecurity
@DisplayName("TransactionController Unit Tests")
class TransactionControllerTest extends BaseControllerTest {

    private static final long ACCOUNT_ID = 101L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/accounts/{accountId}/upload should return list of transaction previews")
    void uploadTransactions_shouldReturnPreviews() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "date,amount,description\n2023-01-01,10.00,Test".getBytes());
        TransactionPreviewDto preview = TransactionPreviewDto.builder()
                .description("Test")
                .amount(new BigDecimal("10.00"))
                .build();
        
        when(securityService.isAccountOwner(eq(ACCOUNT_ID), any())).thenReturn(true);
        when(transactionImportService.previewTransactions(eq(USER_ID), eq(ACCOUNT_ID), eq("CAPITAL_ONE"), any(InputStream.class), eq("test.csv")))
                .thenReturn(List.of(preview));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", ACCOUNT_ID)
                        .file(file)
                        .param("bankName", "CAPITAL_ONE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].description").value("Test"));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/accounts/{accountId}/transactions should save transactions and return success message")
    void saveTransactions_shouldReturnSuccessMessage() throws Exception {
        // Arrange
        com.mayureshpatel.pfdataservice.dto.account.AccountDto account = new com.mayureshpatel.pfdataservice.dto.account.AccountDto(1L, 1L, "Checking", "C", "Checking", java.math.BigDecimal.ZERO, "USD", "$", "Bank");
        TransactionDto tx = TransactionDto.builder().id(1L).date(java.time.OffsetDateTime.now()).amount(java.math.BigDecimal.TEN).type(com.mayureshpatel.pfdataservice.domain.transaction.TransactionType.EXPENSE).account(account).description("Test").build();
        SaveTransactionRequest request = new SaveTransactionRequest(List.of(tx), "test.csv", "hash123");

        when(securityService.isAccountOwner(eq(ACCOUNT_ID), any())).thenReturn(true);
        when(transactionImportService.saveTransactions(eq(USER_ID), eq(ACCOUNT_ID), anyList(), eq("test.csv"), eq("hash123")))
                .thenReturn(5);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", ACCOUNT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully saved 5 transactions."));
    }
    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/accounts/{accountId}/upload should return 403 when not owner")
    void uploadTransactions_shouldReturn403WhenNotOwner() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "date,amount,description".getBytes());
        when(securityService.isAccountOwner(eq(ACCOUNT_ID), any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", ACCOUNT_ID)
                        .file(file)
                        .param("bankName", "CAPITAL_ONE")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
