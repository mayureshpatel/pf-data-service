package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryRuleService Unit Tests")
class CategoryRuleServiceTest {

    @Mock
    private CategoryRuleRepository categoryRuleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionCategorizer categorizer;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryRuleService ruleService;

    private static final Long USER_ID = 1L;
    private static final Long RULE_ID = 100L;
    private static final Long CATEGORY_ID = 50L;

    @Nested
    @DisplayName("getRules")
    class GetRulesTests {
        @Test
        @DisplayName("should return list of rule DTOs")
        void shouldReturnRules() {
            // Arrange
            CategoryRule rule = CategoryRule.builder().id(RULE_ID).keyword("Amazon").build();
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));

            // Act
            List<CategoryRuleDto> result = ruleService.getRules(USER_ID);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Amazon", result.get(0).keyword());
        }
    }

    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {
        @Test
        @DisplayName("should create rule successfully")
        void shouldCreate() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRuleRepository.insert(any())).thenReturn(1);

            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .categoryId(CATEGORY_ID)
                    .keyword("Amazon")
                    .priority(1)
                    .build();

            // Act
            int result = ruleService.createRule(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRuleRepository).insert(argThat(r -> r.getKeyword().equals("Amazon")));
        }

        @Test
        @DisplayName("should create with default priority if not provided")
        void shouldCreateWithDefaultPriority() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRuleRepository.insert(any())).thenReturn(1);

            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .categoryId(CATEGORY_ID)
                    .keyword("Amazon")
                    .priority(null)
                    .build();

            // Act
            ruleService.createRule(USER_ID, request);

            // Assert
            verify(categoryRuleRepository).insert(argThat(r -> r.getPriority() == 0));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> ruleService.createRule(USER_ID, CategoryRuleCreateRequest.builder().build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if category not found")
        void shouldThrowOnCategoryNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());
            CategoryRuleCreateRequest request = CategoryRuleCreateRequest.builder()
                    .userId(USER_ID)
                    .categoryId(CATEGORY_ID)
                    .build();
            assertThrows(ResourceNotFoundException.class, () -> ruleService.createRule(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("updateRule")
    class UpdateRuleTests {
        @Test
        @DisplayName("should update rule successfully")
        void shouldUpdate() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            CategoryRule rule = CategoryRule.builder().id(RULE_ID).user(user).build();
            Category category = Category.builder().id(CATEGORY_ID).userId(USER_ID).build();

            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRuleRepository.save(any())).thenReturn(1);
            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(RULE_ID)
                    .keyword("NewKW")
                    .categoryId(CATEGORY_ID)
                    .priority(5)
                    .build();

            // Act
            int result = ruleService.updateRule(USER_ID, request);

            // Assert
            assertEquals(1, result);
            verify(categoryRuleRepository).save(argThat(r -> r.getKeyword().equals("NewKW") && r.getPriority() == 5));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if rule not found during update")
        void shouldThrowOnRuleNotFoundDuringUpdate() {
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> ruleService.updateRule(USER_ID, CategoryRuleUpdateRequest.builder().id(RULE_ID).build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if category not found during update")
        void shouldThrowOnCategoryNotFoundDuringUpdate() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            CategoryRule rule = CategoryRule.builder().id(RULE_ID).user(user).build();
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
            when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

            CategoryRuleUpdateRequest request = CategoryRuleUpdateRequest.builder()
                    .id(RULE_ID).categoryId(CATEGORY_ID).build();

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> ruleService.updateRule(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own rule")
        void shouldThrowOnOwnership() {
            User otherUser = User.builder().id(999L).build();
            CategoryRule rule = CategoryRule.builder().id(RULE_ID).user(otherUser).build();
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            assertThrows(AccessDeniedException.class, () -> ruleService.updateRule(USER_ID, CategoryRuleUpdateRequest.builder().id(RULE_ID).build()));
        }
    }

    @Nested
    @DisplayName("deleteRule")
    class DeleteRuleTests {
        @Test
        @DisplayName("should delete rule successfully")
        void shouldDelete() {
            // Arrange
            User user = User.builder().id(USER_ID).build();
            CategoryRule rule = CategoryRule.builder().id(RULE_ID).user(user).build();
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            // Act
            ruleService.deleteRule(USER_ID, RULE_ID);

            // Assert
            verify(categoryRuleRepository).deleteById(RULE_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if rule not found during delete")
        void shouldThrowOnRuleNotFoundDuringDelete() {
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> ruleService.deleteRule(USER_ID, RULE_ID));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own rule during delete")
        void shouldThrowOnOwnershipDuringDelete() {
            User otherUser = User.builder().id(999L).build();
            CategoryRule rule = CategoryRule.builder().id(RULE_ID).user(otherUser).build();
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            assertThrows(AccessDeniedException.class, () -> ruleService.deleteRule(USER_ID, RULE_ID));
        }
    }

    @Nested
    @DisplayName("previewApply")
    class PreviewApplyTests {
        @Test
        @DisplayName("should return previews for uncategorized transactions matching rules")
        void shouldReturnPreviews() {
            // Arrange
            Transaction t1 = Transaction.builder().description("Amazon").category(null).type(TransactionType.EXPENSE).build();
            Transaction tAlready = Transaction.builder().description("AlreadyCat").category(Category.builder().build()).build();
            Category targetCat = Category.builder().id(CATEGORY_ID).name("Shopping").build();

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(targetCat));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(t1, tAlready));
            when(categorizer.guessCategory(eq(t1), anyList(), anyList())).thenReturn(CATEGORY_ID);

            // Act
            List<RuleChangePreviewDto> result = ruleService.previewApply(USER_ID);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Amazon", result.get(0).description());
            assertEquals("Shopping", result.get(0).newValue());
        }

        @Test
        @DisplayName("should handle null guess or missing category in map")
        void shouldHandleNoMatch() {
            // Arrange
            Transaction t1 = Transaction.builder().description("Unknown").category(null).type(TransactionType.EXPENSE).build();
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(t1));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(null);

            // Act
            List<RuleChangePreviewDto> result = ruleService.previewApply(USER_ID);

            // Assert
            assertTrue(result.isEmpty());

            // Case: Guessed ID not in category map
            reset(categorizer);
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(999L);
            result = ruleService.previewApply(USER_ID);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should handle duplicate categories in map merge")
        void shouldHandleDuplicateCategories() {
            // Arrange
            Transaction t1 = Transaction.builder().description("Target").category(null).type(TransactionType.EXPENSE).build();
            Category c1 = Category.builder().id(CATEGORY_ID).name("Cat1").build();
            Category c2 = Category.builder().id(CATEGORY_ID).name("Cat2").build();

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(c1, c2));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(t1));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(CATEGORY_ID);

            // Act
            List<RuleChangePreviewDto> result = ruleService.previewApply(USER_ID);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Cat1", result.get(0).newValue());
        }
    }

    @Nested
    @DisplayName("applyRules")
    class ApplyRulesTests {
        @Test
        @DisplayName("should apply matching categories to transactions and persist")
        void shouldApplyRules() {
            // Arrange
            Transaction t1 = Transaction.builder().id(1L).description("Target").category(null).type(TransactionType.EXPENSE).build();
            Transaction tAlready = Transaction.builder().id(2L).description("Already").category(Category.builder().id(10L).build()).type(TransactionType.EXPENSE).build();
            Category targetCat = Category.builder().id(CATEGORY_ID).build();

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(targetCat));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(t1, tAlready));
            when(categorizer.guessCategory(eq(t1), anyList(), anyList())).thenReturn(CATEGORY_ID);

            // Act
            int result = ruleService.applyRules(USER_ID);

            // Assert
            assertEquals(1, result);
            verify(transactionRepository).updateAll(eq(USER_ID), argThat(list -> list.size() == 1 && list.get(0).getCategory().getId().equals(CATEGORY_ID)));
        }

        @Test
        @DisplayName("should skip transaction if guess is null or category not in map during apply")
        void shouldSkipOnNoMatch() {
            // Arrange
            Transaction t1 = Transaction.builder().id(1L).description("Target").category(null).type(TransactionType.EXPENSE).build();
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(t1));

            // Case 1: guess is null
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(null);
            assertEquals(0, ruleService.applyRules(USER_ID));

            // Case 2: matched category is null
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(999L);
            assertEquals(0, ruleService.applyRules(USER_ID));

            verify(transactionRepository, never()).updateAll(eq(USER_ID), anyList());
        }

        @Test
        @DisplayName("should handle duplicate categories in applyRules map merge")
        void shouldHandleDuplicateCategoriesInApply() {
            // Arrange
            Transaction t1 = Transaction.builder().id(1L).description("Target").category(null).type(TransactionType.EXPENSE).build();
            Category c1 = Category.builder().id(CATEGORY_ID).name("Cat1").build();
            Category c2 = Category.builder().id(CATEGORY_ID).name("Cat2").build();

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(c1, c2));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(t1));
            when(categorizer.guessCategory(any(), anyList(), anyList())).thenReturn(CATEGORY_ID);

            // Act
            int result = ruleService.applyRules(USER_ID);

            // Assert
            assertEquals(1, result);
            verify(transactionRepository).updateAll(eq(USER_ID), anyList());
        }

        @Test
        @DisplayName("should return 0 and not call updateAll if no matches")
        void shouldHandleNoUpdates() {
            // Arrange
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // Act
            int result = ruleService.applyRules(USER_ID);

            // Assert
            assertEquals(0, result);
            verify(transactionRepository, never()).updateAll(eq(USER_ID), anyList());
        }
    }
}
