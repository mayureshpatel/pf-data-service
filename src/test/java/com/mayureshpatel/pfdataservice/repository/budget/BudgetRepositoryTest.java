package com.mayureshpatel.pfdataservice.repository.budget;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetCreateRequest;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetStatusDto;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(BudgetRepository.class)
@DisplayName("BudgetRepository Integration Tests (PostgreSQL)")
class BudgetRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private BudgetRepository budgetRepository;

    private static final Long USER_1 = 1L;
    private static final Long CAT_RENT = 6L; // From baseline

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should insert and find budget")
        void shouldInsertAndFind() {
            // arrange
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(USER_1)
                    .categoryId(CAT_RENT)
                    .amount(new BigDecimal("1500.00"))
                    .month(3)
                    .year(2026)
                    .build();

            // act
            int newId = budgetRepository.insert(request);
            Optional<Budget> budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 3, 2026);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count
            assertTrue(budget.isPresent());
            assertEquals(0, new BigDecimal("1500.00").compareTo(budget.get().getAmount()));
            assertEquals(budget.get().getId(), (long) newId);
        }

        @Test
        @DisplayName("should update budget amount")
        void shouldUpdate() {
            // arrange
            BudgetCreateRequest create = BudgetCreateRequest.builder()
                    .userId(USER_1).categoryId(CAT_RENT).amount(BigDecimal.TEN).month(4).year(2026).build();
            budgetRepository.insert(create);
            Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 4, 2026).orElseThrow();

            BudgetUpdateRequest update = BudgetUpdateRequest.builder()
                    .id(budget.getId())
                    .amount(new BigDecimal("20.00"))
                    .build();

            // act
            int rows = budgetRepository.update(update);

            // assert & verify
            assertEquals(1, rows);
            Budget updated = budgetRepository.findById(budget.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("20.00").compareTo(updated.getAmount()));
        }

        @Test
        @DisplayName("should soft delete budget")
        void shouldDelete() {
            // arrange
            BudgetCreateRequest create = BudgetCreateRequest.builder()
                    .userId(USER_1).categoryId(CAT_RENT).amount(BigDecimal.TEN).month(5).year(2026).build();
            budgetRepository.insert(create);
            Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 5, 2026).orElseThrow();

            // act
            int rows = budgetRepository.delete(budget);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(budgetRepository.findById(budget.getId()).isEmpty());
        }

        @Test
        @DisplayName("should handle delete by ID directly")
        void shouldDeleteById() {
            // act
            int rows = budgetRepository.deleteById(999L);
            assertEquals(0, rows);
        }
        
        @Test
        @DisplayName("should return 0 when deleting budget with no ID")
        void shouldHandleNoIdDelete() {
            assertEquals(0, budgetRepository.delete(Budget.builder().build()));
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryTests {
        @Test
        @DisplayName("should find all budgets for user ordered by period")
        void shouldFindAllForUser() {
            // arrange
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(1L).amount(BigDecimal.ONE).month(1).year(2026).build());
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(2L).amount(BigDecimal.ONE).month(2).year(2026).build());

            // act
            List<Budget> result = budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_1);

            // assert & verify
            assertTrue(result.size() >= 2);
            assertEquals(2, result.get(0).getMonth());
            assertEquals(1, result.get(1).getMonth());
        }

        @Test
        @DisplayName("PF-320: should return a Page honoring the requested page size, with total count matching the full unpaginated list")
        void shouldFindAllForUserPaged() {
            // arrange -- ground truth from the existing unpaginated method, whatever the shared
            // baseline fixture already contains for USER_1, plus these 2 new inserts
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(1L).amount(BigDecimal.ONE).month(3).year(2026).build());
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(2L).amount(BigDecimal.ONE).month(4).year(2026).build());
            int fullCount = budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_1).size();
            Pageable pageable = PageRequest.of(0, 1);

            // act
            Page<Budget> result = budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_1, pageable);

            // assert & verify
            assertEquals(1, result.getContent().size());
            assertEquals(fullCount, result.getTotalElements());
        }

        @Test
        @DisplayName("PF-320: should return every budget, not throw, when called with Pageable.unpaged()")
        void shouldFindAllForUserUnpaged() {
            // arrange -- Pageable.unpaged() throws UnsupportedOperationException from
            // getPageSize()/getOffset(); a real bug caught here once (the sibling fix in
            // MerchantRepository) before it could ever reach a real caller of this method
            int fullCount = budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_1).size();

            // act
            Page<Budget> result = budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(
                    USER_1, Pageable.unpaged());

            // assert & verify
            assertEquals(fullCount, result.getContent().size());
            assertEquals(fullCount, result.getTotalElements());
        }

        @Test
        @DisplayName("should find budgets by month and year")
        void shouldFindByMonthYear() {
            // arrange
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(1L).amount(BigDecimal.ONE).month(6).year(2026).build());

            // act
            List<Budget> result = budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(USER_1, 6, 2026);

            // assert & verify
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should count active budgets for a category, excluding soft-deleted ones")
        void shouldCountByCategoryIdExcludingDeleted() {
            // arrange
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(CAT_RENT).amount(BigDecimal.TEN).month(7).year(2026).build());
            Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 7, 2026).orElseThrow();

            // act
            long countBeforeDelete = budgetRepository.countByCategoryIdAndDeletedAtIsNull(CAT_RENT);
            budgetRepository.delete(budget);
            long countAfterDelete = budgetRepository.countByCategoryIdAndDeletedAtIsNull(CAT_RENT);

            // assert & verify
            assertEquals(1, countBeforeDelete);
            assertEquals(0, countAfterDelete);
        }

        @Test
        @DisplayName("should count zero budgets for a category with none")
        void shouldCountByCategoryIdZeroWhenNoBudget() {
            // act -- category 2 (Food) has no budget inserted anywhere in this test class
            long count = budgetRepository.countByCategoryIdAndDeletedAtIsNull(2L);

            // assert & verify
            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("Budget Status (Aggregation)")
    class StatusTests {
        @Test
        @DisplayName("should calculate budget status including spending from baseline")
        void shouldCalculateStatus() {
            // arrange
            budgetRepository.insert(BudgetCreateRequest.builder()
                    .userId(USER_1)
                    .categoryId(CAT_RENT)
                    .amount(new BigDecimal("2000.00"))
                    .month(9)
                    .year(2025)
                    .build());

            // act
            List<BudgetStatusDto> status = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_1, 9, 2025);

            // assert & verify
            assertFalse(status.isEmpty());
            BudgetStatusDto rentStatus = status.stream()
                    .filter(s -> s.category().name().equals("Rent"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(0, new BigDecimal("2000.00").compareTo(rentStatus.budgetedAmount()));
            assertEquals(0, new BigDecimal("1500.00").compareTo(rentStatus.spentAmount()));
            assertEquals(0, new BigDecimal("500.00").compareTo(rentStatus.remainingAmount()));
            assertEquals(75.0, rentStatus.percentageUsed());
        }

        @Test
        @DisplayName("should include unbudgeted categories with spending")
        void shouldIncludeUnbudgeted() {
            // act
            List<BudgetStatusDto> status = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_1, 10, 2025);

            // assert & verify
            assertTrue(status.stream().anyMatch(s -> s.category().name().equals("Rent") && s.budgetedAmount().compareTo(BigDecimal.ZERO) == 0));
        }
    }
}
