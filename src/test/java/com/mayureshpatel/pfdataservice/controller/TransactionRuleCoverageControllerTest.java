package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.TransactionRuleCoverageDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Unit tests for {@link TransactionRuleCoverageController} (PF-919): the list answer, a missing or oversized id list, and the 403 for anonymous callers. */
@DisplayName("TransactionRuleCoverageController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class TransactionRuleCoverageControllerTest extends BaseControllerTest {

    private static final String URL = "/api/v1/transactions/rule-coverage";
    private static final String IDS = "ids";

    @Test
    @DisplayName("GET should return the coverage for the given ids, scoped to the signed-in user")
    void getCoverage_shouldReturnList() throws Exception {
        // arrange
        when(transactionRuleCoverageService.findCoverage(USER_ID, List.of(1L, 2L))).thenReturn(List.of(
                new TransactionRuleCoverageDto(1L, true, false),
                new TransactionRuleCoverageDto(2L, false, true)));

        // act & assert & verify
        mockMvc.perform(get(URL).param(IDS, "1,2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionId").value(1))
                .andExpect(jsonPath("$[0].categoryRuleCovers").value(true))
                .andExpect(jsonPath("$[0].merchantRuleCovers").value(false))
                .andExpect(jsonPath("$[1].merchantRuleCovers").value(true));

        verify(transactionRuleCoverageService).findCoverage(USER_ID, List.of(1L, 2L));
    }

    @Test
    @DisplayName("GET should reject a request with no ids")
    void getCoverage_requiresIds() throws Exception {
        // act & assert & verify
        mockMvc.perform(get(URL)).andExpect(status().isBadRequest());
        verifyNoInteractions(transactionRuleCoverageService);
    }

    @Test
    @DisplayName("GET should reject more ids than one page can hold")
    void getCoverage_rejectsTooManyIds() throws Exception {
        // arrange
        String ids = LongStream.rangeClosed(1, 201).mapToObj(String::valueOf).collect(Collectors.joining(","));

        // act & assert & verify
        mockMvc.perform(get(URL).param(IDS, ids)).andExpect(status().isBadRequest());
        verifyNoInteractions(transactionRuleCoverageService);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET should reject an unauthenticated caller")
    void getCoverage_requiresAuthentication() throws Exception {
        // act & assert & verify
        mockMvc.perform(get(URL).param(IDS, "1")).andExpect(status().isForbidden());
    }
}
