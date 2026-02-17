package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.account.AccountTypeLookup;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountTypeRepository accountTypeRepository;

    @Test
    @WithMockUser
    void getAccountTypes_ShouldReturnActiveAccountTypes() throws Exception {
        // Given
        AccountTypeLookup type1 = new AccountTypeLookup();
        type1.setCode("CHECKING");
        type1.setLabel("Checking Account");
        type1.setActive(true);

        when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(type1));

        // When/Then
        mockMvc.perform(get("/api/v1/account-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CHECKING"))
                .andExpect(jsonPath("$[0].label").value("Checking Account"));
    }
}
