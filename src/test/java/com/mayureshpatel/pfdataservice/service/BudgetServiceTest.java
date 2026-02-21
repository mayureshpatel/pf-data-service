package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private BudgetDto testBudgetDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Groceries");

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);
        testBudget.setAmount(new BigDecimal("500.00"));
        testBudget.setMonth(1);
        testBudget.setYear(2024);

        testBudgetDto = new BudgetDto(
                1L, 1L, "Groceries", new BigDecimal("500.00"), 1, 2024);
    }

    @Test
    void save() {
        // Given
        BudgetDto createDto = new BudgetDto(
                null, 1L, null, new BigDecimal("500.00"), 1, 2024);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                1L, 1L, 1, 2024)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // When
        BudgetDto result = budgetService.save(1L, createDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void save() {
        // Given
        BudgetDto updateDto = new BudgetDto(
                null, 1L, null, new BigDecimal("750.00"), 1, 2024);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                1L, 1L, 1, 2024)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // When
        BudgetDto result = budgetService.save(1L, updateDto);

        // Then
        assertThat(result).isNotNull();
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void save_UserNotFound_ShouldThrowException() {
        // Given
        BudgetDto createDto = new BudgetDto(
                null, 1L, null, new BigDecimal("500.00"), 1, 2024);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> budgetService.save(1L, createDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void save_CategoryNotFound_ShouldThrowException() {
        // Given
        BudgetDto createDto = new BudgetDto(
                null, 1L, null, new BigDecimal("500.00"), 1, 2024);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> budgetService.save(1L, createDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBudgets_ShouldReturnBudgetsForMonth() {
        // Given
        when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(1L, 1, 2024))
                .thenReturn(List.of(testBudget));

        // When
        List<BudgetDto> result = budgetService.getBudgets(1L, 1, 2024);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void getAllBudgets_ShouldReturnAllBudgets() {
        // Given
        when(budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(1L))
                .thenReturn(List.of(testBudget));

        // When
        List<BudgetDto> result = budgetService.getAllBudgets(1L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void deleteBudget_ValidId_ShouldDelete() {
        // Given
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        // When
        budgetService.delete(1L, 1L);

        // Then
        verify(budgetRepository).delete(testBudget);
    }

    @Test
    void deleteBudget_NotFound_ShouldThrowException() {
        // Given
        when(budgetRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> budgetService.delete(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_WrongUser_ShouldThrowException() {
        // Given
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        // When/Then
        assertThatThrownBy(() -> budgetService.delete(999L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to user");
    }
}
