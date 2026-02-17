package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.vendor.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.service.CategoryRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryRuleService categoryRuleService;

    @Test
    @WithCustomMockUser
    void getRules_ShouldReturnList() throws Exception {
        CategoryRuleDto rule = CategoryRuleDto.builder().id(1L).keyword("Starbucks").categoryName("Dining").build();
        when(categoryRuleService.getRules(1L)).thenReturn(List.of(rule));

        mockMvc.perform(get("/api/v1/category-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("Starbucks"));
    }

    @Test
    @WithCustomMockUser
    void createRule_ShouldReturnCreated() throws Exception {
        CategoryRuleDto dto = CategoryRuleDto.builder().keyword("New").categoryName("Cat").build();
        CategoryRuleDto response = CategoryRuleDto.builder().id(10L).keyword("New").categoryName("Cat").build();
        
        when(categoryRuleService.createRule(eq(1L), any(CategoryRuleDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/category-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithCustomMockUser
    void previewApply_ShouldReturnList() throws Exception {
        RuleChangePreviewDto preview = new RuleChangePreviewDto("Desc", "Old", "New");
        when(categoryRuleService.previewApply(1L)).thenReturn(List.of(preview));

        mockMvc.perform(get("/api/v1/category-rules/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalDescription").value("Desc"));
    }

    @Test
    @WithCustomMockUser
    void applyRules_ShouldReturnOk() throws Exception {
        when(categoryRuleService.applyRules(1L)).thenReturn(5);

        mockMvc.perform(post("/api/v1/category-rules/apply"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomMockUser
    void deleteRule_ShouldReturnNoContent() throws Exception {
        doNothing().when(categoryRuleService).deleteRule(1L, 1L);

        mockMvc.perform(delete("/api/v1/category-rules/1"))
                .andExpect(status().isNoContent());
    }
}
