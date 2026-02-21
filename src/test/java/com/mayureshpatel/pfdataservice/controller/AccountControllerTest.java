package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser
    void createAccount_ValidData_ShouldReturnCreated() throws Exception {
        // Given
        AccountDto createDto = new AccountDto(null, "Checking Account", "CHECKING", new BigDecimal("1000.00"), null);
        AccountDto responseDto = new AccountDto(1L, "Checking Account", "CHECKING", new BigDecimal("1000.00"), null);

        when(accountService.createAccount(eq(1L), any(AccountDto.class))).thenReturn(responseDto);

        // When/Then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Checking Account"))
                .andExpect(jsonPath("$.type").value("CHECKING"));
    }

    @Test
    @WithMockUser
    void getAllAccounts_ShouldReturnAccountList() throws Exception {
        // Given
        AccountDto accountDto = new AccountDto(1L, "Checking Account", "CHECKING", new BigDecimal("1000.00"), null);
        when(accountService.getAllAccountsByUserId(1L)).thenReturn(List.of(accountDto));

        // When/Then
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Checking Account"));
    }

    @Test
    @WithMockUser
    void updateAccount_ValidData_ShouldReturnUpdatedAccount() throws Exception {
        // Given
        AccountDto updateDto = new AccountDto(null, "Savings Account", "SAVINGS", new BigDecimal("2000.00"), null);
        AccountDto responseDto = new AccountDto(1L, "Savings Account", "SAVINGS", new BigDecimal("2000.00"), null);

        when(accountService.updateAccount(eq(1L), eq(1L), any(AccountDto.class))).thenReturn(responseDto);

        // When/Then
        mockMvc.perform(put("/api/v1/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Savings Account"))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }

    @Test
    @WithMockUser
    void deleteAccount_ValidId_ShouldReturnNoContent() throws Exception {
        // When/Then
        mockMvc.perform(delete("/api/v1/accounts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void reconcileAccount_ValidData_ShouldReturnUpdatedAccount() throws Exception {
        // Given
        AccountDto responseDto = new AccountDto(1L, "Checking Account", "CHECKING", new BigDecimal("1500.00"), null);
        when(accountService.reconcileAccount(eq(1L), eq(1L), any(BigDecimal.class))).thenReturn(responseDto);

        // When/Then
        mockMvc.perform(post("/api/v1/accounts/1/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1500.00));
    }
}
