package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AccountTypeController Unit Tests")
class AccountTypeControllerTest extends BaseControllerTest {

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/account-types should return list of active account types")
    void getAccountTypes_shouldReturnListOfAccountTypes() throws Exception {
        // Arrange
        AccountType type = new AccountType("CHECKING", "Checking", null, true, 1, true, null);
        when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(type));

        // Act & Assert
        mockMvc.perform(get("/api/v1/account-types"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("CHECKING"))
                .andExpect(jsonPath("$[0].label").value("Checking"));

        verify(accountTypeRepository).findByIsActiveTrueOrderBySortOrder();
    }
}
