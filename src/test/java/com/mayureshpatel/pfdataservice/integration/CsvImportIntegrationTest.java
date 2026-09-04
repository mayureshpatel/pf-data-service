package com.mayureshpatel.pfdataservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CsvImportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should upload CSV and then save bulk transactions successfully")
    @WithCustomMockUser(id = 100L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (100, 'testuser', 'test@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (100, 100, 'Test Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;"
    })
    void shouldUploadAndSaveCsvTransactions() throws Exception {
        // 1. Upload CSV to get Previews
        String csvContent = "Date,Description,Amount\n2024-01-01T00:00:00Z,Test Merchant,-50.00\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-upload.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csvContent.getBytes()
        );

        String previewResponse = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/accounts/100/upload")
                        .file(file)
                        .param("bankName", "STANDARD")
                        .with(request -> {
                            request.setMethod(HttpMethod.POST.name());
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Merchant"))
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andReturn().getResponse().getContentAsString();

        // 2. Map preview to SaveTransactionRequest
        TransactionDto transactionDto = TransactionDto.builder()
                .date(OffsetDateTime.now())
                .description("Test Merchant")
                .amount(new BigDecimal("50.00")) // Save request expects positive amount
                .type(TransactionType.EXPENSE)
                .build();

        SaveTransactionRequest saveRequest = new SaveTransactionRequest(
                List.of(transactionDto),
                "test-upload.csv",
                "hash123",
                100L
        );

        // 3. Save Bulk Transactions
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(saveRequest))))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully saved 1 transactions."));
    }
}
