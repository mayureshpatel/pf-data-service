package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AccountController}.
 */
@DisplayName("AccountController Unit Tests")
class AccountControllerTest extends BaseControllerTest {

    private static final Long ACCOUNT_ID = 101L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/accounts should return list of accounts")
    void getAccounts_shouldReturnListOfAccounts() throws Exception {
        // Arrange
        AccountType type = new AccountType();
        type.setCode("CHECKING");
        AccountDto accountDto = new AccountDto(ACCOUNT_ID, null, "Checking Account", type.getCode(), "Label", new BigDecimal("1000.00"), "USD", "$", BankName.CAPITAL_ONE.name());
        when(accountService.getAllAccountsByUserId(USER_ID)).thenReturn(List.of(accountDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(ACCOUNT_ID))
                .andExpect(jsonPath("$[0].name").value("Checking Account"))
                .andExpect(jsonPath("$[0].currentBalance").value(1000.00));

        verify(accountService).getAllAccountsByUserId(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/accounts should create a new account")
    void createAccount_shouldCreateNewAccount() throws Exception {
        // Arrange
        AccountType type = new AccountType();
        type.setCode("CHECKING");
        AccountDto requestDto = new AccountDto(null, null, "New Account", type.getCode(), "Label", new BigDecimal("500.00"), "USD", "$", BankName.DISCOVER.name());
        AccountDto responseDto = new AccountDto(ACCOUNT_ID, null, "New Account", type.getCode(), "Label", new BigDecimal("500.00"), "USD", "$", BankName.DISCOVER.name());
        
        when(accountService.createAccount(eq(USER_ID), any(AccountDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.name").value("New Account"));

        verify(accountService).createAccount(eq(USER_ID), any(AccountDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("PUT /api/v1/accounts/{id} should update the account")
    void updateAccount_shouldUpdateAccount() throws Exception {
        // Arrange
        AccountType type = new AccountType();
        type.setCode("SAVINGS");
        AccountDto requestDto = new AccountDto(ACCOUNT_ID, null, "Updated Name", type.getCode(), "Label", new BigDecimal("1200.00"), "USD", "$", BankName.SYNOVUS.name());
        
        when(accountService.updateAccount(eq(USER_ID), eq(ACCOUNT_ID), any(AccountDto.class))).thenReturn(requestDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/accounts/{id}", ACCOUNT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        verify(accountService).updateAccount(eq(USER_ID), eq(ACCOUNT_ID), any(AccountDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/accounts/{id} should delete the account")
    void deleteAccount_shouldDeleteAccount() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/accounts/{id}", ACCOUNT_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(USER_ID, ACCOUNT_ID);
    }
}
