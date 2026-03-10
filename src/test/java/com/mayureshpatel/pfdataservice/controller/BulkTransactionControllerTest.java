package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        when(transactionImportService.saveBulkTransactions(eq(1L), any())).thenReturn(5);
        mockMvc.perform(post("/api/v1/transactions/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk());
    }
}
