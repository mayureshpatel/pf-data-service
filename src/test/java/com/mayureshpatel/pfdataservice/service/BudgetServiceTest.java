package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Unit Tests")
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
    private static final Long CATEGORY_ID = 10L;
    private static final Long BUDGET_ID = 100L;

    @Nested
    @DisplayName("getBudgets")
    class GetBudgetsTests {
        @Test
        @DisplayName("should return filtered budgets for user, month and year")
        void shouldReturnBudgets() {
            // arrange
            Budget budget = Budget.builder().id(BUDGET_ID).userId(USER_ID).month(3).year(2026).build();
            when(budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(USER_ID, 3, 2026)).thenReturn(List.of(budget));

            // act
            List<BudgetDto> result = budgetService.getBudgets(USER_ID, 3, 2026);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(BUDGET_ID, result.get(0).id());
        }
    }

    @Nested
    @DisplayName("getAllBudgets")
    class GetAllBudgetsTests {
        @Test
        @DisplayName("should return a page of budgets for user ordered")
        void shouldReturnAllBudgets() {
            // arrange
            Budget budget = Budget.builder().id(BUDGET_ID).userId(USER_ID).build();
            Pageable pageable = PageRequest.of(0, 20);
            when(budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(budget), pageable, 1));

            // act
            Page<BudgetDto> result = budgetService.getAllBudgets(USER_ID, pageable);

            // assert & verify
            assertEquals(1, result.getContent().size());
            assertEquals(BUDGET_ID, result.getContent().get(0).id());
        }
    }

    @Nested
    @DisplayName("getBudgetStatus")
    class GetBudgetStatusTests {
        @Test
        @DisplayName("should return budget status from repository")
        void shouldReturnStatus() {
            // arrange
            CategoryDto cat = CategoryDto.builder().name("Food").build();
            BudgetStatusDto status = new BudgetStatusDto(cat, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(9), 0.1);
            when(budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_ID, 3, 2026)).thenReturn(List.of(status));

            // act
            List<BudgetStatusDto> result = budgetService.getBudgetStatus(USER_ID, 3, 2026);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("Food", result.get(0).category().name());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {
        @Test
        @DisplayName("should throw exception when budget already exists")
        void shouldThrowWhenBudgetExists() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).userId(USER_ID).build()));
            when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                    USER_ID, CATEGORY_ID, 3, 2026)).thenReturn(Optional.of(Budget.builder().id(1L).build()));

            BudgetCreateRequest request = BudgetCreateRequest.builder().categoryId(CATEGORY_ID).month(3).year(2026).amount(BigDecimal.TEN).build();

            // act & assert & verify
            assertThrows(IllegalArgumentException.class, () -> budgetService.create(USER_ID, request));
            verify(budgetRepository, org.mockito.Mockito.never()).insert(any(BudgetCreateRequest.class));
        }

        @Test
        @DisplayName("should create budget when user and category are valid and owned")
        void shouldCreate() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).userId(USER_ID).build()));
            when(budgetRepository.insert(any(BudgetCreateRequest.class))).thenReturn(1);

            BudgetCreateRequest request = BudgetCreateRequest.builder().categoryId(CATEGORY_ID).month(3).year(2026).amount(BigDecimal.TEN).build();

            // act
            int result = budgetService.create(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(budgetRepository).insert(any(BudgetCreateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> budgetService.create(USER_ID, BudgetCreateRequest.builder().build()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if category not found")
        void shouldThrowOnCategoryNotFound() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            BudgetCreateRequest request = BudgetCreateRequest.builder().categoryId(CATEGORY_ID).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> budgetService.create(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user doesn't own category")
        void shouldThrowOnCategoryAccessDenied() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().build()));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).userId(999L).build()));

            BudgetCreateRequest request = BudgetCreateRequest.builder().categoryId(CATEGORY_ID).build();

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> budgetService.create(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {
        @Test
        @DisplayName("should update budget if owned")
        void shouldUpdate() {
            // arrange
            Budget existing = Budget.builder().id(BUDGET_ID).userId(USER_ID).build();
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));
            when(budgetRepository.update(any(BudgetUpdateRequest.class))).thenReturn(1);

            BudgetUpdateRequest request = BudgetUpdateRequest.builder().id(BUDGET_ID).amount(BigDecimal.TEN).build();

            // act
            int result = budgetService.update(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(budgetRepository).update(any(BudgetUpdateRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if budget not found")
        void shouldThrowOnNotFound() {
            // arrange
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.empty());

            BudgetUpdateRequest request = BudgetUpdateRequest.builder().id(BUDGET_ID).build();

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> budgetService.update(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own budget")
        void shouldThrowOnAccessDenied() {
            // arrange
            Budget existing = Budget.builder().id(BUDGET_ID).userId(999L).build();
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));

            BudgetUpdateRequest request = BudgetUpdateRequest.builder().id(BUDGET_ID).build();

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> budgetService.update(USER_ID, request));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {
        @Test
        @DisplayName("should delete budget if owned")
        void shouldDelete() {
            // arrange
            Budget budget = Budget.builder().id(BUDGET_ID).userId(USER_ID).build();
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));

            // act
            budgetService.delete(USER_ID, BUDGET_ID);

            // assert & verify
            verify(budgetRepository).deleteById(BUDGET_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if budget not found during delete")
        void shouldThrowOnNotFound() {
            // arrange
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.empty());

            // act & assert & verify
            assertThrows(ResourceNotFoundException.class, () -> budgetService.delete(USER_ID, BUDGET_ID));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own budget during delete")
        void shouldThrowOnAccessDenied() {
            // arrange
            Budget budget = Budget.builder().id(BUDGET_ID).userId(999L).build();
            when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));

            // act & assert & verify
            assertThrows(AccessDeniedException.class, () -> budgetService.delete(USER_ID, BUDGET_ID));
        }
    }
}
