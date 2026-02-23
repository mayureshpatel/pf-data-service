package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountTypeController.class)
@DisplayName("AccountTypeController Unit Tests")
class AccountTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountTypeRepository accountTypeRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

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
