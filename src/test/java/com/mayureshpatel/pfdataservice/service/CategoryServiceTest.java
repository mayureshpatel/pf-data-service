package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
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
    @Mock
    private CategoryRuleRepository categoryRuleRepository;
    @Mock
    private BudgetRepository budgetRepository;

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
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).name("Food").build();
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(category));

            // act
            List<CategoryDto> result = categoryService.getCategoriesByUserId(USER_ID);

            // assert & verify
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
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenReturn(1);

            CategoryCreateRequest request = CategoryCreateRequest.builder().name("New").userId(USER_ID).build();

            // act
            int result = categoryService.createCategory(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(categoryRepository).insert(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("should create category successfully with parent owned by user")
        void shouldCreateWithParent() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Category parent = Category.builder().id(PARENT_ID).userId(USER_ID).build();
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenReturn(1);

            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .name("Child")
                    .userId(USER_ID)
                    .parentId(PARENT_ID)
                    .build();

            // act
            int result = categoryService.createCategory(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(categoryRepository).insert(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("should create category successfully when parentId is 0 (treated as no parent)")
        void shouldCreateWithParentIdZero() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenReturn(1);

            CategoryCreateRequest request = CategoryCreateRequest.builder().name("New").userId(USER_ID).parentId(0L).build();

            // act
            int result = categoryService.createCategory(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(categoryRepository).insert(any(CategoryCreateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(USER_ID, CategoryCreateRequest.builder().build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if parent category not found")
        void shouldThrowOnParentNotFound() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

            CategoryCreateRequest request = CategoryCreateRequest.builder().userId(USER_ID).parentId(PARENT_ID).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own parent category")
        void shouldThrowOnParentAccessDenied() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            Category parent = Category.builder().id(PARENT_ID).userId(999L).build();
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            CategoryCreateRequest request = CategoryCreateRequest.builder().userId(USER_ID).parentId(PARENT_ID).build();

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> categoryService.createCategory(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {
        @Test
        @DisplayName("should update category successfully if owned")
        void shouldUpdateSuccessfully() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.update(any(CategoryUpdateRequest.class))).thenReturn(1);

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).userId(USER_ID).name("Updated").build();

            // act
            int result = categoryService.updateCategory(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(categoryRepository).update(any(CategoryUpdateRequest.class));
        }

        @Test
        @DisplayName("should update category successfully with a valid parent")
        void shouldUpdateWithParent() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            Category parent = Category.builder().id(PARENT_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(categoryRepository.update(any(CategoryUpdateRequest.class))).thenReturn(1);

            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(CATEGORY_ID).userId(USER_ID).parentId(PARENT_ID).name("Updated").build();

            // act
            int result = categoryService.updateCategory(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(categoryRepository).update(any(CategoryUpdateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if category not found")
        void shouldThrowOnNotFound() {
            // arrange
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(USER_ID, CategoryUpdateRequest.builder().id(CATEGORY_ID).build()));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own category")
        void shouldThrowOnAccessDenied() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(999L).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(USER_ID, CategoryUpdateRequest.builder().id(CATEGORY_ID).build()));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if parent category ID is zero")
        void shouldThrowOnParentIdZero() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(0L).build();

            // act & assert & verify
            assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if category is its own parent")
        void shouldThrowOnSelfParenting() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(CATEGORY_ID).build();

            // act & assert & verify
            assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if parent category not found during update")
        void shouldThrowOnParentNotFound() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(PARENT_ID).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own parent category during update")
        void shouldThrowOnParentAccessDenied() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            Category parent = Category.builder().id(PARENT_ID).userId(999L).build();
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            CategoryUpdateRequest request = CategoryUpdateRequest.builder().id(CATEGORY_ID).parentId(PARENT_ID).build();

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {
        @Test
        @DisplayName("should delete category if owned and has no transactions, subcategories, rules, or budgets")
        void shouldDeleteSuccessfully() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentId(CATEGORY_ID)).thenReturn(0L);
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
            when(categoryRuleRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
            when(budgetRepository.countByCategoryIdAndDeletedAtIsNull(CATEGORY_ID)).thenReturn(0L);
            when(categoryRepository.delete(category)).thenReturn(1);

            // act
            int result = categoryService.deleteCategory(USER_ID, CATEGORY_ID);

            // assert & verify
            assertEquals(1, result);
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if not owned")
        void shouldThrowOnAccessDenied() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(999L).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
        }

        @Test
        @DisplayName("should throw IllegalStateException if category has transactions")
        void shouldThrowOnExistingTransactions() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentId(CATEGORY_ID)).thenReturn(0L);
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(5L);

            // act & assert & verify
            assertThrows(IllegalStateException.class, () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
        }

        @Test
        @DisplayName("should throw IllegalStateException if category has subcategories (PF-191)")
        void shouldThrowOnExistingSubcategories() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentId(CATEGORY_ID)).thenReturn(2L);

            // act & assert & verify
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
            assertTrue(ex.getMessage().contains("subcategor"));
            // Should short-circuit before ever checking transactions/rules/budgets on this category.
            verify(transactionRepository, org.mockito.Mockito.never()).countByCategoryId(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException if category has dependent category rules (PF-191)")
        void shouldThrowOnExistingCategoryRules() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentId(CATEGORY_ID)).thenReturn(0L);
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
            when(categoryRuleRepository.countByCategoryId(CATEGORY_ID)).thenReturn(1L);

            // act & assert & verify
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
            assertTrue(ex.getMessage().contains("rule"));
        }

        @Test
        @DisplayName("should throw IllegalStateException if category has a dependent budget (PF-191)")
        void shouldThrowOnExistingBudget() {
            // arrange
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentId(CATEGORY_ID)).thenReturn(0L);
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
            when(categoryRuleRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
            when(budgetRepository.countByCategoryIdAndDeletedAtIsNull(CATEGORY_ID)).thenReturn(1L);

            // act & assert & verify
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));
            assertTrue(ex.getMessage().contains("budget"));
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
            // arrange
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // act
            List<CategoryDto> result = categoryService.getCategoriesByUserId(USER_ID);

            // assert & verify
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
            // arrange
            when(categoryRepository.findAllSubCategories(USER_ID)).thenReturn(Collections.emptyList());

            // act
            List<CategoryDto> result = categoryService.getChildCategories(USER_ID);

            // assert & verify
            assertNotNull(result);
            verify(categoryRepository).findAllSubCategories(USER_ID);
        }
    }
}
