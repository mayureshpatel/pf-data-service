package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.AppliedCategoryChangeDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.AppliedMerchantChangeDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.rule.CreateBothAndApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.rule.RuleApplicationUndoResultDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Unit tests for {@link RuleApplicationController} (PF-922): the undo answer, the "something to undo" validation, and the 403 for anonymous callers. */
@DisplayName("RuleApplicationController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class RuleApplicationControllerTest extends BaseControllerTest {

    private static final String URL = "/api/v1/rule-applications/undo";
    private static final String BOTH_URL = "/api/v1/rule-applications/create-both-and-apply";
    private static final String BOTH_BODY = "{\"category\":{\"rule\":{\"userId\":1,\"categoryId\":2,\"keywords\":[\"taco\"]},\"transactionIds\":[5]},\"merchant\":{\"rule\":{\"merchantId\":4,\"keywords\":[\"taco\"]},\"transactionIds\":[5]}}";
    private static final String BODY_CATEGORY = "{\"category\":{\"ruleId\":9,\"categoryId\":2,\"changes\":[{\"transactionId\":5,\"previousCategoryId\":null}]}}";

    @Test
    @DisplayName("POST should return what was restored, skipped and deleted")
    void undo_shouldReturnResult() throws Exception {
        // arrange
        when(ruleApplicationUndoService.undo(eq(USER_ID), any()))
                .thenReturn(new RuleApplicationUndoResultDto(1, List.of(6L), true, false));

        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_CATEGORY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restoredCount").value(1))
                .andExpect(jsonPath("$.skippedTransactionIds[0]").value(6))
                .andExpect(jsonPath("$.categoryRuleDeleted").value(true))
                .andExpect(jsonPath("$.merchantRuleDeleted").value(false));
    }

    @Test
    @DisplayName("POST should reject a request that has neither a category nor a merchant result")
    void undo_requiresSomethingToUndo() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ruleApplicationUndoService);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST should reject an unauthenticated caller")
    void undo_requiresAuthentication() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_CATEGORY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST create-both-and-apply should return each kind's result")
    void createBoth_shouldReturnBothResults() throws Exception {
        // arrange
        when(ruleApplicationService.createBothAndApply(eq(USER_ID), any())).thenReturn(new CreateBothAndApplyResultDto(
                new CategoryRuleApplyResultDto(1L, 2L, List.of(new AppliedCategoryChangeDto(5L, null)), List.of()),
                new MerchantRuleApplyResultDto(3L, 4L, List.of(new AppliedMerchantChangeDto(5L, null)))));

        // act & assert & verify
        mockMvc.perform(post(BOTH_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BOTH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.ruleId").value(1))
                .andExpect(jsonPath("$.category.categoryId").value(2))
                .andExpect(jsonPath("$.merchant.ruleId").value(3))
                .andExpect(jsonPath("$.merchant.merchantId").value(4));
    }

    @Test
    @DisplayName("POST create-both-and-apply should reject a request missing either kind")
    void createBoth_requiresBothKinds() throws Exception {
        // act & assert & verify
        mockMvc.perform(post(BOTH_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ruleApplicationService);
    }
}
