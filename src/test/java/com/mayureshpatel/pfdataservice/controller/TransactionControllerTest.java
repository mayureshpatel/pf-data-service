package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.TransactionDto;
import com.mayureshpatel.pfdataservice.dto.TransactionPreview;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.SecurityService;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionImportService transactionImportService;

    @MockitoBean(name = "ss")
    private SecurityService securityService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }
    }

    @Test
    @WithCustomMockUser
    void uploadTransactions_ShouldReturnPreview() throws Exception {
        Long accountId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());

        when(securityService.isAccountOwner(eq(accountId), any())).thenReturn(true);
        when(transactionImportService.previewTransactions(any(), eq(accountId), eq("BANK"), any(), any()))
                .thenReturn(Collections.singletonList(
                        TransactionPreview.builder().description("Test").build()
                ));

        mockMvc.perform(multipart("/api/v1/accounts/{accountId}/upload", accountId)
                        .file(file)
                        .param("bankName", "BANK"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomMockUser
    void saveTransactions_ShouldReturnSuccess() throws Exception {
        Long accountId = 1L;
        TransactionDto dto = TransactionDto.builder()
                .date(LocalDate.now())
                .amount(BigDecimal.TEN)
                .type(TransactionType.EXPENSE)
                .description("Test")
                .build();
        
        SaveTransactionRequest request = new SaveTransactionRequest(
                List.of(dto),
                "file.csv",
                "hash123"
        );

        when(securityService.isAccountOwner(eq(accountId), any())).thenReturn(true);
        when(transactionImportService.saveTransactions(any(), eq(accountId), any(), any(), any())).thenReturn(1);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        verify(transactionImportService).saveTransactions(any(), eq(accountId), any(), any(), any());
    }
}
