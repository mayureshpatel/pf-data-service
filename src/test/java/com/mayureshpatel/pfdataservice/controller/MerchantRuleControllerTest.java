package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import com.mayureshpatel.pfdataservice.dto.merchant.AppliedMerchantChangeDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleRefDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleVerdict;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Unit tests for {@link MerchantRuleController}: list, create, update and delete, validation failures, ownership guards, 404 for a foreign merchant and the 403 for anonymous callers. */
@DisplayName("MerchantRuleController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class MerchantRuleControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/api/v1/merchant-rules";
    private static final String ITEM_URL = BASE_URL + "/{id}";
    private static final Long RULE_ID = 1L;
    private static final Long MERCHANT_ID = 10L;
    private static final String KEYWORD = "KROGER";
    private static final String MERCHANT_NAME = "Kroger";

    private MerchantRuleCreateRequest.MerchantRuleCreateRequestBuilder validCreate() {
        return MerchantRuleCreateRequest.builder().merchantId(MERCHANT_ID).keywords(List.of(KEYWORD));
    }

    private MerchantRuleUpdateRequest.MerchantRuleUpdateRequestBuilder validUpdate() {
        return MerchantRuleUpdateRequest.builder().id(RULE_ID).merchantId(MERCHANT_ID).keywords(List.of(KEYWORD));
    }

    /** {@code GET /api/v1/merchant-rules} returns the user's rules, or an empty list when there are none. */
    @Nested
    @DisplayName("getRules")
    class GetRulesTests {

        @Test
        @DisplayName("GET should return list of rules for authenticated user")
        void getRules_shouldReturnList() throws Exception {
            // arrange
            MerchantDto merchant = MerchantDto.builder().id(MERCHANT_ID).name(MERCHANT_NAME).build();
            MerchantRuleDto ruleDto = MerchantRuleDto.builder()
                    .id(RULE_ID).keywords(List.of(KEYWORD)).priority(1).merchant(merchant).build();
            when(merchantRuleService.getRules(USER_ID)).thenReturn(List.of(ruleDto));

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(RULE_ID))
                    .andExpect(jsonPath("$[0].keywords[0]").value(KEYWORD))
                    .andExpect(jsonPath("$[0].merchant.name").value(MERCHANT_NAME));

            verify(merchantRuleService).getRules(USER_ID);
        }

        @Test
        @DisplayName("GET should return empty list when no rules exist")
        void getRules_shouldReturnEmptyList() throws Exception {
            // arrange
            when(merchantRuleService.getRules(USER_ID)).thenReturn(Collections.emptyList());

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET should reject an unauthenticated caller")
        void getRules_requiresAuthentication() throws Exception {
            // act & assert & verify -- no custom authentication entry point, so spring security answers 403
            mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleService);
        }
    }

    /** {@code POST /api/v1/merchant-rules} creates the rule and returns its id, 400 on validation failures, 404 for a foreign merchant and 403 when anonymous. */
    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {

        @Test
        @DisplayName("POST should create a new rule and return the generated id")
        void createRule_shouldReturnGeneratedId() throws Exception {
            // arrange
            when(merchantRuleService.createRule(eq(USER_ID), any(MerchantRuleCreateRequest.class))).thenReturn(42L);

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreate().priority(5).build())))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));

            verify(merchantRuleService).createRule(eq(USER_ID), any(MerchantRuleCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 naming every violated field when validation fails")
        void createRule_shouldReturn400OnInvalidInput() throws Exception {
            // arrange -- missing keywords and merchantId
            MerchantRuleCreateRequest request = MerchantRuleCreateRequest.builder().priority(5).build();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", containsInAnyOrder("keywords", "merchantId")));

            verifyNoInteractions(merchantRuleService);
        }

        @Test
        @DisplayName("POST should return 400 when there are more than 20 keywords")
        void createRule_moreThan20Keywords_returns400() throws Exception {
            // arrange
            List<String> keywords = IntStream.rangeClosed(1, 21).mapToObj(i -> "KW" + i).toList();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreate().keywords(keywords).build())))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(merchantRuleService);
        }

        @Test
        @DisplayName("POST should return 400 when the minimum amount exceeds the maximum")
        void createRule_inconsistentAmountRange_returns400() throws Exception {
            // arrange
            MerchantRuleCreateRequest request = validCreate()
                    .minAmount(new BigDecimal("50.00")).maxAmount(new BigDecimal("10.00")).build();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", containsInAnyOrder("amountRangeConsistent")));

            verifyNoInteractions(merchantRuleService);
        }

        @Test
        @DisplayName("POST should return 404 when the merchant is not the caller's")
        void createRule_foreignMerchant_returns404() throws Exception {
            // arrange
            when(merchantRuleService.createRule(eq(USER_ID), any(MerchantRuleCreateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Merchant not found."));

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreate().build())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST should reject an unauthenticated caller")
        void createRule_requiresAuthentication() throws Exception {
            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreate().build())))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleService);
        }
    }

    /** {@code PUT /api/v1/merchant-rules} updates the rule and returns the affected-row count, 400 on validation failures, 403 for a rule that is not the caller's and 403 when anonymous. */
    @Nested
    @DisplayName("updateRule")
    class UpdateRuleTests {

        @Test
        @DisplayName("PUT should update an existing rule and return rows affected")
        void updateRule_shouldReturnRowsAffected() throws Exception {
            // arrange
            when(merchantRuleService.updateRule(eq(USER_ID), any(MerchantRuleUpdateRequest.class))).thenReturn(1);

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdate().build())))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(merchantRuleService).updateRule(eq(USER_ID), any(MerchantRuleUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 naming every violated field when validation fails")
        void updateRule_shouldReturn400OnInvalidInput() throws Exception {
            // arrange -- missing id, merchantId and keywords
            MerchantRuleUpdateRequest request = MerchantRuleUpdateRequest.builder().priority(10).build();

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", containsInAnyOrder("id", "merchantId", "keywords")));
        }

        @Test
        @DisplayName("PUT should return 403 when the caller does not own the rule")
        void updateRule_notOwner_returns403() throws Exception {
            // arrange
            when(securityService.isMerchantRuleOwner(eq(RULE_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdate().build())))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleService);
        }

        @Test
        @DisplayName("PUT should return 404 when the merchant is not the caller's")
        void updateRule_foreignMerchant_returns404() throws Exception {
            // arrange
            when(merchantRuleService.updateRule(eq(USER_ID), any(MerchantRuleUpdateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Merchant not found."));

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdate().build())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("PUT should reject an unauthenticated caller")
        void updateRule_requiresAuthentication() throws Exception {
            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdate().build())))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleService);
        }
    }

    /** {@code DELETE /api/v1/merchant-rules/{id}} deletes with 204, 403 for a rule that is not the caller's, and 403 when anonymous. */
    @Nested
    @DisplayName("deleteRule")
    class DeleteRuleTests {

        @Test
        @DisplayName("DELETE should delete the rule and return 204")
        void deleteRule_shouldReturn204() throws Exception {
            // act & assert & verify
            mockMvc.perform(delete(ITEM_URL, RULE_ID).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(merchantRuleService).deleteRule(USER_ID, RULE_ID);
        }

        @Test
        @DisplayName("DELETE should return 403 when the caller does not own the rule")
        void deleteRule_notOwner_returns403() throws Exception {
            // arrange
            when(securityService.isMerchantRuleOwner(eq(RULE_ID), any())).thenReturn(false);

            // act & assert & verify
            mockMvc.perform(delete(ITEM_URL, RULE_ID).with(csrf()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleService);
        }

        @Test
        @DisplayName("DELETE should return 404 when the service reports the rule missing")
        void deleteRule_missing_returns404() throws Exception {
            // arrange
            doThrow(new ResourceNotFoundException("Rule not found")).when(merchantRuleService).deleteRule(eq(USER_ID), anyLong());

            // act & assert & verify
            mockMvc.perform(delete(ITEM_URL, RULE_ID).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("DELETE should reject an unauthenticated caller")
        void deleteRule_requiresAuthentication() throws Exception {
            // act & assert & verify
            mockMvc.perform(delete(ITEM_URL, RULE_ID).with(csrf()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleService);
        }
    }

    /** {@code POST /api/v1/merchant-rules/match-preview} returns the read-only preview for a draft rule, 400 on validation failures, 404 for a rule or merchant that is not the caller's, and is closed to anonymous callers. */
    @Nested
    @DisplayName("matchPreview")
    class MatchPreviewTests {

        private static final String URL = BASE_URL + "/match-preview";

        private MerchantRuleMatchPreviewRequest draft(Long ruleId, List<String> keywords) {
            return new MerchantRuleMatchPreviewRequest(ruleId, keywords, MatchType.OR, 0, null, null, MERCHANT_ID);
        }

        @Test
        @DisplayName("POST /match-preview should return 200 with the preview body")
        void matchPreview_validRequest_returns200WithBody() throws Exception {
            // arrange
            MerchantRuleMatchRowDto row = new MerchantRuleMatchRowDto(5L, OffsetDateTime.parse("2026-10-01T00:00:00Z"),
                    "KROGER #1", new BigDecimal("-25.00"), "Checking", null,
                    MerchantRuleVerdict.WILL_ASSIGN, List.of(), null);
            MerchantRuleMatchPreviewDto preview = new MerchantRuleMatchPreviewDto(MERCHANT_NAME, 1, 1, 0, 0, 0, false, List.of(row));
            when(merchantRuleMatchPreviewService.preview(eq(USER_ID), any(MerchantRuleMatchPreviewRequest.class))).thenReturn(preview);

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(null, List.of(KEYWORD)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.targetMerchantName").value(MERCHANT_NAME))
                    .andExpect(jsonPath("$.totalMatches").value(1))
                    .andExpect(jsonPath("$.willAssignCount").value(1))
                    .andExpect(jsonPath("$.linkedCount").value(0))
                    .andExpect(jsonPath("$.truncated").value(false))
                    .andExpect(jsonPath("$.rows", hasSize(1)))
                    .andExpect(jsonPath("$.rows[0].transactionId").value(5))
                    .andExpect(jsonPath("$.rows[0].verdict").value("WILL_ASSIGN"));

            verify(merchantRuleMatchPreviewService).preview(eq(USER_ID), eq(draft(null, List.of(KEYWORD))));
        }

        @Test
        @DisplayName("POST /match-preview should return 400 when a keyword is blank")
        void matchPreview_blankKeyword_returns400() throws Exception {
            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(null, List.of(KEYWORD, " ")))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(merchantRuleMatchPreviewService);
        }

        @Test
        @DisplayName("POST /match-preview should return 400 when there are more than 20 keywords")
        void matchPreview_moreThan20Keywords_returns400() throws Exception {
            // arrange
            List<String> keywords = IntStream.rangeClosed(1, 21).mapToObj(i -> "KW" + i).toList();

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(null, keywords))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(merchantRuleMatchPreviewService);
        }

        @Test
        @DisplayName("POST /match-preview should return 200 with an empty preview for an empty keyword list")
        void matchPreview_emptyKeywordList_returns200Empty() throws Exception {
            // arrange
            when(merchantRuleMatchPreviewService.preview(eq(USER_ID), any(MerchantRuleMatchPreviewRequest.class)))
                    .thenReturn(MerchantRuleMatchPreviewDto.empty(null));

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(null, List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalMatches").value(0))
                    .andExpect(jsonPath("$.rows", hasSize(0)));
        }

        @Test
        @DisplayName("POST /match-preview should return 404 for an unknown or foreign rule or merchant id")
        void matchPreview_unknownOrForeignId_returns404() throws Exception {
            // arrange
            when(merchantRuleMatchPreviewService.preview(eq(USER_ID), any(MerchantRuleMatchPreviewRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Rule not found"));

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(999L, List.of(KEYWORD)))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /match-preview should reject an unauthenticated caller")
        void matchPreview_requiresAuthentication() throws Exception {
            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(null, List.of(KEYWORD)))))
                    // no custom authentication entry point, so spring security answers 403
                    .andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleMatchPreviewService);
        }
    }

    /** {@code GET /preview} lists the changes and {@code POST /apply} returns how many changed; both answer 403 when anonymous. */
    @Nested
    @DisplayName("preview and apply")
    class ApplyTests {

        @Test
        @DisplayName("GET /preview should return the changes for the signed-in user")
        void previewApply_returnsChanges() throws Exception {
            // arrange
            MerchantDto merchant = MerchantDto.builder().id(MERCHANT_ID).name(MERCHANT_NAME).build();
            MerchantRuleChangePreviewDto change = new MerchantRuleChangePreviewDto(
                    5L, OffsetDateTime.parse("2026-01-02T00:00:00Z"), "KROGER #1", new BigDecimal("-12.50"),
                    "Main Checking", merchant, new MerchantRuleRefDto(RULE_ID, List.of(KEYWORD), MERCHANT_NAME, 1));
            when(merchantRuleApplyService.preview(USER_ID)).thenReturn(List.of(change));

            // act & assert & verify
            mockMvc.perform(get(BASE_URL + "/preview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].transactionId").value(5))
                    .andExpect(jsonPath("$[0].merchant.name").value(MERCHANT_NAME))
                    .andExpect(jsonPath("$[0].rule.id").value(RULE_ID));
            verify(merchantRuleApplyService).preview(USER_ID);
        }

        @Test
        @DisplayName("POST /apply should return the number of changed transactions")
        void applyRules_returnsCount() throws Exception {
            // arrange
            when(merchantRuleApplyService.apply(USER_ID)).thenReturn(3);

            // act & assert & verify
            mockMvc.perform(post(BASE_URL + "/apply").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
            verify(merchantRuleApplyService).apply(USER_ID);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /preview and POST /apply should reject an unauthenticated caller")
        void previewAndApply_requireAuthentication() throws Exception {
            // act & assert & verify
            mockMvc.perform(get(BASE_URL + "/preview")).andExpect(status().isForbidden());
            mockMvc.perform(post(BASE_URL + "/apply").with(csrf())).andExpect(status().isForbidden());

            verifyNoInteractions(merchantRuleApplyService);
        }
    }

    /** {@code POST /api/v1/merchant-rules/create-and-apply} (PF-921): the answer, and the validation of the rule and the id list. */
    @Nested
    @DisplayName("createAndApply")
    class CreateAndApplyTests {

        private static final String URL = BASE_URL + "/create-and-apply";
        private static final String BODY_OK = "{\"rule\":{\"merchantId\":10,\"keywords\":[\"kroger\"]},\"transactionIds\":[5,6]}";
        private static final String BODY_NO_IDS = "{\"rule\":{\"merchantId\":10,\"keywords\":[\"kroger\"]},\"transactionIds\":[]}";
        private static final String BODY_NO_KEYWORDS = "{\"rule\":{\"merchantId\":10,\"keywords\":[]},\"transactionIds\":[5]}";

        @Test
        @DisplayName("POST should return the rule id and the changed rows with their previous merchants")
        void createAndApply_shouldReturnResult() throws Exception {
            // arrange
            when(merchantRuleService.createAndApply(eq(USER_ID), any())).thenReturn(new MerchantRuleApplyResultDto(
                    9L, 10L, List.of(new AppliedMerchantChangeDto(5L, null), new AppliedMerchantChangeDto(6L, 3L))));

            // act & assert & verify
            mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleId").value(9))
                    .andExpect(jsonPath("$.changes", hasSize(2)))
                    .andExpect(jsonPath("$.changes[1].previousMerchantId").value(3));
        }

        @Test
        @DisplayName("POST should reject an empty id list and an invalid rule without calling the service")
        void createAndApply_shouldValidate() throws Exception {
            // act & assert & verify
            mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_NO_IDS))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_NO_KEYWORDS))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(merchantRuleService);
        }
    }
}
