package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.AppliedCategoryChangeDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchPreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchPreviewRequest;
import com.mayureshpatel.pfdataservice.dto.category.RuleMatchRowDto;
import com.mayureshpatel.pfdataservice.dto.category.RuleVerdict;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CategoryRuleController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("CategoryRuleController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class CategoryRuleControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/api/v1/category-rules";
    private static final String VALIDATION_FIELDS = "$.validationErrors[*].field";
    private static final String FIELD_KEYWORDS = "keywords";
    private static final Long RULE_ID = 1L;

    /** {@code GET /api/v1/category-rules} returns the user's rules, or an empty list, not an error, when there are none. */
    @Nested
    @DisplayName("getRules")
    class GetRulesTests {

        @Test
        @DisplayName("GET should return list of rules for authenticated user")
        void getRules_shouldReturnList() throws Exception {
            // arrange
            CategoryRuleDto ruleDto = CategoryRuleDto.builder()
                    .id(RULE_ID)
                    .keywords(List.of("AMZN"))
                    .priority(1)
                    .build();

            when(categoryRuleService.getRules(USER_ID)).thenReturn(List.of(ruleDto));

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(RULE_ID))
                    .andExpect(jsonPath("$[0].keywords[0]").value("AMZN"));

            verify(categoryRuleService).getRules(USER_ID);
        }

        @Test
        @DisplayName("GET should return empty list when no rules exist")
        void getRules_shouldReturnEmptyList() throws Exception {
            // arrange
            when(categoryRuleService.getRules(USER_ID)).thenReturn(Collections.emptyList());

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    /** {@code POST /api/v1/category-rules} creates the rule and returns its new id, and returns a 400 naming every violated field at once (missing {@code keywords} and {@code categoryId} together) when the request fails validation. */
    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {

        @Test
        @DisplayName("POST should create a new rule and return the generated id")
        void createRule_shouldReturnGeneratedId() throws Exception {
            // arrange
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .keywords(List.of("Starbucks"))
                    .priority(5)
                    .categoryId(10L)
                    .build();

            when(categoryRuleService.createRule(eq(USER_ID), any(CategoryRuleCreateRequest.class))).thenReturn(42L);

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));

            verify(categoryRuleService).createRule(eq(USER_ID), any(CategoryRuleCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 when there are more than 20 keywords (PF-911)")
        void createRule_moreThan20Keywords_returns400() throws Exception {
            // arrange
            List<String> keywords = IntStream.rangeClosed(1, 21).mapToObj(i -> "KW" + i).toList();
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID).categoryId(1L).keywords(keywords).build();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(VALIDATION_FIELDS, org.hamcrest.Matchers.hasItem(FIELD_KEYWORDS)));

            verifyNoInteractions(categoryRuleService);
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createRule_shouldReturn400OnInvalidInput() throws Exception {
            // arrange - missing keywords and categoryId
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .priority(5)
                    .build();

            // act & assert & verify
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(VALIDATION_FIELDS, org.hamcrest.Matchers.containsInAnyOrder(FIELD_KEYWORDS, "categoryId")));
        }
    }

    /** {@code PUT /api/v1/category-rules} updates the rule and returns the affected-row count, and returns a 400 naming every violated field at once (missing {@code id}, {@code categoryId}, and {@code keywords} together) when the request fails validation. */
    @Nested
    @DisplayName("updateRule")
    class UpdateRuleTests {

        @Test
        @DisplayName("PUT should update an existing rule and return rows affected")
        void updateRule_shouldReturnRowsAffected() throws Exception {
            // arrange
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(RULE_ID)
                    .keywords(List.of("Updated Keyword"))
                    .categoryId(20L)
                    .priority(10)
                    .build();

            when(categoryRuleService.updateRule(eq(USER_ID), any(CategoryRuleUpdateRequest.class))).thenReturn(1);

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(categoryRuleService).updateRule(eq(USER_ID), any(CategoryRuleUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 when there are more than 20 keywords (PF-911)")
        void updateRule_moreThan20Keywords_returns400() throws Exception {
            // arrange
            List<String> keywords = IntStream.rangeClosed(1, 21).mapToObj(i -> "KW" + i).toList();
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(1L).categoryId(1L).keywords(keywords).build();

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(VALIDATION_FIELDS, org.hamcrest.Matchers.hasItem(FIELD_KEYWORDS)));

            verifyNoInteractions(categoryRuleService);
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when validation fails")
        void updateRule_shouldReturn400OnInvalidInput() throws Exception {
            // arrange - missing id and categoryId
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .priority(10)
                    .build();

            // act & assert & verify
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(VALIDATION_FIELDS, org.hamcrest.Matchers.containsInAnyOrder("id", "categoryId", FIELD_KEYWORDS)));
        }
    }

    /** {@code POST /api/v1/category-rules/match-preview} returns the read-only match preview for a draft rule, 400 on validation failures, 404 for a rule that is not the caller's, and is closed to anonymous callers. */
    @Nested
    @DisplayName("matchPreview")
    class MatchPreviewTests {

        private static final String URL = BASE_URL + "/match-preview";
        private static final String KEYWORD = "AMAZON";

        private RuleMatchPreviewRequest draft(Long ruleId, List<String> keywords) {
            return new RuleMatchPreviewRequest(ruleId, keywords, MatchType.OR, 0, null, null);
        }

        @Test
        @DisplayName("POST /match-preview should return 200 with the preview body")
        void matchPreview_validRequest_returns200WithBody() throws Exception {
            // arrange
            RuleMatchRowDto row = new RuleMatchRowDto(5L, OffsetDateTime.parse("2026-10-01T00:00:00Z"),
                    "AMAZON MKTP", new BigDecimal("-25.00"), "Checking", null,
                    RuleVerdict.WILL_CATEGORIZE, List.of());
            RuleMatchPreviewDto preview = new RuleMatchPreviewDto(1, 1, 0, 0, false, List.of(row), List.of(row.transactionId()));
            when(ruleMatchPreviewService.preview(eq(USER_ID), any(RuleMatchPreviewRequest.class))).thenReturn(preview);

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft(null, List.of(KEYWORD)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalMatches").value(1))
                    .andExpect(jsonPath("$.willCategorizeCount").value(1))
                    .andExpect(jsonPath("$.truncated").value(false))
                    .andExpect(jsonPath("$.rows", hasSize(1)))
                    .andExpect(jsonPath("$.rows[0].transactionId").value(5))
                    .andExpect(jsonPath("$.rows[0].verdict").value("WILL_CATEGORIZE"));

            verify(ruleMatchPreviewService).preview(eq(USER_ID), eq(draft(null, List.of(KEYWORD))));
        }

        @Test
        @DisplayName("POST /match-preview should return 400 when a keyword is blank")
        void matchPreview_blankKeyword_returns400() throws Exception {
            // arrange
            RuleMatchPreviewRequest request = draft(null, List.of(KEYWORD, " "));

            // act & assert & verify
            mockMvc.perform(post(URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(ruleMatchPreviewService);
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

            verifyNoInteractions(ruleMatchPreviewService);
        }

        @Test
        @DisplayName("POST /match-preview should return 200 with an empty preview for an empty keyword list")
        void matchPreview_emptyKeywordList_returns200Empty() throws Exception {
            // arrange
            when(ruleMatchPreviewService.preview(eq(USER_ID), any(RuleMatchPreviewRequest.class)))
                    .thenReturn(RuleMatchPreviewDto.empty());

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
        @DisplayName("POST /match-preview should return 404 for an unknown or foreign rule id")
        void matchPreview_unknownOrForeignRuleId_returns404() throws Exception {
            // arrange
            when(ruleMatchPreviewService.preview(eq(USER_ID), any(RuleMatchPreviewRequest.class)))
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

            verifyNoInteractions(ruleMatchPreviewService);
        }
    }

    /** {@code GET /api/v1/category-rules/preview} returns {@code CategoryRuleService#previewApply}'s change previews without applying anything. */
    @Nested
    @DisplayName("previewApply")
    class PreviewApplyTests {

        @Test
        @DisplayName("GET /preview should return rule change previews")
        void previewApply_shouldReturnPreviews() throws Exception {
            // arrange
            RuleChangePreviewDto preview = new RuleChangePreviewDto("AMZN MKTP", "Uncategorized", "Shopping");
            when(categoryRuleService.previewApply(USER_ID)).thenReturn(List.of(preview));

            // act & assert & verify
            mockMvc.perform(get("/api/v1/category-rules/preview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].oldValue").value("Uncategorized"))
                    .andExpect(jsonPath("$[0].newValue").value("Shopping"));

            verify(categoryRuleService).previewApply(USER_ID);
        }
    }

    /** {@code POST /api/v1/category-rules/apply} delegates to {@code CategoryRuleService#applyRules} and returns 200. */
    @Nested
    @DisplayName("applyRules")
    class ApplyRulesTests {

        @Test
        @DisplayName("POST /apply should trigger rule application and return 200")
        void applyRules_shouldReturnOk() throws Exception {
            // act & assert & verify
            mockMvc.perform(post("/api/v1/category-rules/apply")
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(categoryRuleService).applyRules(USER_ID);
        }
    }

    /** {@code DELETE /api/v1/category-rules/{id}} returns 204 on success. */
    @Nested
    @DisplayName("deleteRule")
    class DeleteRuleTests {

        @Test
        @DisplayName("DELETE should remove rule and return 204 No Content")
        void deleteRule_shouldReturnNoContent() throws Exception {
            // act & assert & verify
            mockMvc.perform(delete("/api/v1/category-rules/{id}", RULE_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(categoryRuleService).deleteRule(USER_ID, RULE_ID);
        }
    }

    /** {@code DELETE} returns 404 when the service throws {@link ResourceNotFoundException}; an unexpected exception elsewhere surfaces as a generic 500 via {@code GlobalExceptionHandler}'s catch-all. */
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("DELETE should return 404 Not Found when rule does not exist")
        void deleteRule_shouldReturn404() throws Exception {
            // arrange
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Rule not found"))
                    .when(categoryRuleService).deleteRule(USER_ID, RULE_ID);

            // act & assert & verify
            mockMvc.perform(delete("/api/v1/category-rules/{id}", RULE_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET should return 500 when service fails unexpectedly")
        void getRules_shouldReturn500() throws Exception {
            // arrange
            when(categoryRuleService.getRules(anyLong()))
                    .thenThrow(new RuntimeException("Database error"));

            // act & assert & verify
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isInternalServerError());
        }
    }

    /** {@code POST /api/v1/category-rules/create-and-apply} (PF-920): the answer, and the validation of the rule and the id list. */
    @Nested
    @DisplayName("createAndApply")
    class CreateAndApplyTests {

        private static final String URL = BASE_URL + "/create-and-apply";
        private static final String BODY_NO_IDS = "{\"rule\":{\"userId\":1,\"categoryId\":2,\"keywords\":[\"taco\"]},\"transactionIds\":[]}";
        private static final String BODY_NO_KEYWORDS = "{\"rule\":{\"userId\":1,\"categoryId\":2,\"keywords\":[]},\"transactionIds\":[5]}";
        private static final String BODY_OK = "{\"rule\":{\"userId\":1,\"categoryId\":2,\"keywords\":[\"taco\"]},\"transactionIds\":[5,6]}";

        @Test
        @DisplayName("POST should return the rule id, the changed rows with previous categories and the skipped ids")
        void createAndApply_shouldReturnResult() throws Exception {
            // arrange
            when(categoryRuleService.createAndApply(eq(USER_ID), any())).thenReturn(new CategoryRuleApplyResultDto(
                    9L, 2L, List.of(new AppliedCategoryChangeDto(5L, null), new AppliedCategoryChangeDto(6L, 3L)), List.of(7L)));

            // act & assert & verify
            mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleId").value(9))
                    .andExpect(jsonPath("$.changes", hasSize(2)))
                    .andExpect(jsonPath("$.changes[1].previousCategoryId").value(3))
                    .andExpect(jsonPath("$.skippedTransactionIds[0]").value(7));
        }

        @Test
        @DisplayName("POST should reject an empty id list and an invalid rule without calling the service")
        void createAndApply_shouldValidate() throws Exception {
            // act & assert & verify
            mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_NO_IDS))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY_NO_KEYWORDS))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(categoryRuleService);
        }
    }
}
