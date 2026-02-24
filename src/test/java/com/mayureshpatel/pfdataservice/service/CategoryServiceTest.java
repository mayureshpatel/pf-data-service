package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.CategoryDto;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService unit tests")
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
    private static final Long OTHER_USER_ID = 99L;
    private static final Long CATEGORY_ID = 20L;
    private static final Long PARENT_CATEGORY_ID = 5L;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser");
        return user;
    }

    private CategoryDto buildCategory(Long id, Long userId) {
        return buildCategory(id, userId, null);
    }

    private CategoryDto buildCategory(Long id, Long userId, CategoryDto parent) {
        CategoryDto category = new CategoryDto();
        category.setId(id);
        category.setUser(buildUser(userId));
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);
        category.setIconography(new Iconography("food-icon", "#FF0000"));
        category.setParent(parent);
        category.setAudit(new TableAudit());
        return category;
    }

    private com.mayureshpatel.pfdataservice.dto.category.CategoryDto buildCategoryDto(String name) {
        return new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(null, null, name, CategoryType.EXPENSE, null, new Iconography("icon", "#000000"));
    }

    private com.mayureshpatel.pfdataservice.dto.category.CategoryDto buildCategoryDtoWithParent(String name, Long parentId) {
        com.mayureshpatel.pfdataservice.dto.category.CategoryDto parentDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(parentId, null, "Parent", CategoryType.EXPENSE, null, null);
        return new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(null, null, name, CategoryType.EXPENSE, parentDto, new Iconography("icon", "#000000"));
    }

    @Nested
    class GetCategoriesByUserIdTest {

        @Test
        @DisplayName("should return mapped DTOs for all categories of the user")
        void getCategoriesByUserId_happyPath_returnsMappedDtos() {
            // Arrange
            CategoryDto cat1 = buildCategory(20L, USER_ID);
            cat1.setName("Food");
            CategoryDto cat2 = buildCategory(21L, USER_ID);
            cat2.setName("Transport");

            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat1, cat2));

            // Act
            List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto> result = categoryService.getCategoriesByUserId(USER_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(com.mayureshpatel.pfdataservice.dto.category.CategoryDto::name)
                    .containsExactlyInAnyOrder("Food", "Transport");
        }

        @Test
        @DisplayName("should return empty list when user has no categories")
        void getCategoriesByUserId_noCategories_returnsEmptyList() {
            // Arrange
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // Act
            List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto> result = categoryService.getCategoriesByUserId(USER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetCategoriesGroupedTest {

        @Test
        @DisplayName("should return a map where each category maps to itself as a list")
        void getCategoriesGrouped_multipleCategories_returnsGroupedMap() {
            // Arrange
            CategoryDto cat1 = buildCategory(20L, USER_ID);
            cat1.setName("Food");
            CategoryDto cat2 = buildCategory(21L, USER_ID);
            cat2.setName("Transport");

            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat1, cat2));

            // Act
            Map<com.mayureshpatel.pfdataservice.dto.category.CategoryDto, List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto>> result = categoryService.getCategoriesGrouped(USER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.keySet()).extracting(com.mayureshpatel.pfdataservice.dto.category.CategoryDto::name)
                    .containsExactlyInAnyOrder("Food", "Transport");
            // Each category key should map to a list containing itself
            for (Map.Entry<com.mayureshpatel.pfdataservice.dto.category.CategoryDto, List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto>> entry : result.entrySet()) {
                assertThat(entry.getValue())
                        .hasSize(1)
                        .first()
                        .extracting(com.mayureshpatel.pfdataservice.dto.category.CategoryDto::name)
                        .isEqualTo(entry.getKey().name());
            }
        }

        @Test
        @DisplayName("should return empty map when no categories exist")
        void getCategoriesGrouped_noCategories_returnsEmptyMap() {
            // Arrange
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // Act
            Map<com.mayureshpatel.pfdataservice.dto.category.CategoryDto, List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto>> result = categoryService.getCategoriesGrouped(USER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetChildCategoriesTest {

        @Test
        @DisplayName("should return mapped DTOs of all sub-categories")
        void getChildCategories_happyPath_returnsMappedSubCategories() {
            // Arrange
            CategoryDto parent = buildCategory(5L, USER_ID);
            parent.setName("Shopping");
            CategoryDto child = buildCategory(6L, USER_ID, parent);
            child.setName("Clothes");

            when(categoryRepository.findAllSubCategories(USER_ID)).thenReturn(List.of(child));

            // Act
            List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto> result = categoryService.getChildCategories(USER_ID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Clothes");
            assertThat(result.get(0).parent()).isNotNull();
            assertThat(result.get(0).parent().name()).isEqualTo("Shopping");
        }
    }

    @Nested
    class CreateCategoryTest {

        @Test
        @DisplayName("should save category without parent when parentId is null")
        void createCategory_withoutParent_savesAndReturnsDto() {
            // Arrange
            User user = buildUser(USER_ID);
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto dto = buildCategoryDto("Food");

            CategoryDto savedCategory = buildCategory(CATEGORY_ID, USER_ID);
            savedCategory.setName("Food");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.save(any(CategoryDto.class))).thenReturn(savedCategory);

            // Act
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto result = categoryService.createCategory(USER_ID, dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CATEGORY_ID);
            assertThat(result.name()).isEqualTo("Food");

            ArgumentCaptor<CategoryDto> captor = ArgumentCaptor.forClass(CategoryDto.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getParent()).isNull();
            assertThat(captor.getValue().getUser().getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should save category with parent when parentId is provided and parent is owned by same user")
        void createCategory_withValidParent_savesWithParentSet() {
            // Arrange
            User user = buildUser(USER_ID);
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto dto = buildCategoryDtoWithParent("Clothes", PARENT_CATEGORY_ID);

            CategoryDto parentCategory = buildCategory(PARENT_CATEGORY_ID, USER_ID);
            parentCategory.setName("Shopping");

            CategoryDto savedCategory = buildCategory(CATEGORY_ID, USER_ID, parentCategory);
            savedCategory.setName("Clothes");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(PARENT_CATEGORY_ID)).thenReturn(Optional.of(parentCategory));
            when(categoryRepository.save(any(CategoryDto.class))).thenReturn(savedCategory);

            // Act
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto result = categoryService.createCategory(USER_ID, dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.parent()).isNotNull();
            assertThat(result.parent().name()).isEqualTo("Shopping");

            ArgumentCaptor<CategoryDto> captor = ArgumentCaptor.forClass(CategoryDto.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getParent().getId()).isEqualTo(PARENT_CATEGORY_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user is not found")
        void createCategory_userNotFound_throwsResourceNotFoundException() {
            // Arrange
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto dto = buildCategoryDto("Food");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when parent category is not found")
        void createCategory_parentCategoryNotFound_throwsResourceNotFoundException() {
            // Arrange
            User user = buildUser(USER_ID);
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto dto = buildCategoryDtoWithParent("Clothes", PARENT_CATEGORY_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(PARENT_CATEGORY_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent category not found");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when parent category is owned by a different user")
        void createCategory_parentOwnedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            User user = buildUser(USER_ID);
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto dto = buildCategoryDtoWithParent("Clothes", PARENT_CATEGORY_ID);

            CategoryDto parentCategory = buildCategory(PARENT_CATEGORY_ID, OTHER_USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(PARENT_CATEGORY_ID)).thenReturn(Optional.of(parentCategory));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(USER_ID, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied to parent category");

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateCategoryTest {

        @Test
        @DisplayName("should update name, type, and iconography when category exists and is owned by user")
        void updateCategory_happyPath_updatesAndReturnsDto() {
            // Arrange
            CategoryDto existing = buildCategory(CATEGORY_ID, USER_ID);
            existing.setName("Old Name");

            com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(null, null, "New Name", CategoryType.INCOME, null,
                    new Iconography("new-icon", "#FFFFFF"));

            CategoryDto saved = buildCategory(CATEGORY_ID, USER_ID);
            saved.setName("New Name");
            saved.setType(CategoryType.INCOME);
            saved.setIconography(new Iconography("new-icon", "#FFFFFF"));

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(CategoryDto.class))).thenReturn(saved);

            // Act
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto result = categoryService.updateCategory(USER_ID, CATEGORY_ID, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("New Name");

            ArgumentCaptor<CategoryDto> captor = ArgumentCaptor.forClass(CategoryDto.class);
            verify(categoryRepository).save(captor.capture());
            CategoryDto captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("New Name");
            assertThat(captured.getType()).isEqualTo(CategoryType.INCOME);
            assertThat(captured.getIconography().getIcon()).isEqualTo("new-icon");
            assertThat(captured.getIconography().getColor()).isEqualTo("#FFFFFF");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category does not exist")
        void updateCategory_categoryNotFound_throwsResourceNotFoundException() {
            // Arrange
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = buildCategoryDto("New Name");
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(USER_ID, CATEGORY_ID, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when category is owned by a different user")
        void updateCategory_categoryOwnedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            CategoryDto existing = buildCategory(CATEGORY_ID, OTHER_USER_ID);
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = buildCategoryDto("New Name");

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(USER_ID, CATEGORY_ID, updateDto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when category references itself as parent")
        void updateCategory_selfReferencingParent_throwsIllegalArgumentException() {
            // Arrange
            CategoryDto existing = buildCategory(CATEGORY_ID, USER_ID);
            // dto sets parentId = same as CATEGORY_ID
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto selfRefDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(null, null, "Self", CategoryType.EXPENSE,
                    new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(CATEGORY_ID, null, "Self", CategoryType.EXPENSE, null, null),
                    new Iconography("icon", "#000"));

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(USER_ID, CATEGORY_ID, selfRefDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category cannot be its own parent");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when new parent category is not found")
        void updateCategory_parentCategoryNotFound_throwsResourceNotFoundException() {
            // Arrange
            CategoryDto existing = buildCategory(CATEGORY_ID, USER_ID);
            com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(null, null, "New Name", CategoryType.EXPENSE,
                    new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(PARENT_CATEGORY_ID, null, "Parent", CategoryType.EXPENSE, null, null),
                    new Iconography("icon", "#000"));

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(PARENT_CATEGORY_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(USER_ID, CATEGORY_ID, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent category not found");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when new parent is owned by a different user")
        void updateCategory_parentOwnedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            CategoryDto existing = buildCategory(CATEGORY_ID, USER_ID);
            CategoryDto foreignParent = buildCategory(PARENT_CATEGORY_ID, OTHER_USER_ID);

            com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateDto = new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(null, null, "New Name", CategoryType.EXPENSE,
                    new com.mayureshpatel.pfdataservice.dto.category.CategoryDto(PARENT_CATEGORY_ID, null, "Foreign Parent", CategoryType.EXPENSE, null, null),
                    new Iconography("icon", "#000"));

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(PARENT_CATEGORY_ID)).thenReturn(Optional.of(foreignParent));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(USER_ID, CATEGORY_ID, updateDto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied to parent category");

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteCategoryTest {

        @Test
        @DisplayName("should delete category when owned by user and has no transactions")
        void deleteCategory_happyPath_deletesCategory() {
            // Arrange
            CategoryDto category = buildCategory(CATEGORY_ID, USER_ID);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(0L);

            // Act
            categoryService.deleteCategory(USER_ID, CATEGORY_ID);

            // Assert
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category does not exist")
        void deleteCategory_categoryNotFound_throwsResourceNotFoundException() {
            // Arrange
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");

            verify(categoryRepository, never()).delete(any(CategoryDto.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when category is owned by a different user")
        void deleteCategory_categoryOwnedByDifferentUser_throwsAccessDeniedException() {
            // Arrange
            CategoryDto category = buildCategory(CATEGORY_ID, OTHER_USER_ID);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(transactionRepository, never()).countByCategoryId(any());
            verify(categoryRepository, never()).delete(any(CategoryDto.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when category has associated transactions")
        void deleteCategory_hasTransactions_throwsIllegalStateException() {
            // Arrange
            CategoryDto category = buildCategory(CATEGORY_ID, USER_ID);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete category with associated transactions");

            verify(categoryRepository, never()).delete(any(CategoryDto.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when category has exactly one transaction")
        void deleteCategory_exactlyOneTransaction_throwsIllegalStateException() {
            // Arrange
            CategoryDto category = buildCategory(CATEGORY_ID, USER_ID);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(transactionRepository.countByCategoryId(CATEGORY_ID)).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(IllegalStateException.class);

            verify(categoryRepository, never()).delete(any(CategoryDto.class));
        }
    }
}
