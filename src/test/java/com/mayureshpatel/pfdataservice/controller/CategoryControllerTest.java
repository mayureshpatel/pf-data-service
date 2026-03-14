package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Unit tests for {@link CategoryController}.
 * Follows the Gold Standard for controller testing.
 */
@DisplayName("CategoryController Unit Tests")
@WithCustomMockUser(id = BaseControllerTest.USER_ID)
class CategoryControllerTest extends BaseControllerTest {

    private static final Long CATEGORY_ID = 1L;

    @Nested
    @DisplayName("getCategories")
    class GetCategoriesTests {

        @ParameterizedTest
        @ValueSource(strings = {"/api/v1/categories", "/api/categories"})
        @DisplayName("GET should return list of categories for both URL versions")
        void getCategories_shouldReturnList(String url) throws Exception {
            // Arrange
            CategoryDto categoryDto = new CategoryDto(CATEGORY_ID, USER_ID, "Groceries", null, null, "shopping_cart", "#FF5722");
            when(categoryService.getCategoriesByUserId(USER_ID)).thenReturn(List.of(categoryDto));

            // Act & Assert
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(CATEGORY_ID))
                    .andExpect(jsonPath("$[0].name").value("Groceries"));

            verify(categoryService).getCategoriesByUserId(USER_ID);
        }

        @Test
        @DisplayName("GET should return empty list when no categories exist")
        void getCategories_shouldReturnEmptyList() throws Exception {
            // Arrange
            when(categoryService.getCategoriesByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("getCategoriesGrouped")
    class GetCategoriesGroupedTests {

        @Test
        @DisplayName("GET /grouped should return grouped categories")
        void getCategoriesGrouped_shouldReturnList() throws Exception {
            // Arrange
            when(categoryService.getParentCategories(USER_ID)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/categories/grouped"))
                    .andExpect(status().isOk());

            verify(categoryService).getParentCategories(USER_ID);
        }
    }

    @Nested
    @DisplayName("getChildCategories")
    class GetChildCategoriesTests {

        @Test
        @DisplayName("GET /children should return list of child categories")
        void getChildCategories_shouldReturnList() throws Exception {
            // Arrange
            when(categoryService.getChildCategories(USER_ID)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/categories/children"))
                    .andExpect(status().isOk());

            verify(categoryService).getChildCategories(USER_ID);
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("POST should create a new category and return its ID")
        void createCategory_shouldReturnId() throws Exception {
            // Arrange
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(USER_ID)
                    .name("Dining")
                    .type("EXPENSE")
                    .build();

            when(categoryService.createCategory(eq(USER_ID), any(CategoryCreateRequest.class))).thenReturn(CATEGORY_ID.intValue());

            // Act & Assert
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(CATEGORY_ID.toString()));

            verify(categoryService).createCategory(eq(USER_ID), any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("POST should return 400 Bad Request when validation fails")
        void createCategory_shouldReturn400OnInvalidInput() throws Exception {
            // Arrange - blank name
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .userId(USER_ID)
                    .name("")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("PUT should update category and return status")
        void updateCategory_shouldReturnStatus() throws Exception {
            // Arrange
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(CATEGORY_ID)
                    .userId(USER_ID)
                    .name("Updated Name")
                    .build();

            when(categoryService.updateCategory(eq(USER_ID), any(CategoryUpdateRequest.class))).thenReturn(1);

            // Act & Assert
            mockMvc.perform(put("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(categoryService).updateCategory(eq(USER_ID), any(CategoryUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("DELETE should remove category and return rows affected")
        void deleteCategory_shouldReturnStatus() throws Exception {
            // Arrange
            when(categoryService.deleteCategory(USER_ID, CATEGORY_ID)).thenReturn(1);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/categories/{id}", CATEGORY_ID)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));

            verify(categoryService).deleteCategory(USER_ID, CATEGORY_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("GET should return 500 when service fails unexpectedly")
        void getCategories_shouldReturn500() throws Exception {
            // Arrange
            when(categoryService.getCategoriesByUserId(anyLong()))
                    .thenThrow(new RuntimeException("Server error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("DELETE should return 404 Not Found when category does not exist")
        void deleteCategory_shouldReturn404() throws Exception {
            // Arrange
            when(categoryService.deleteCategory(USER_ID, CATEGORY_ID))
                    .thenThrow(new ResourceNotFoundException("Category not found"));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/categories/{id}", CATEGORY_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
