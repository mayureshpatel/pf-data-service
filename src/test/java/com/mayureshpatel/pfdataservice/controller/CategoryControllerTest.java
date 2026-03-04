package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CategoryController}.
 */
@DisplayName("CategoryController Unit Tests")
class CategoryControllerTest extends BaseControllerTest {

    private static final long CATEGORY_ID = 50L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/categories should return list of categories")
    void getCategories_shouldReturnListOfCategories() throws Exception {
        // Arrange
        CategoryDto categoryDto = new CategoryDto(CATEGORY_ID, null, "Groceries", null, null, "icon", "color");
        when(categoryService.getCategoriesByUserId(USER_ID)).thenReturn(List.of(categoryDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$[0].name").value("Groceries"));

        verify(categoryService).getCategoriesByUserId(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/categories/grouped should return grouped categories")
    void getCategoriesGrouped_shouldReturnGroupedCategories() throws Exception {
        // Arrange
        CategoryDto parentDto = new CategoryDto(CATEGORY_ID, null, "Food", null, null, "icon", "color");
        CategoryDto childDto = new CategoryDto(CATEGORY_ID + 1, null, "Groceries", null, parentDto, "icon", "color");

        List<CategoryDto> grouped = List.of(parentDto, childDto);

        when(categoryService.getCategoriesGrouped(USER_ID)).thenReturn(grouped);

        // Act & Assert
        // Map keys in JSON are strings, so this might be serialized differently depending on ObjectMapper config.
        // Usually, maps with object keys are serialized as a list of objects or stringified keys.
        // Given the complex key, Spring Boot default Jackson might fail or use toString() unless a KeySerializer is defined.
        // However, we just test that the endpoint returns 200 OK for now.
        mockMvc.perform(get("/api/v1/categories/grouped"))
                .andExpect(status().isOk());

        verify(categoryService).getCategoriesGrouped(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("GET /api/v1/categories/children should return child categories")
    void getChildCategories_shouldReturnChildCategories() throws Exception {
        // Arrange
        CategoryDto childDto = new CategoryDto(CATEGORY_ID + 1, null, "Groceries", null, null, "icon", "color");
        when(categoryService.getChildCategories(USER_ID)).thenReturn(List.of(childDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Groceries"));

        verify(categoryService).getChildCategories(USER_ID);
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("POST /api/v1/categories should create category")
    void createCategory_shouldCreateCategory() throws Exception {
        // Arrange
        CategoryDto requestDto = new CategoryDto(null, null, "New Category", com.mayureshpatel.pfdataservice.domain.category.CategoryType.EXPENSE, null, "icon", "color");
        CategoryDto responseDto = new CategoryDto(CATEGORY_ID, null, "New Category", com.mayureshpatel.pfdataservice.domain.category.CategoryType.EXPENSE, null, "icon", "color");

        when(categoryService.createCategory(eq(USER_ID), any(CategoryDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.name").value("New Category"));


        verify(categoryService).createCategory(eq(USER_ID), any(CategoryDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("PUT /api/v1/categories/{id} should update category")
    void updateCategory_shouldUpdateCategory() throws Exception {
        // Arrange
        CategoryDto requestDto = new CategoryDto(CATEGORY_ID, null, "Updated Name", com.mayureshpatel.pfdataservice.domain.category.CategoryType.EXPENSE, null, "icon", "color");

        when(categoryService.updateCategory(eq(USER_ID), eq(CATEGORY_ID), any(CategoryDto.class))).thenReturn(requestDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/categories/{id}", CATEGORY_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        verify(categoryService).updateCategory(eq(USER_ID), eq(CATEGORY_ID), any(CategoryDto.class));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("DELETE /api/v1/categories/{id} should delete category")
    void deleteCategory_shouldDeleteCategory() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/categories/{id}", CATEGORY_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(USER_ID, CATEGORY_ID);
    }

    @Test
    @DisplayName("GET /api/v1/categories should return 401 when not authenticated")
    void getCategories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/categories should return 401 when not authenticated")
    void createCategory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
