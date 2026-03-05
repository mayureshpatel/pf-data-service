package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AccountTypeController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("AccountTypeController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class AccountTypeControllerTest extends BaseControllerTest {

    private static final String TYPE_CODE = "CHECKING";

    @Nested
    @DisplayName("getAccountTypes")
    class GetAccountTypesTests {

        @ParameterizedTest
        @ValueSource(strings = {"/api/v1/account-types", "/api/account-types"})
        @DisplayName("GET should return list of active account types for both URL versions")
        void getAccountTypes_shouldReturnListOfAccountTypes(String url) throws Exception {
            // Arrange
            AccountType type = AccountType.builder()
                    .code(TYPE_CODE)
                    .label("Checking Account")
                    .active(true)
                    .sortOrder(1)
                    .icon("account_balance")
                    .color("#4CAF50")
                    .asset(true)
                    .build();

            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(type));

            // Act & Assert
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].code").value(TYPE_CODE))
                    .andExpect(jsonPath("$[0].label").value("Checking Account"))
                    .andExpect(jsonPath("$[0].isActive").value(true))
                    .andExpect(jsonPath("$[0].sortOrder").value(1));

            verify(accountTypeRepository).findByIsActiveTrueOrderBySortOrder();
        }

        @Test
        @DisplayName("GET should return empty list when no active account types exist")
        void getAccountTypes_shouldReturnEmptyList() throws Exception {
            // Arrange
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/account-types"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(accountTypeRepository).findByIsActiveTrueOrderBySortOrder();
        }
    }

    @Nested
    @DisplayName("createAccountType")
    class CreateAccountTypeTests {

        @Test
        @DisplayName("POST should create a new account type and return rows affected")
        void createAccountType_shouldCreateNewAccountType() throws Exception {
            // Arrange
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .code("SAVINGS")
                    .label("Savings")
                    .icon("savings")
                    .color("#2196F3")
                    .isAsset(true)
                    .sortOrder(2)
                    .isActive(true)
                    .build();

            when(accountTypeRepository.insert(any(AccountTypeCreateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(post("/api/v1/account-types")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(accountTypeRepository).insert(any(AccountTypeCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails (missing code)")
        void createAccountType_shouldReturn400WhenValidationFails() throws Exception {
            AccountTypeCreateRequest request = AccountTypeCreateRequest.builder()
                    .label("Missing Code")
                    .isAsset(true)
                    .sortOrder(0)
                    .isActive(true)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/account-types")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("code"));
        }
    }

    @Nested
    @DisplayName("deleteAccountType")
    class DeleteAccountTypeTests {

        @Test
        @DisplayName("DELETE should remove the account type and return rows affected")
        void deleteAccountType_shouldDeleteAccountType() throws Exception {
            // Arrange
            when(accountTypeRepository.deleteByCode(TYPE_CODE)).thenReturn(1);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/account-types/{code}", TYPE_CODE)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(accountTypeRepository).deleteByCode(TYPE_CODE);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("GET should return 500 Internal Server Error when repository fails")
        void getAccountTypes_shouldReturn500WhenRepositoryFails() throws Exception {
            // Arrange
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder())
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/account-types"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.detail").value("An unexpected internal error occurred. Please contact support."));
        }
    }
}
