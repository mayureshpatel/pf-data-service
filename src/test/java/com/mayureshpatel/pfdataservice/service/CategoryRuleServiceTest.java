package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryRuleService unit tests")
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
    private CategoryRuleService categoryRuleService;

    @Captor
    private ArgumentCaptor<List<Transaction>> transactionListCaptor;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long RULE_ID = 30L;
    private static final Long CATEGORY_ID = 20L;

    private User buildUser(Long id) {
        User user = new User();

        user.setId(id);
        user.setUsername("testuser");

        return user;
    }

    private Category buildCategory(Long id, Long userId) {
        Category category = new Category();

        category.setId(id);
        category.setUser(buildUser(userId));
        category.setName("Groceries");
        category.setType(CategoryType.EXPENSE);

        return category;
    }

    private CategoryRule buildRule(Long ruleId, Long userId, Long categoryId, String keyword, Integer priority) {
        TableAudit audit = new TableAudit();

        audit.setCreatedAt(OffsetDateTime.now());
        audit.setUpdatedAt(OffsetDateTime.now());

        return new CategoryRule(
                ruleId,
                keyword,
                buildCategory(categoryId, userId),
                priority,
                buildUser(userId),
                audit
        );
    }

    private CategoryDto buildCategoryDto(Long categoryId) {
        return new CategoryDto(categoryId, null, "Groceries", CategoryType.EXPENSE, null, null);
    }

    private CategoryRuleDto buildRuleDto(String keyword, Long categoryId, Integer priority) {
        return CategoryRuleDto.builder()
                .keyword(keyword)
                .category(buildCategoryDto(categoryId))
                .priority(priority)
                .build();
    }

    private Transaction buildTransaction(Long id, Category category, String description) {
        Transaction tx = new Transaction();

        tx.setId(id);
        tx.setCategory(category);
        tx.setDescription(description);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setType(TransactionType.EXPENSE);
        tx.setTransactionDate(OffsetDateTime.now());

        return tx;
    }

    @Nested
    class GetRulesTests {

        @Test
        @DisplayName("should return mapped DTOs for all rules belonging to the user")
        void getRules_happyPath_returnsMappedDtos() {
            // arrange
            CategoryRule rule1 = buildRule(30L, USER_ID, CATEGORY_ID, "amazon", 1);
            CategoryRule rule2 = buildRule(31L, USER_ID, CATEGORY_ID, "walmart", 2);

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule1, rule2));

            // act
            List<CategoryRuleDto> result = categoryRuleService.getRules(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CategoryRuleDto::keyword)
                    .containsExactlyInAnyOrder("amazon", "walmart");
            verify(categoryRuleRepository).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("should return empty list when no rules exist for the user")
        void getRules_noRules_returnsEmptyList() {
            // arrange
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // act
            List<CategoryRuleDto> result = categoryRuleService.getRules(USER_ID);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CreateRuleTests {

        @Test
        @DisplayName("should save rule with explicit priority and return mapped DTO")
        void createRule_withExplicitPriority_savesAndReturnsMappedDto() {
            // arrange
            User user = buildUser(USER_ID);
            CategoryRuleDto dto = buildRuleDto("amazon", CATEGORY_ID, 5);

            CategoryRule savedRule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 5);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRuleRepository.save(any(CategoryRule.class))).thenReturn(savedRule);

            // act
            CategoryRuleDto result = categoryRuleService.createRule(USER_ID, dto);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.keyword()).isEqualTo("amazon");
            assertThat(result.priority()).isEqualTo(5);

            ArgumentCaptor<CategoryRule> captor = ArgumentCaptor.forClass(CategoryRule.class);
            verify(categoryRuleRepository).save(captor.capture());
            CategoryRule captured = captor.getValue();
            assertThat(captured.getKeyword()).isEqualTo("amazon");
            assertThat(captured.getPriority()).isEqualTo(5);
            assertThat(captured.getUser().getId()).isEqualTo(USER_ID);
            assertThat(captured.getCategory().getId()).isEqualTo(CATEGORY_ID);
            assertThat(captured.getId()).isNull();
        }

        @Test
        @DisplayName("should default priority to 0 when DTO priority is null")
        void createRule_nullPriority_defaultsToZero() {
            // arrange
            User user = buildUser(USER_ID);
            CategoryRuleDto dto = buildRuleDto("walmart", CATEGORY_ID, null);

            CategoryRule savedRule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "walmart", 0);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRuleRepository.save(any(CategoryRule.class))).thenReturn(savedRule);

            // act
            CategoryRuleDto result = categoryRuleService.createRule(USER_ID, dto);

            // assert
            assertThat(result).isNotNull();
            ArgumentCaptor<CategoryRule> captor = ArgumentCaptor.forClass(CategoryRule.class);
            verify(categoryRuleRepository).save(captor.capture());
            assertThat(captor.getValue().getPriority()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user is not found")
        void createRule_userNotFound_throwsResourceNotFoundException() {
            // arrange
            CategoryRuleDto dto = buildRuleDto("amazon", CATEGORY_ID, 1);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> categoryRuleService.createRule(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(categoryRuleRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set audit timestamps on the created rule")
        void createRule_happyPath_setsAuditTimestamps() {
            // arrange
            User user = buildUser(USER_ID);
            CategoryRuleDto dto = buildRuleDto("target", CATEGORY_ID, 1);
            CategoryRule savedRule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "target", 1);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRuleRepository.save(any(CategoryRule.class))).thenReturn(savedRule);

            // act
            categoryRuleService.createRule(USER_ID, dto);

            // assert
            ArgumentCaptor<CategoryRule> captor = ArgumentCaptor.forClass(CategoryRule.class);
            verify(categoryRuleRepository).save(captor.capture());
            assertThat(captor.getValue().getAudit()).isNotNull();
            assertThat(captor.getValue().getAudit().getCreatedAt()).isNotNull();
            assertThat(captor.getValue().getAudit().getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class UpdateRuleTests {

        @Test
        @DisplayName("should update keyword, category, and priority when rule exists and user owns it")
        void updateRule_happyPath_updatesAndReturnsMappedDto() {
            // arrange
            CategoryRule existing = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "old-keyword", 1);
            CategoryRuleDto dto = buildRuleDto("new-keyword", CATEGORY_ID, 10);
            CategoryRule updatedRule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "new-keyword", 10);

            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(existing));
            when(categoryRuleRepository.save(any(CategoryRule.class))).thenReturn(updatedRule);

            // act
            CategoryRuleDto result = categoryRuleService.updateRule(USER_ID, RULE_ID, dto);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.keyword()).isEqualTo("new-keyword");
            assertThat(result.priority()).isEqualTo(10);

            ArgumentCaptor<CategoryRule> captor = ArgumentCaptor.forClass(CategoryRule.class);
            verify(categoryRuleRepository).save(captor.capture());
            assertThat(captor.getValue().getKeyword()).isEqualTo("new-keyword");
            assertThat(captor.getValue().getPriority()).isEqualTo(10);
            assertThat(captor.getValue().getAudit().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when rule does not exist")
        void updateRule_ruleNotFound_throwsResourceNotFoundException() {
            // arrange
            CategoryRuleDto dto = buildRuleDto("new-keyword", CATEGORY_ID, 10);
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> categoryRuleService.updateRule(USER_ID, RULE_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rule not found");

            verify(categoryRuleRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when rule is owned by a different user")
        void updateRule_ruleOwnedByDifferentUser_throwsAccessDeniedException() {
            // arrange
            CategoryRule existing = buildRule(RULE_ID, OTHER_USER_ID, CATEGORY_ID, "amazon", 1);
            CategoryRuleDto dto = buildRuleDto("new-keyword", CATEGORY_ID, 10);

            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(existing));

            // act & assert
            assertThatThrownBy(() -> categoryRuleService.updateRule(USER_ID, RULE_ID, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You do not own this rule");

            verify(categoryRuleRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteRuleTests {

        @Test
        @DisplayName("should call deleteById with the correct ruleId when rule is owned by the user")
        void deleteRule_happyPath_callsDeleteById() {
            // arrange
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            // act
            categoryRuleService.deleteRule(USER_ID, RULE_ID);

            // assert
            verify(categoryRuleRepository).deleteById(RULE_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when rule does not exist")
        void deleteRule_ruleNotFound_throwsResourceNotFoundException() {
            // arrange
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> categoryRuleService.deleteRule(USER_ID, RULE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rule not found");

            verify(categoryRuleRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when rule is owned by a different user")
        void deleteRule_ruleOwnedByDifferentUser_throwsAccessDeniedException() {
            // arrange
            CategoryRule rule = buildRule(RULE_ID, OTHER_USER_ID, CATEGORY_ID, "amazon", 1);
            when(categoryRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

            // act & assert
            assertThatThrownBy(() -> categoryRuleService.deleteRule(USER_ID, RULE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You do not own this rule");

            verify(categoryRuleRepository, never()).deleteById(any());
        }
    }

    @Nested
    class PreviewApplyTests {

        @Test
        @DisplayName("should skip already-categorized transactions and returns preview only for uncategorized ones")
        void previewApply_uncategorizedTransactionMatchesRule_returnsPreview() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            groceryCategory.setName("Groceries");

            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);

            Transaction uncategorized = buildTransaction(1L, null, "Amazon Purchase");
            Transaction alreadyCategorized = buildTransaction(2L, groceryCategory, "Walmart");

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(uncategorized, alreadyCategorized));
            when(categorizer.guessCategory(uncategorized, List.of(rule), List.of(groceryCategory)))
                    .thenReturn(CATEGORY_ID);

            // act
            List<RuleChangePreviewDto> previews = categoryRuleService.previewApply(USER_ID);

            // assert
            assertThat(previews).hasSize(1);
            RuleChangePreviewDto preview = previews.get(0);
            assertThat(preview.description()).isEqualTo("Amazon Purchase");
            assertThat(preview.oldValue()).isEqualTo("Uncategorized");
            assertThat(preview.newValue()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("should return empty list when all transactions are already categorized")
        void previewApply_allTransactionsCategorized_returnsEmptyList() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            Transaction categorized = buildTransaction(1L, groceryCategory, "Amazon Purchase");

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(categorized));

            // act
            List<RuleChangePreviewDto> previews = categoryRuleService.previewApply(USER_ID);

            // assert
            assertThat(previews).isEmpty();
            verify(categorizer, never()).guessCategory(any(), anyList(), anyList());
        }

        @Test
        @DisplayName("should skip uncategorized transaction when no rule matches (guessCategory returns null)")
        void previewApply_noRuleMatchesUncategorized_returnsEmptyList() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            Transaction uncategorized = buildTransaction(1L, null, "Random Vendor");

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(uncategorized));
            when(categorizer.guessCategory(uncategorized, List.of(rule), List.of(groceryCategory)))
                    .thenReturn(null);

            // act
            List<RuleChangePreviewDto> previews = categoryRuleService.previewApply(USER_ID);

            // assert
            assertThat(previews).isEmpty();
        }

        @Test
        @DisplayName("should skip uncategorized transaction when guessed category is not in category map")
        void previewApply_guessedCategoryNotInMap_returnsEmptyList() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            Transaction uncategorized = buildTransaction(1L, null, "Amazon Purchase");

            Long unmappedCategoryId = 9999L;

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(uncategorized));
            // Categorizer returns an id not in the category list
            when(categorizer.guessCategory(uncategorized, List.of(rule), List.of(groceryCategory)))
                    .thenReturn(unmappedCategoryId);

            // act
            List<RuleChangePreviewDto> previews = categoryRuleService.previewApply(USER_ID);

            // assert
            assertThat(previews).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when there are no transactions")
        void previewApply_noTransactions_returnsEmptyList() {
            // arrange
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // act
            List<RuleChangePreviewDto> previews = categoryRuleService.previewApply(USER_ID);

            // assert
            assertThat(previews).isEmpty();
        }
    }

    @Nested
    class ApplyRulesTests {

        @Test
        @DisplayName("should assign category to uncategorized transactions and save them; returns count")
        void applyRules_uncategorizedTransactionMatched_savesAndReturnsCount() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            groceryCategory.setName("Groceries");
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);

            Transaction uncategorized = buildTransaction(1L, null, "Amazon Purchase");

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(uncategorized));
            when(categorizer.guessCategory(uncategorized, List.of(rule), List.of(groceryCategory)))
                    .thenReturn(CATEGORY_ID);

            // act
            int count = categoryRuleService.applyRules(USER_ID);

            // assert
            assertThat(count).isEqualTo(1);
            assertThat(uncategorized.getCategory()).isEqualTo(groceryCategory);

            verify(transactionRepository).saveAll(transactionListCaptor.capture());
            List<Transaction> saved = transactionListCaptor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getCategory().getName()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("should not call saveAll when all transactions are already categorized")
        void applyRules_allCategorized_doesNotCallSaveAllReturnsZero() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            Transaction categorized = buildTransaction(1L, groceryCategory, "Amazon Purchase");

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(categorized));

            // act
            int count = categoryRuleService.applyRules(USER_ID);

            // assert
            assertThat(count).isEqualTo(0);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should not call saveAll when no rule matches uncategorized transactions")
        void applyRules_noRuleMatchesAnyTransaction_doesNotCallSaveAllReturnsZero() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            Transaction uncategorized = buildTransaction(1L, null, "Random Vendor");

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(uncategorized));
            when(categorizer.guessCategory(uncategorized, List.of(rule), List.of(groceryCategory)))
                    .thenReturn(null);

            // act
            int count = categoryRuleService.applyRules(USER_ID);

            // assert
            assertThat(count).isEqualTo(0);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should processes multiple transactions and return correct count of updated ones")
        void applyRules_multipleTransactions_returnsCorrectCount() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            Category diningCategory = buildCategory(21L, USER_ID);
            diningCategory.setName("Dining");

            CategoryRule rule1 = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            CategoryRule rule2 = buildRule(31L, USER_ID, 21L, "restaurant", 2);

            Transaction uncategorized1 = buildTransaction(1L, null, "Amazon Prime");
            Transaction uncategorized2 = buildTransaction(2L, null, "Local Restaurant");
            Transaction alreadyCategorized = buildTransaction(3L, groceryCategory, "Walmart");
            Transaction noMatch = buildTransaction(4L, null, "Unknown Vendor");

            List<CategoryRule> rules = List.of(rule1, rule2);
            List<Category> categories = List.of(groceryCategory, diningCategory);

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(rules);
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(categories);
            when(transactionRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(uncategorized1, uncategorized2, alreadyCategorized, noMatch));
            when(categorizer.guessCategory(uncategorized1, rules, categories)).thenReturn(CATEGORY_ID);
            when(categorizer.guessCategory(uncategorized2, rules, categories)).thenReturn(21L);
            when(categorizer.guessCategory(noMatch, rules, categories)).thenReturn(null);

            // act
            int count = categoryRuleService.applyRules(USER_ID);

            // assert
            assertThat(count).isEqualTo(2);
            assertThat(uncategorized1.getCategory().getId()).isEqualTo(CATEGORY_ID);
            assertThat(uncategorized2.getCategory().getId()).isEqualTo(21L);
            assertThat(alreadyCategorized.getCategory()).isNotNull(); // unchanged

            verify(transactionRepository).saveAll(transactionListCaptor.capture());
            List<Transaction> saved = transactionListCaptor.getValue();
            assertThat(saved).hasSize(2);
        }

        @Test
        @DisplayName("should skip uncategorized transaction when guessed category is not in category map")
        void applyRules_guessedCategoryNotInMap_skipsTransaction() {
            // arrange
            Category groceryCategory = buildCategory(CATEGORY_ID, USER_ID);
            CategoryRule rule = buildRule(RULE_ID, USER_ID, CATEGORY_ID, "amazon", 1);
            Transaction uncategorized = buildTransaction(1L, null, "Amazon Purchase");

            Long unmappedCategoryId = 9999L;

            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule));
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(groceryCategory));
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of(uncategorized));
            when(categorizer.guessCategory(uncategorized, List.of(rule), List.of(groceryCategory)))
                    .thenReturn(unmappedCategoryId);

            // act
            int count = categoryRuleService.applyRules(USER_ID);

            // assert
            assertThat(count).isEqualTo(0);
            assertThat(uncategorized.getCategory()).isNull();
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should not call saveAll when there are no transactions at all")
        void applyRules_noTransactions_doesNotCallSaveAllReturnsZero() {
            // arrange
            when(categoryRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(transactionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // act
            int count = categoryRuleService.applyRules(USER_ID);

            // assert
            assertThat(count).isEqualTo(0);
            verify(transactionRepository, never()).saveAll(anyList());
        }
    }
}
