package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.CategoryRuleDto;
import com.mayureshpatel.pfdataservice.dto.RuleChangePreviewDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.service.categorization.TransactionCategorizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    private User user;
    private CategoryRule rule;
    private CategoryRuleDto ruleDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        rule = new CategoryRule();
        rule.setId(10L);
        rule.setUser(user);
        rule.setKeyword("Starbucks");
        rule.setCategoryName("Dining");
        rule.setPriority(1);

        ruleDto = CategoryRuleDto.builder()
                .keyword("Starbucks")
                .categoryName("Dining")
                .priority(1)
                .build();
    }

    @Test
    void getRules_ShouldReturnListOfDtos() {
        // Given
        when(categoryRuleRepository.findByUserId(1L)).thenReturn(List.of(rule));

        // When
        List<CategoryRuleDto> result = categoryRuleService.getRules(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).keyword()).isEqualTo("Starbucks");
        verify(categoryRuleRepository).findByUserId(1L);
    }

    @Test
    void createRule_ShouldSaveAndReturnDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRuleRepository.save(any(CategoryRule.class))).thenReturn(rule);

        // When
        CategoryRuleDto result = categoryRuleService.createRule(1L, ruleDto);

        // Then
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.keyword()).isEqualTo("Starbucks");
        verify(userRepository).findById(1L);
        verify(categoryRuleRepository).save(any(CategoryRule.class));
    }

    @Test
    void updateRule_ShouldUpdateAndReturnDto() {
        // Given
        when(categoryRuleRepository.findById(10L)).thenReturn(Optional.of(rule));
        when(categoryRuleRepository.save(any(CategoryRule.class))).thenReturn(rule);

        // When
        CategoryRuleDto result = categoryRuleService.updateRule(10L, ruleDto);

        // Then
        assertThat(result.keyword()).isEqualTo("Starbucks");
        verify(categoryRuleRepository).findById(10L);
        verify(categoryRuleRepository).save(rule);
    }

    @Test
    void deleteRule_ShouldDeleteIfOwner() {
        // Given
        when(categoryRuleRepository.findById(10L)).thenReturn(Optional.of(rule));

        // When
        categoryRuleService.deleteRule(1L, 10L);

        // Then
        verify(categoryRuleRepository).deleteById(10L);
    }

    @Test
    void deleteRule_ShouldThrowExceptionIfNotOwner() {
        // Given
        when(categoryRuleRepository.findById(10L)).thenReturn(Optional.of(rule));

        // When & Then
        assertThatThrownBy(() -> categoryRuleService.deleteRule(99L, 10L))
                .isInstanceOf(AccessDeniedException.class);
        verify(categoryRuleRepository, never()).deleteById(anyLong());
    }

    @Test
    void previewApply_ShouldReturnList() {
        // Given
        Category category = new Category();
        category.setName("Dining");
        category.setId(50L);

        Transaction transaction = new Transaction();
        transaction.setDescription("STARBUCKS COFFEE");
        transaction.setCategory(null);

        when(categoryRuleRepository.findByUserId(1L)).thenReturn(List.of(rule));
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(category));
        when(transactionRepository.findByAccount_User_Id(1L)).thenReturn(List.of(transaction));
        when(categorizer.guessCategory(eq(transaction), anyList(), anyList())).thenReturn("Dining");

        // When
        List<RuleChangePreviewDto> previews = categoryRuleService.previewApply(1L);

        // Then
        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).newCategory()).isEqualTo("Dining");
    }

    @Test
    void applyRules_ShouldUpdateTransactionsAndReturnCount() {
        // Given
        Category category = new Category();
        category.setName("Dining");
        category.setId(50L);

        Transaction transaction = new Transaction();
        transaction.setDescription("STARBUCKS COFFEE");
        transaction.setCategory(null);

        when(categoryRuleRepository.findByUserId(1L)).thenReturn(List.of(rule));
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(category));
        when(transactionRepository.findByAccount_User_Id(1L)).thenReturn(List.of(transaction));
        when(categorizer.guessCategory(eq(transaction), anyList(), anyList())).thenReturn("Dining");

        // When
        int count = categoryRuleService.applyRules(1L);

        // Then
        assertThat(count).isEqualTo(1);
        assertThat(transaction.getCategory()).isEqualTo(category);
        verify(transactionRepository).saveAll(anyList());
    }
}
