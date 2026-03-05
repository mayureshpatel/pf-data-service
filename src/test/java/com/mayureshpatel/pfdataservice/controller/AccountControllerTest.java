package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.bank.BankName;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.currency.CurrencyDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AccountController}.
 * This class serves as a gold standard for controller unit testing,
 * demonstrating best practices for MockMvc, security context, and validation testing.
 */
@DisplayName("AccountController Unit Tests")
@WithCustomMockUser()
class AccountControllerTest extends BaseControllerTest {

    private static final Long ACCOUNT_ID = 101L;

    @Nested
    @DisplayName("getAccounts")
    class GetAccountsTests {

        @ParameterizedTest
        @ValueSource(strings = {"/api/v1/accounts", "/api/accounts"})
        @DisplayName("GET should return list of accounts for both URL versions")
        void getAccounts_shouldReturnListOfAccounts(String url) throws Exception {
            // Arrange
            AccountDto accountDto = new AccountDto(
                    ACCOUNT_ID,
                    null,
                    "Checking Account",
                    AccountTypeDto.builder().code("CHECKING").build(),
                    new BigDecimal("1000.00"),
                    CurrencyDto.builder().code("USD").build(),
                    BankName.CAPITAL_ONE
            );

            when(accountService.getAllAccountsByUserId(USER_ID)).thenReturn(List.of(accountDto));

            // Act & Assert
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$[0].name").value("Checking Account"))
                    .andExpect(jsonPath("$[0].currentBalance").value(1000.00))
                    .andExpect(jsonPath("$[0].bank").value("CAPITAL_ONE"));

            verify(accountService).getAllAccountsByUserId(USER_ID);
        }

        @Test
        @DisplayName("GET should return empty list when no accounts exist")
        void getAccounts_shouldReturnEmptyList() throws Exception {
            // Arrange
            when(accountService.getAllAccountsByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(accountService).getAllAccountsByUserId(USER_ID);
        }

        @Test
        @DisplayName("GET should return 500 Internal Server Error when service throws an unhandled exception")
        void getAccounts_shouldReturn500WhenServiceFails() throws Exception {
            // Arrange
            when(accountService.getAllAccountsByUserId(USER_ID))
                    .thenThrow(new RuntimeException("Database connection failure"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/accounts"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.detail").value("An unexpected internal error occurred. Please contact support."))
                    .andExpect(jsonPath("$.instance").value("/api/v1/accounts"));
        }
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccountTests {

        @Test
        @DisplayName("POST should create a new account and return its ID")
        void createAccount_shouldCreateNewAccount() throws Exception {
            // Arrange
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .userId(USER_ID)
                    .name("Savings")
                    .type("SAVINGS")
                    .startingBalance(new BigDecimal("500.00"))
                    .currencyCode("USD")
                    .bankName("DISCOVER")
                    .build();

            when(accountService.createAccount(eq(USER_ID), any(AccountCreateRequest.class))).thenReturn(ACCOUNT_ID.intValue());

            // Act & Assert
            mockMvc.perform(post("/api/v1/accounts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(ACCOUNT_ID.toString()));

            verify(accountService).createAccount(eq(USER_ID), any(AccountCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createAccount_shouldReturn400WhenValidationFails() throws Exception {
            // Arrange - invalid request (blank name, invalid currency)
            AccountCreateRequest request = AccountCreateRequest.builder()
                    .userId(USER_ID)
                    .name("")
                    .type("SAVINGS")
                    .startingBalance(new BigDecimal("500.00"))
                    .currencyCode("INVALID")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/accounts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                    .andExpect(jsonPath("$.validationErrors", hasSize(2)))
                    .andExpect(jsonPath("$.validationErrors[*].field", containsInAnyOrder("name", "currencyCode")));
        }
    }

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccountTests {

        @Test
        @DisplayName("PUT should update the account and return 1")
        void updateAccount_shouldUpdateAccount() throws Exception {
            // Arrange
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_ID)
                    .name("Updated Savings")
                    .type("SAVINGS")
                    .currencyCode("USD")
                    .bankName("SYNOVUS")
                    .version(1L)
                    .build();

            when(accountService.updateAccount(eq(USER_ID), any(AccountUpdateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(put("/api/v1/accounts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(accountService).updateAccount(eq(USER_ID), any(AccountUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when ID is missing")
        void updateAccount_shouldReturn400WhenIdIsMissing() throws Exception {
            // Arrange
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .name("Updated Name")
                    .type("SAVINGS")
                    .currencyCode("USD")
                    .version(1L)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/v1/accounts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("id"));
        }

        @Test
        @DisplayName("PUT should return 403 Forbidden when user does not own the account")
        void updateAccount_shouldReturn403WhenAccessDenied() throws Exception {
            // Arrange
            AccountUpdateRequest request = AccountUpdateRequest.builder()
                    .id(ACCOUNT_ID)
                    .name("Unauthorized Update")
                    .type("SAVINGS")
                    .currencyCode("USD")
                    .version(1L)
                    .build();

            when(accountService.updateAccount(eq(USER_ID), any(AccountUpdateRequest.class)))
                    .thenThrow(new AccessDeniedException("You do not have permission to access this resource."));

            // Act & Assert
            mockMvc.perform(put("/api/v1/accounts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource."));
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {

        @Test
        @DisplayName("DELETE should remove the account and return 204 No Content")
        void deleteAccount_shouldDeleteAccount() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/accounts/{id}", ACCOUNT_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(accountService).deleteAccount(USER_ID, ACCOUNT_ID);
        }

        @Test
        @DisplayName("DELETE should return 404 Not Found when account does not exist")
        void deleteAccount_shouldReturn404WhenNotFound() throws Exception {
            // Arrange
            when(accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .thenThrow(new ResourceNotFoundException("Account with ID " + ACCOUNT_ID + " not found"));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/accounts/{id}", ACCOUNT_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.detail").value("Account with ID " + ACCOUNT_ID + " not found"));
        }
    }

    @Nested
    @DisplayName("reconcileAccount")
    class ReconcileAccountTests {

        @Test
        @DisplayName("POST /reconcile should update balance and return status")
        void reconcileAccount_shouldReconcile() throws Exception {
            // Arrange
            BigDecimal newBalance = new BigDecimal("1500.00");
            when(accountService.reconcileAccount(USER_ID, ACCOUNT_ID, newBalance)).thenReturn(1);

            // Act & Assert
            mockMvc.perform(post("/api/v1/accounts/{id}/reconcile", ACCOUNT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newBalance)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(accountService).reconcileAccount(USER_ID, ACCOUNT_ID, newBalance);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @WithCustomMockUser(id = 0)
        @DisplayName("Request as different user should use that user's ID")
        void requestWithDifferentUser_shouldUseCorrectId() throws Exception {
            mockMvc.perform(get("/api/v1/accounts"))
                    .andExpect(status().isOk());

            verify(accountService).getAllAccountsByUserId(0L);
        }
    }
}
