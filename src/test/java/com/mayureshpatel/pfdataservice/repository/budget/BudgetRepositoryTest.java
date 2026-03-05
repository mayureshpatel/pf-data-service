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
            // Arrange
            BudgetCreateRequest request = BudgetCreateRequest.builder()
                    .userId(USER_1)
                    .categoryId(CAT_RENT)
                    .amount(new BigDecimal("1500.00"))
                    .month(3)
                    .year(2026)
                    .build();

            // Act
            int rows = budgetRepository.insert(request);
            Optional<Budget> budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 3, 2026);

            // Assert
            assertEquals(1, rows);
            assertTrue(budget.isPresent());
            assertEquals(0, new BigDecimal("1500.00").compareTo(budget.get().getAmount()));
        }

        @Test
        @DisplayName("should update budget amount")
        void shouldUpdate() {
            // Arrange
            BudgetCreateRequest create = BudgetCreateRequest.builder()
                    .userId(USER_1).categoryId(CAT_RENT).amount(BigDecimal.TEN).month(4).year(2026).build();
            budgetRepository.insert(create);
            Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 4, 2026).orElseThrow();

            BudgetUpdateRequest update = BudgetUpdateRequest.builder()
                    .id(budget.getId())
                    .amount(new BigDecimal("20.00"))
                    .build();

            // Act
            int rows = budgetRepository.update(update);

            // Assert
            assertEquals(1, rows);
            Budget updated = budgetRepository.findById(budget.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("20.00").compareTo(updated.getAmount()));
        }

        @Test
        @DisplayName("should soft delete budget")
        void shouldDelete() {
            // Arrange
            BudgetCreateRequest create = BudgetCreateRequest.builder()
                    .userId(USER_1).categoryId(CAT_RENT).amount(BigDecimal.TEN).month(5).year(2026).build();
            budgetRepository.insert(create);
            Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(USER_1, CAT_RENT, 5, 2026).orElseThrow();

            // Act
            int rows = budgetRepository.delete(budget);

            // Assert
            assertEquals(1, rows);
            assertTrue(budgetRepository.findById(budget.getId()).isEmpty());
        }

        @Test
        @DisplayName("should handle delete by ID directly")
        void shouldDeleteById() {
            // Act
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
            // Arrange
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(1L).amount(BigDecimal.ONE).month(1).year(2026).build());
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(2L).amount(BigDecimal.ONE).month(2).year(2026).build());

            // Act
            List<Budget> result = budgetRepository.findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(USER_1);

            // Assert
            assertTrue(result.size() >= 2);
            assertEquals(2, result.get(0).getMonth());
            assertEquals(1, result.get(1).getMonth());
        }

        @Test
        @DisplayName("should find budgets by month and year")
        void shouldFindByMonthYear() {
            // Arrange
            budgetRepository.insert(BudgetCreateRequest.builder().userId(USER_1).categoryId(1L).amount(BigDecimal.ONE).month(6).year(2026).build());

            // Act
            List<Budget> result = budgetRepository.findByUserIdAndMonthAndYearAndDeletedAtIsNull(USER_1, 6, 2026);

            // Assert
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Budget Status (Aggregation)")
    class StatusTests {
        @Test
        @DisplayName("should calculate budget status including spending from baseline")
        void shouldCalculateStatus() {
            // Arrange
            budgetRepository.insert(BudgetCreateRequest.builder()
                    .userId(USER_1)
                    .categoryId(CAT_RENT)
                    .amount(new BigDecimal("2000.00"))
                    .month(9)
                    .year(2025)
                    .build());

            // Act
            List<BudgetStatusDto> status = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_1, 9, 2025);

            // Assert
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
            // Act
            List<BudgetStatusDto> status = budgetRepository.findBudgetStatusByUserIdAndMonthAndYear(USER_1, 10, 2025);

            // Assert
            assertTrue(status.stream().anyMatch(s -> s.category().name().equals("Rent") && s.budgetedAmount().compareTo(BigDecimal.ZERO) == 0));
        }
    }
}
