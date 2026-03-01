package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.CategoryRuleService;
import com.mayureshpatel.pfdataservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CategoryRuleController Unit Tests")
class CategoryRuleControllerTest extends BaseControllerTest {

    private static final long RULE_ID = 100L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/category-rules should return list of rules")
    void getRules_shouldReturnListOfRules() throws Exception {
        // Arrange
        CategoryRuleDto ruleDto = new CategoryRuleDto(RULE_ID, null, "netflix", 1, null);
        when(categoryRuleService.getRules(USER_ID)).thenReturn(List.of(ruleDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/category-rules"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(RULE_ID))
                .andExpect(jsonPath("$[0].keyword").value("netflix"));

        verify(categoryRuleService).getRules(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/category-rules should create rule")
    void createRule_shouldCreateRule() throws Exception {
        // Arrange
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto category = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(1L, null, "Category", com.mayureshpatel.pfdataservice.domain.category.CategoryType.EXPENSE, null, null, null);
        CategoryRuleDto requestDto = new CategoryRuleDto(null, null, "amazon", 2, category);
        CategoryRuleDto responseDto = new CategoryRuleDto(RULE_ID, null, "amazon", 2, category);

        when(categoryRuleService.createRule(eq(USER_ID), any(CategoryRuleDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/category-rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RULE_ID))
                .andExpect(jsonPath("$.keyword").value("amazon"));

        verify(categoryRuleService).createRule(eq(USER_ID), any(CategoryRuleDto.class));
    }
    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/category-rules/preview should return preview of changes")
    void previewApply_shouldReturnPreview() throws Exception {
        // Arrange
        RuleChangePreviewDto previewDto = new RuleChangePreviewDto("Category Change", "Old Category", "New Category");
        when(categoryRuleService.previewApply(USER_ID)).thenReturn(List.of(previewDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/category-rules/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].oldValue").value("Old Category"))
                .andExpect(jsonPath("$[0].newValue").value("New Category"));

        verify(categoryRuleService).previewApply(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/category-rules/apply should apply rules")
    void applyRules_shouldApplyRules() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/category-rules/apply")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(categoryRuleService).applyRules(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/category-rules/{id} should delete rule")
    void deleteRule_shouldDeleteRule() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/category-rules/{id}", RULE_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(categoryRuleService).deleteRule(USER_ID, RULE_ID);
    }
}
