package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionPreviewDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link TransactionController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("TransactionController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class TransactionControllerTest extends BaseControllerTest {

    private static final Long ACCOUNT_ID = 10L;

    @Nested
    @DisplayName("uploadTransactions")
    class UploadTransactionsTests {

        @Test
        @DisplayName("POST /upload should return transaction previews on successful upload")
        void uploadTransactions_shouldReturnPreviews() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.csv", MediaType.TEXT_PLAIN_VALUE, "date,description,amount\n2026-03-04,Coffee,5.00".getBytes());
            String bankName = "CAPITAL_ONE";
            TransactionPreviewDto previewDto = TransactionPreviewDto.builder()
                    .description("Coffee")
                    .amount(new BigDecimal("5.00"))
                    .build();

            when(transactionImportService.previewTransactions(eq(USER_ID), eq(ACCOUNT_ID), eq(bankName), any(InputStream.class), eq("test.csv")))
                    .thenReturn(List.of(previewDto));

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", ACCOUNT_ID)
                            .file(file)
                            .param("bankName", bankName)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].description").value("Coffee"));

            verify(transactionImportService).previewTransactions(eq(USER_ID), eq(ACCOUNT_ID), eq(bankName), any(InputStream.class), eq("test.csv"));
        }

        @Test
        @DisplayName("POST /upload should return 400 Bad Request when file is empty")
        void uploadTransactions_shouldReturn400WhenFileEmpty() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.csv", MediaType.TEXT_PLAIN_VALUE, new byte[0]);
            
            // Act & Assert
            mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", ACCOUNT_ID)
                            .file(file)
                            .param("bankName", "CAPITAL_ONE")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("File must not be empty"));
        }

        @Test
        @DisplayName("POST /upload should return 403 Forbidden when user is not account owner")
        @WithCustomMockUser(id = 999L)
        void uploadTransactions_shouldReturn403WhenNotOwner() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile("file", "test.csv", MediaType.TEXT_PLAIN_VALUE, "test".getBytes());
            when(securityService.isAccountOwner(eq(ACCOUNT_ID), any())).thenReturn(false);

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", ACCOUNT_ID)
                            .file(file)
                            .param("bankName", "CAPITAL_ONE")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("saveTransactions")
    class SaveTransactionsTests {

        @Test
        @DisplayName("POST /transactions should save transactions and return success message")
        void saveTransactions_shouldReturnSuccessMessage() throws Exception {
            // Arrange
            TransactionDto transaction = TransactionDto.builder()
                    .description("Test")
                    .amount(new BigDecimal("10.00"))
                    .build();
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(transaction), "test.csv", "hash123", 10L);

            when(transactionImportService.saveTransactions(eq(USER_ID), eq(ACCOUNT_ID), anyList(), eq("test.csv"), eq("hash123")))
                    .thenReturn(1);

            // Act & Assert
            mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", ACCOUNT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Successfully saved 1 transactions."));

            verify(transactionImportService).saveTransactions(eq(USER_ID), eq(ACCOUNT_ID), anyList(), eq("test.csv"), eq("hash123"));
        }

        @Test
        @DisplayName("POST /transactions should return 400 Bad Request when validation fails")
        void saveTransactions_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange - empty transactions list
            SaveTransactionRequest request = new SaveTransactionRequest(Collections.emptyList(), "", "", 10L);

            // Act & Assert
            mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", ACCOUNT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", 
                            org.hamcrest.Matchers.hasItems("transactions", "fileName", "fileHash")));
        }

        @Test
        @DisplayName("POST /transactions should return 403 Forbidden when user is not account owner")
        @WithCustomMockUser(id = 999L)
        void saveTransactions_shouldReturn403WhenNotOwner() throws Exception {
            // Arrange
            SaveTransactionRequest request = new SaveTransactionRequest(List.of(TransactionDto.builder().build()), "test.csv", "hash", 10L);
            when(securityService.isAccountOwner(eq(ACCOUNT_ID), any())).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", ACCOUNT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("POST /upload should return 500 when service fails unexpectedly")
        void uploadTransactions_shouldReturn500OnServiceError() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile("file", "test.csv", MediaType.TEXT_PLAIN_VALUE, "test".getBytes());
            when(transactionImportService.previewTransactions(anyLong(), anyLong(), anyString(), any(InputStream.class), anyString()))
                    .thenThrow(new RuntimeException("Import failed"));

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", ACCOUNT_ID)
                            .file(file)
                            .param("bankName", "CAPITAL_ONE")
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }
    }
}
