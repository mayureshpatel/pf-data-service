package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportResultDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportStatus;
import com.mayureshpatel.pfdataservice.dto.transaction.ImportHashCheckRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.SaveTransactionRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionDto;
import com.mayureshpatel.pfdataservice.security.SecurityService;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.TransactionImportService;
import com.mayureshpatel.pfdataservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies {@code BulkTransactionController}'s single {@code POST /api/v1/transactions/bulk} endpoint delegates to {@code TransactionImportService#saveBulkTransactions} and returns its per-file results. */
@WebMvcTest(BulkTransactionController.class)
class BulkTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionImportService transactionImportService;

    @MockBean
    private SecurityService securityService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("should handle bulk save properly")
    @WithCustomMockUser(id = 1L)
    void shouldHandleBulkSave() throws Exception {
        // arrange
        when(transactionImportService.saveBulkTransactions(eq(1L), any())).thenReturn(List.of(
                new FileImportResultDto("a.csv", FileImportStatus.IMPORTED, 5, 2, null),
                new FileImportResultDto("b.csv", FileImportStatus.SKIPPED_ALREADY_IMPORTED, 0, 0, null),
                new FileImportResultDto("c.csv", FileImportStatus.FAILED, 0, 0, "boom")));

        // act
        mockMvc.perform(post("/api/v1/transactions/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("[]"))
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].fileName").value("a.csv"))
                .andExpect(jsonPath("$[0].status").value("IMPORTED"))
                .andExpect(jsonPath("$[0].savedCount").value(5))
                .andExpect(jsonPath("$[0].duplicateRowCount").value(2))
                .andExpect(jsonPath("$[0].error").value(nullValue()))
                .andExpect(jsonPath("$[1].status").value("SKIPPED_ALREADY_IMPORTED"))
                .andExpect(jsonPath("$[2].status").value("FAILED"))
                .andExpect(jsonPath("$[2].error").value("boom"));
    }

    @Test
    @DisplayName("should return the already-imported subset from import-check")
    @WithCustomMockUser(id = 1L)
    void shouldReturnAlreadyImportedHashes() throws Exception {
        // arrange
        when(transactionImportService.findAlreadyImported(eq(1L), any()))
                .thenReturn(List.of(new ImportHashCheckRequest(7L, "abc")));

        // act
        mockMvc.perform(post("/api/v1/transactions/import-check").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"accountId\":7,\"fileHash\":\"abc\"},{\"accountId\":7,\"fileHash\":\"def\"}]"))
                // assert & verify
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountId").value(7))
                .andExpect(jsonPath("$[0].fileHash").value("abc"));
    }

    @Test
    @DisplayName("should reject import-check items with a blank hash or null account")
    @WithCustomMockUser(id = 1L)
    void shouldRejectInvalidImportCheckItems() throws Exception {
        // act
        mockMvc.perform(post("/api/v1/transactions/import-check").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"accountId\":null,\"fileHash\":\" \"}]"))
                // assert & verify
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should reject import-check lists over 1000 items")
    @WithCustomMockUser(id = 1L)
    void shouldRejectOversizedImportCheckList() throws Exception {
        // arrange
        String body = "[" + String.join(",", java.util.Collections.nCopies(1001, "{\"accountId\":1,\"fileHash\":\"h\"}")) + "]";

        // act
        mockMvc.perform(post("/api/v1/transactions/import-check").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                // assert & verify
                .andExpect(status().isBadRequest());
    }
}
