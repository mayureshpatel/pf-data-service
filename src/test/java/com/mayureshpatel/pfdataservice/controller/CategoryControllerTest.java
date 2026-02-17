package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.dto.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.CategoryGroupDto;
import com.mayureshpatel.pfdataservice.service.CategoryService;
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

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @WithCustomMockUser
    void getCategories_ShouldReturnList() throws Exception {
        CategoryDto category = new CategoryDto(1L, "Dining", "fa-utensils", "EXPENSE", null, null);
        when(categoryService.getCategoriesByUserId(1L)).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dining"));
    }

    @Test
    @WithCustomMockUser
    void getCategoriesGrouped_ShouldReturnList() throws Exception {
        CategoryGroupDto group = new CategoryGroupDto("EXPENSE", List.of());
        when(categoryService.getCategoriesGrouped(1L)).thenReturn(List.of(group));

        mockMvc.perform(get("/api/v1/categories/grouped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    @WithCustomMockUser
    void createCategory_ShouldReturnCreated() throws Exception {
        CategoryDto dto = new CategoryDto(null, "New Category", "icon", "EXPENSE", null, null);
        CategoryDto response = new CategoryDto(10L, "New Category", "icon", "EXPENSE", null, null);
        
        when(categoryService.createCategory(eq(1L), any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("New Category"));
    }

    @Test
    @WithCustomMockUser
    void updateCategory_ShouldReturnUpdated() throws Exception {
        CategoryDto dto = new CategoryDto(null, "Updated", "icon", "EXPENSE", null, null);
        CategoryDto response = new CategoryDto(1L, "Updated", "icon", "EXPENSE", null, null);
        
        when(categoryService.updateCategory(eq(1L), eq(1L), any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @WithCustomMockUser
    void deleteCategory_ShouldReturnNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L, 1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());
    }
}
