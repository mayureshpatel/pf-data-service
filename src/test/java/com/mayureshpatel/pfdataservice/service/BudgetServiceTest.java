package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("BudgetService unit tests")
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long BUDGET_ID = 50L;
    private static final Long CATEGORY_ID = 20L;
    private static final Integer MONTH = 3;
    private static final Integer YEAR = 2025;

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

    private Budget buildBudget(Long budgetId, Long userId, Long categoryId, BigDecimal amount) {
        Budget budget = new Budget();

        budget.setId(budgetId);
        budget.setUser(buildUser(userId));
        budget.setCategory(buildCategory(categoryId, userId));
        budget.setAmount(amount);
        budget.setMonth(MONTH);
        budget.setYear(YEAR);
        budget.setAudit(new TableAudit());

        return budget;
    }

    private CategoryDto buildCategoryDto(Long categoryId) {
        return new CategoryDto(categoryId, null, "Groceries", CategoryType.EXPENSE, null, null);
    }

    private BudgetDto buildBudgetDto(Long categoryId, BigDecimal amount, Integer month, Integer year) {
        return BudgetDto.builder()
                .category(buildCategoryDto(categoryId))
                .amount(amount)
                .month(month)
                .year(year)
                .build();
    }

    @Nested
    class GetBudgetsTest {

        @Test
        @DisplayName("should return mapped DTOs for the given user, month, and year")
        void getBudgets_happyPath_returnsMappedDtos() {
            // arrange
            Budget budget1 = buildBudget(50L, USER_ID, CATEGORY_ID, new BigDecimal("300.00"));
            Budget budget2 = buildBudget(51L, USER_ID, 21L, new BigDecimal("150.00"));

            when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(USER_ID, MONTH, YEAR))
                    .thenReturn(List.of(budget1, budget2));

            // act
            List<BudgetDto> result = budgetService.getBudgets(USER_ID, MONTH, YEAR);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(BudgetDto::amount)
                    .containsExactlyInAnyOrder(new BigDecimal("300.00"), new BigDecimal("150.00"));
            verify(budgetRepository).findByUserIdAndMonthAndYearAndDeletedAtIsNull(USER_ID, MONTH, YEAR);
        }

        @Test
        @DisplayName("should return empty list when no budgets exist for the given user, month, and year")
        void getBudgets_noBudgets_returnsEmptyList() {
            // arrange
            when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(USER_ID, MONTH, YEAR))
                    .thenReturn(List.of());

            // act
            List<BudgetDto> result = budgetService.getBudgets(USER_ID, MONTH, YEAR);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetAllBudgetsTest {

        @Test
        @DisplayName("should return all non-deleted budgets ordered by year and month descending")
        void getAllBudgets_happyPath_returnsMappedDtosOrdered() {
            // arrange
            Budget budget1 = buildBudget(50L, USER_ID, CATEGORY_ID, new BigDecimal("200.00"));
            budget1.setYear(2025);
            budget1.setMonth(3);
            Budget budget2 = buildBudget(51L, USER_ID, CATEGORY_ID, new BigDecimal("180.00"));
            budget2.setYear(2025);
            budget2.setMonth(2);

            when(budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_ID))
                    .thenReturn(List.of(budget1, budget2));

            // act
            List<BudgetDto> result = budgetService.getAllBudgets(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).month()).isEqualTo(3);
            assertThat(result.get(1).month()).isEqualTo(2);
            verify(budgetRepository).findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_ID);
        }
    }

    @Nested
    class GetBudgetStatusTest {

        @Test
        @DisplayName("should delegate to repository and return status DTOs directly")
        void getBudgetStatus_happyPath_returnsStatusDtos() {
            // arrange
            Category category = buildCategory(CATEGORY_ID, USER_ID);
            BudgetStatusDto status = BudgetStatusDto.builder()
                    .category(category)
                    .budgetedAmount(new BigDecimal("300.00"))
                    .spentAmount(new BigDecimal("220.00"))
                    .remainingAmount(new BigDecimal("80.00"))
                    .percentageUsed(73.33)
                    .build();

            when(budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_ID, MONTH, YEAR))
                    .thenReturn(List.of(status));

            // act
            List<BudgetStatusDto> result = budgetService.getBudgetStatus(USER_ID, MONTH, YEAR);

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).budgetedAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(result.get(0).spentAmount()).isEqualByComparingTo(new BigDecimal("220.00"));
            assertThat(result.get(0).remainingAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(result.get(0).percentageUsed()).isEqualTo(73.33);
            verify(budgetRepository).findBudgetStatusByUserIdAndMonthAndYear(USER_ID, MONTH, YEAR);
        }

        @Test
        @DisplayName("should return empty list when no budgets have status data")
        void getBudgetStatus_noBudgets_returnsEmptyList() {
            // arrange
            when(budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_ID, MONTH, YEAR))
                    .thenReturn(List.of());

            // act
            List<BudgetStatusDto> result = budgetService.getBudgetStatus(USER_ID, MONTH, YEAR);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SaveBudgetTest {

        @Test
        @DisplayName("should create a new budget when none exists for the user/category/month/year combination")
        void save_noBudgetExists_createsAndSavesNewBudget() {
            // arrange
            User user = buildUser(USER_ID);
            Category category = buildCategory(CATEGORY_ID, USER_ID);
            BudgetDto dto = buildBudgetDto(CATEGORY_ID, new BigDecimal("400.00"), MONTH, YEAR);

            Budget savedBudget = buildBudget(BUDGET_ID, USER_ID, CATEGORY_ID, new BigDecimal("400.00"));

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                    USER_ID, CATEGORY_ID, MONTH, YEAR)).thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

            // act
            BudgetDto result = budgetService.save(USER_ID, dto);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(BUDGET_ID);
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("400.00"));

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetRepository).save(captor.capture());
            Budget captured = captor.getValue();
            assertThat(captured.getUser().getId()).isEqualTo(USER_ID);
            assertThat(captured.getCategory().getId()).isEqualTo(CATEGORY_ID);
            assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(captured.getMonth()).isEqualTo(MONTH);
            assertThat(captured.getYear()).isEqualTo(YEAR);
        }

        @Test
        @DisplayName("should update the amount of an existing budget when one already exists for the period")
        void save_budgetAlreadyExists_updatesExistingBudgetAmount() {
            // arrange
            User user = buildUser(USER_ID);
            Category category = buildCategory(CATEGORY_ID, USER_ID);
            BudgetDto dto = buildBudgetDto(CATEGORY_ID, new BigDecimal("500.00"), MONTH, YEAR);

            Budget existingBudget = buildBudget(BUDGET_ID, USER_ID, CATEGORY_ID, new BigDecimal("300.00"));
            Budget savedBudget = buildBudget(BUDGET_ID, USER_ID, CATEGORY_ID, new BigDecimal("500.00"));

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                    USER_ID, CATEGORY_ID, MONTH, YEAR)).thenReturn(Optional.of(existingBudget));
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

            // act
            BudgetDto result = budgetService.save(USER_ID, dto);

            // assert
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("500.00"));

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetRepository).save(captor.capture());
            // The same existing budget instance should be passed in with updated amount
            assertThat(captor.getValue().getId()).isEqualTo(BUDGET_ID);
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user is not found")
        void save_userNotFound_throwsResourceNotFoundException() {
            // arrange
            BudgetDto dto = buildBudgetDto(CATEGORY_ID, new BigDecimal("300.00"), MONTH, YEAR);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> budgetService.save(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category is not found")
        void save_categoryNotFound_throwsResourceNotFoundException() {
            // arrange
            User user = buildUser(USER_ID);
            BudgetDto dto = buildBudgetDto(CATEGORY_ID, new BigDecimal("300.00"), MONTH, YEAR);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> budgetService.save(USER_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");

            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when category is owned by a different user")
        void save_categoryOwnedByDifferentUser_throwsAccessDeniedException() {
            // arrange
            User user = buildUser(USER_ID);
            Category foreignCategory = buildCategory(CATEGORY_ID, OTHER_USER_ID);
            BudgetDto dto = buildBudgetDto(CATEGORY_ID, new BigDecimal("300.00"), MONTH, YEAR);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory));

            // act & assert
            assertThatThrownBy(() -> budgetService.save(USER_ID, dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied to category");

            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteTest {

        @Test
        @DisplayName("should soft-delete budget by setting deletedAt on audit and saving")
        void delete_happyPath_softDeletesBudget() {
            // arrange
            Budget budget = buildBudget(BUDGET_ID, USER_ID, CATEGORY_ID, new BigDecimal("300.00"));

            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));
            when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

            // act
            budgetService.delete(USER_ID, BUDGET_ID);

            // assert
            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetRepository).save(captor.capture());
            Budget captured = captor.getValue();
            assertThat(captured.getAudit().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when budget is not found")
        void delete_budgetNotFound_throwsResourceNotFoundException() {
            // arrange
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> budgetService.delete(USER_ID, BUDGET_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Budget not found");

            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when budget is owned by a different user")
        void delete_budgetOwnedByDifferentUser_throwsAccessDeniedException() {
            // arrange
            Budget budget = buildBudget(BUDGET_ID, OTHER_USER_ID, CATEGORY_ID, new BigDecimal("300.00"));
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));

            // act & assert
            assertThatThrownBy(() -> budgetService.delete(USER_ID, BUDGET_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(budgetRepository, never()).save(any());
        }
    }
}
