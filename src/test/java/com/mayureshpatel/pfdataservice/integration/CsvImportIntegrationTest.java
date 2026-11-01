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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** An end-to-end integration test (real Spring context, real database via {@link BaseIntegrationTest}'s Testcontainers setup, real {@code MockMvc} HTTP calls) of the full CSV-import flow: upload a file to get previews, then submit the approved previews to be saved as real transactions -- verifying the two endpoints actually compose correctly together, not just each in isolation. */
@AutoConfigureMockMvc
class CsvImportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/accounts/100/upload")
                        .file(file)
                        .param("bankName", "STANDARD")
                        .with(request -> {
                            request.setMethod(HttpMethod.POST.name());
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Merchant"))
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));

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
                .andExpect(jsonPath("$[0].fileName").value("test-upload.csv"))
                .andExpect(jsonPath("$[0].status").value("IMPORTED"))
                .andExpect(jsonPath("$[0].savedCount").value(1));
    }

    /**
     * Proves each file in a bulk request commits in its own transaction: a file whose hash was
     * already imported is reported as skipped, and the fresh file after it is still persisted.
     * Unit tests mock the proxy, so only a real database run can catch an outer transaction
     * rolling everything back.
     */
    @Test
    @DisplayName("should persist a fresh file even when an earlier file in the same bulk request was already imported")
    @WithCustomMockUser(id = 101L)
    @Sql(statements = {
            "INSERT INTO users (id, username, email, password_hash, last_updated_by, created_at) VALUES (101, 'bulkuser', 'bulk@example.com', 'hash', 'system', NOW()) ON CONFLICT DO NOTHING;",
            "INSERT INTO accounts (id, user_id, name, type, current_balance, bank_name) VALUES (101, 101, 'Bulk Account', 'CHECKING', 1000.00, 'STANDARD') ON CONFLICT DO NOTHING;"
    })
    void shouldCommitFreshFileWhenEarlierFileAlreadyImported() throws Exception {
        // arrange
        SaveTransactionRequest firstFile = singleRowRequest("first.csv", "bulk-hash-1", "Bulk First Merchant");
        SaveTransactionRequest secondFile = singleRowRequest("second.csv", "bulk-hash-2", "Bulk Second Merchant");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(firstFile))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IMPORTED"));

        // act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(firstFile, secondFile))))
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("first.csv"))
                .andExpect(jsonPath("$[0].status").value("SKIPPED_ALREADY_IMPORTED"))
                .andExpect(jsonPath("$[1].fileName").value("second.csv"))
                .andExpect(jsonPath("$[1].status").value("IMPORTED"))
                .andExpect(jsonPath("$[1].savedCount").value(1));

        Integer secondFileRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = 101 AND description = 'Bulk Second Merchant'", Integer.class);
        assertEquals(1, secondFileRows);
    }

    private SaveTransactionRequest singleRowRequest(String fileName, String fileHash, String description) {
        TransactionDto dto = TransactionDto.builder()
                .date(OffsetDateTime.now())
                .description(description)
                .amount(new BigDecimal("12.00"))
                .type(TransactionType.EXPENSE)
                .build();
        return new SaveTransactionRequest(List.of(dto), fileName, fileHash, 101L);
    }
}
