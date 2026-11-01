package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportResultDto;
import com.mayureshpatel.pfdataservice.dto.transaction.FileImportStatus;
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

        // act & assert & verify
        mockMvc.perform(post("/api/v1/transactions/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("[]"))
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
}
