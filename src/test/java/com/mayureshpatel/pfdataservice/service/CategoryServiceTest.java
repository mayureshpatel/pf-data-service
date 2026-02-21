package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;
    private Category testCategory;
    private com.mayureshpatel.pfdataservice.dto.category.CategoryDto testCategoryDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Groceries");
        testCategory.setType(CategoryType.EXPENSE);
        testCategory.setUser(testUser);

        testCategoryDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(
                1L, "Groceries", "#FF0000", "shopping-cart", CategoryType.EXPENSE, null, null);
    }

    @Test
    void createCategory_ValidData_ShouldCreateCategory() {
        // Given
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto createDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(
                null, "Groceries", "#FF0000", "shopping-cart", CategoryType.EXPENSE, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // When
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto result = categoryService.createCategory(1L, createDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Groceries");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_UserNotFound_ShouldThrowException() {
        // Given
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto createDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(
                null, "Groceries", "#FF0000", "shopping-cart", CategoryType.EXPENSE, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> categoryService.createCategory(1L, createDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCategoriesByUserId_ShouldReturnCategories() {
        // Given
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(testCategory));

        // When
        List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto> result = categoryService.getCategoriesByUserId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Groceries");
    }

    @Test
    void updateCategory_ValidData_ShouldUpdateCategory() {
        // Given
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(
                null, "Food & Dining", "#00FF00", "utensils", CategoryType.EXPENSE, null, null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // When
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto result = categoryService.updateCategory(1L, 1L, updateDto);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_CategoryNotFound_ShouldThrowException() {
        // Given
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(
                null, "Food & Dining", "#00FF00", "utensils", CategoryType.EXPENSE, null, null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, 1L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_WrongUser_ShouldThrowException() {
        // Given
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(
                null, "Food & Dining", "#00FF00", "utensils", CategoryType.EXPENSE, null, null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When/Then
        assertThatThrownBy(() -> categoryService.updateCategory(999L, 1L, updateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    void deleteCategory_ValidId_ShouldDeleteCategory() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        categoryService.deleteCategory(1L, 1L);

        // Then
        verify(categoryRepository).delete(testCategory);
    }

    @Test
    void deleteCategory_CategoryNotFound_ShouldThrowException() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> categoryService.deleteCategory(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategory_WrongUser_ShouldThrowException() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When/Then
        assertThatThrownBy(() -> categoryService.deleteCategory(999L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to user");
    }
}
