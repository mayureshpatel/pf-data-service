package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionImportService transactionImportService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadTransactions_ShouldReturnPreview() throws Exception {
        Long accountId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", MediaType.TEXT_PLAIN_VALUE, "date,desc,amount".getBytes()
        );

        TransactionPreview preview = TransactionPreview.builder()
                .date(LocalDate.now())
                .description("Test Transaction")
                .amount(BigDecimal.TEN)
                .type(TransactionType.EXPENSE)
                .suggestedCategory("Groceries")
                .build();

        when(transactionImportService.previewTransactions(eq(accountId), eq("BANK"), any(), any()))
                .thenReturn(List.of(preview));

        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", accountId)
                        .file(file)
                        .param("bankName", "BANK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Transaction"))
                .andExpect(jsonPath("$[0].suggestedCategory").value("Groceries"));
    }

    @Test
    void uploadTransactions_ShouldReturnBadRequest_WhenFileIsEmpty() throws Exception {
        Long accountId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", MediaType.TEXT_PLAIN_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", accountId)
                        .file(file)
                        .param("bankName", "BANK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("File must not be empty"));
    }

    @Test
    void saveTransactions_ShouldReturnSuccessMessage() throws Exception {
        Long accountId = 1L;
        Transaction t = new Transaction();
        t.setDescription("Saved Txn");
        t.setAmount(BigDecimal.valueOf(100));

        SaveTransactionRequest request = new SaveTransactionRequest();
        request.setTransactions(List.of(t));
        request.setFileName("test.csv");
        request.setFileHash("hash123");

        when(transactionImportService.saveTransactions(eq(accountId), any(), eq("test.csv"), eq("hash123")))
                .thenReturn(1);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Successfully saved 1 transactions."));
    }
}