package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TransactionController.class, com.mayureshpatel.pfdataservice.security.JwtService.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionImportService transactionImportService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithCustomMockUser(id = 10L)
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

        // Match against InputStream now
        when(transactionImportService.previewTransactions(eq(10L), eq(accountId), eq("BANK"), any(InputStream.class), any()))
                .thenReturn(List.of(preview));

        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", accountId)
                        .file(file)
                        .param("bankName", "BANK")
                        .with(csrf())) 
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Transaction"))
                .andExpect(jsonPath("$[0].suggestedCategory").value("Groceries"));
    }

    @Test
    @WithCustomMockUser
    void uploadTransactions_ShouldReturnBadRequest_WhenFileIsEmpty() throws Exception {
        Long accountId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", MediaType.TEXT_PLAIN_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", accountId)
                        .file(file)
                        .param("bankName", "BANK")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("File must not be empty"));
    }

    @Test
    @WithCustomMockUser(id = 10L)
    void saveTransactions_ShouldReturnSuccessMessage() throws Exception {
        TransactionDto dto = TransactionDto.builder()
                .date(LocalDate.now())
                .description("Test")
                .amount(BigDecimal.TEN)
                .type(TransactionType.EXPENSE)
                .build();

        SaveTransactionRequest request = new SaveTransactionRequest(List.of(dto), "test.csv", "hash123");

        when(transactionImportService.saveTransactions(anyLong(), anyLong(), anyList(), anyString(), anyString()))
                .thenReturn(1);

        mockMvc.perform(post("/api/v1/accounts/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully saved 1 transactions."));
    }
}