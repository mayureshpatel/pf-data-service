package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long PARENT_ID = 5L;

    @Nested
    @DisplayName("getCategoriesByUserId")
    class GetCategoriesByUserIdTests {
        @Test
        @DisplayName("should return mapped category DTOs for a user")
        void shouldReturnCategories() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).name("Food").build();
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(category));

            // Act
            List<CategoryDto> result = categoryService.getCategoriesByUserId(USER_ID);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Food", result.get(0).name());
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {
        @Test
        @DisplayName("should create category successfully without parent")
        void shouldCreateSuccessfully() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenReturn(1);

            CategoryCreateRequest request = CategoryCreateRequest.builder().name("New").userId(USER_ID).build();

            // Act
            int result = categoryService.createCategory(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRepository).insert(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("should create category successfully with parent owned by user")
        void shouldCreateWithParent() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Category parent = Category.builder().id(PARENT_ID).userId(USER_ID).build();
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenReturn(1);

            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .name("Child")
                    .userId(USER_ID)
                    .parentId(PARENT_ID)
                    .build();

            // Act
            int result = categoryService.createCategory(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRepository).insert(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("should create category successfully when parentId is 0 (treated as no parent)")
        void shouldCreateWithParentIdZero() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenReturn(1);

            CategoryCreateRequest request = CategoryCreateRequest.builder().name("New").userId(USER_ID).parentId(0L).build();

            // Act
            int result = categoryService.createCategory(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRepository).insert(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(USER_ID, CategoryCreateRequest.builder().build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if parent category not found")
        void shouldThrowOnParentNotFound() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

            CategoryCreateRequest request = CategoryCreateRequest.builder().userId(USER_ID).parentId(PARENT_ID).build();

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own parent category")
        void shouldThrowOnParentAccessDenied() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Category parent = Category.builder().id(PARENT_ID).userId(999L).build();
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            CategoryCreateRequest request = CategoryCreateRequest.builder().userId(USER_ID).parentId(PARENT_ID).build();

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> categoryService.createCategory(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {
        @Test
        @DisplayName("should update category successfully if owned")
        void shouldUpdateSuccessfully() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.update(any(CategoryUpdateRequest.class))).thenReturn(1);

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).userId(USER_ID).name("Updated").build();

            // Act
            int result = categoryService.updateCategory(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRepository).update(any(CategoryUpdateRequest.class));
        }

        @Test
        @DisplayName("should update category successfully with a valid parent")
        void shouldUpdateWithParent() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            Category parent = Category.builder().id(PARENT_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(categoryRepository.update(any(CategoryUpdateRequest.class))).thenReturn(1);

            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(CATEGORY_ID).userId(USER_ID).parentId(PARENT_ID).name("Updated").build();

            // Act
            int result = categoryService.updateCategory(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRepository).update(any(CategoryUpdateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if category not found")
        void shouldThrowOnNotFound() {
            // Arrange
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(USER_ID, CategoryUpdateRequest.builder().id(CATEGORY_ID).build()));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own category")
        void shouldThrowOnAccessDenied() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(999L).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(USER_ID, CategoryUpdateRequest.builder().id(CATEGORY_ID).build()));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if parent category ID is zero")
        void shouldThrowOnParentIdZero() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(0L).build();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if category is its own parent")
        void shouldThrowOnSelfParenting() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(CATEGORY_ID).build();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if parent category not found during update")
        void shouldThrowOnParentNotFound() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(PARENT_ID).build();

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own parent category during update")
        void shouldThrowOnParentAccessDenied() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            Category parent = Category.builder().id(PARENT_ID).userId(999L).build();
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(PARENT_ID).build();

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {
        @Test
        @DisplayName("should delete category if owned and has no transactions")
        void shouldDeleteSuccessfully() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
            when(categoryRepository.delete(category)).thenReturn(1);

            // Act
            int result = categoryService.deleteCategory(USER_ID, CATEGORY_ID);

            // Assert
            assertEquals(1, result);
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if not owned")
        void shouldThrowOnAccessDenied() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(999L).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
        }

        @Test
        @DisplayName("should throw IllegalStateException if category has transactions")
        void shouldThrowOnExistingTransactions() {
            // Arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(5L);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if category not found")
        void shouldThrowOnNotFound() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
        }
    }

    @Nested
    @DisplayName("getCategoriesGrouped")
    class GetCategoriesGroupedTests {
        @Test
        @DisplayName("should return grouped categories")
        void shouldReturnGrouped() {
            // Arrange
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // Act
            List<CategoryDto> result = categoryService.getCategoriesByUserId(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
            verify(categoryRepository).findByUserId(USER_ID);
        }
    }

    @Nested
    @DisplayName("getChildCategories")
    class GetChildCategoriesTests {
        @Test
        @DisplayName("should return child categories")
        void shouldReturnChildren() {
            // Arrange
            when(categoryRepository.findAllSubCategories(USER_ID)).thenReturn(Collections.emptyList());

            // Act
            List<CategoryDto> result = categoryService.getChildCategories(USER_ID);

            // Assert
            assertNotNull(result);
            verify(categoryRepository).findAllSubCategories(USER_ID);
        }
    }
}
