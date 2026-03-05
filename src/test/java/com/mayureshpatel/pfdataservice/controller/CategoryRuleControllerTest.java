package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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

    private static final Long RULE_ID = 1L;

    @Nested
    @DisplayName("getRules")
    class GetRulesTests {

        @Test
        @DisplayName("GET should return list of rules for authenticated user")
        void getRules_shouldReturnList() throws Exception {
            // Arrange
            CategoryRuleDto ruleDto = CategoryRuleDto.builder()
                    .id(RULE_ID)
                    .keyword("AMZN")
                    .priority(1)
                    .build();

            when(categoryRuleService.getRules(USER_ID)).thenReturn(List.of(ruleDto));

            // Act & Assert
            mockMvc.perform(get("/api/v1/category-rules"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(RULE_ID))
                    .andExpect(jsonPath("$[0].keyword").value("AMZN"));

            verify(categoryRuleService).getRules(USER_ID);
        }

        @Test
        @DisplayName("GET should return empty list when no rules exist")
        void getRules_shouldReturnEmptyList() throws Exception {
            // Arrange
            when(categoryRuleService.getRules(USER_ID)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/category-rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {

        @Test
        @DisplayName("POST should create a new rule and return rows affected")
        void createRule_shouldReturnRowsAffected() throws Exception {
            // Arrange
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .keyword("Starbucks")
                    .priority(5)
                    .categoryId(10L)
                    .build();

            when(categoryRuleService.createRule(eq(USER_ID), any(CategoryRuleCreateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(post("/api/v1/category-rules")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(categoryRuleService).createRule(eq(USER_ID), any(CategoryRuleCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createRule_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange - missing keyword and categoryId
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .priority(5)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/category-rules")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[*].field", org.hamcrest.Matchers.containsInAnyOrder("keyword", "categoryId")));
        }
    }

    @Nested
    @DisplayName("updateRule")
    class UpdateRuleTests {

        @Test
        @DisplayName("PUT should update an existing rule and return rows affected")
        void updateRule_shouldReturnRowsAffected() throws Exception {
            // Arrange
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .categoryId(20L)
                    .priority(10)
                    .build();

            when(categoryRuleService.updateRule(eq(USER_ID), eq(RULE_ID), any(CategoryRuleUpdateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(put("/api/v1/category-rules/{id}", RULE_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(categoryRuleService).updateRule(eq(USER_ID), eq(RULE_ID), any(CategoryRuleUpdateRequest.class));
        }

        @Test
        @DisplayName("PUT should return 400 Bad Request when validation fails")
        void updateRule_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange - missing categoryId
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .priority(10)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/v1/category-rules/{id}", RULE_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("categoryId"));
        }
    }

    @Nested
    @DisplayName("previewApply")
    class PreviewApplyTests {

        @Test
        @DisplayName("GET /preview should return rule change previews")
        void previewApply_shouldReturnPreviews() throws Exception {
            // Arrange
            RuleChangePreviewDto preview = new RuleChangePreviewDto("AMZN MKTP", "Uncategorized", "Shopping");
            when(categoryRuleService.previewApply(USER_ID)).thenReturn(List.of(preview));

            // Act & Assert
            mockMvc.perform(get("/api/v1/category-rules/preview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].oldValue").value("Uncategorized"))
                    .andExpect(jsonPath("$[0].newValue").value("Shopping"));

            verify(categoryRuleService).previewApply(USER_ID);
        }
    }

    @Nested
    @DisplayName("applyRules")
    class ApplyRulesTests {

        @Test
        @DisplayName("POST /apply should trigger rule application and return 200")
        void applyRules_shouldReturnOk() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/category-rules/apply")
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(categoryRuleService).applyRules(USER_ID);
        }
    }

    @Nested
    @DisplayName("deleteRule")
    class DeleteRuleTests {

        @Test
        @DisplayName("DELETE should remove rule and return 204 No Content")
        void deleteRule_shouldReturnNoContent() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/category-rules/{id}", RULE_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(categoryRuleService).deleteRule(USER_ID, RULE_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("DELETE should return 404 Not Found when rule does not exist")
        void deleteRule_shouldReturn404() throws Exception {
            // Arrange
            org.mockito.Mockito.doThrow(new ResourceNotFoundException("Rule not found"))
                    .when(categoryRuleService).deleteRule(USER_ID, RULE_ID);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/category-rules/{id}", RULE_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET should return 500 when service fails unexpectedly")
        void getRules_shouldReturn500() throws Exception {
            // Arrange
            when(categoryRuleService.getRules(anyLong()))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/category-rules"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
