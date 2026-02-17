package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class BudgetRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        User newUser = new User();
        newUser.setUsername("budget_test_user");
        newUser.setEmail("budget_test@example.com");
        newUser.setPasswordHash("$2a$10$hashedpassword");
        testUser = userRepository.insert(newUser);

        Category cat = new Category();
        cat.setName("Groceries");
        cat.setType(CategoryType.EXPENSE);
        cat.setIconography(new Iconography("cart", "#FF0000"));
        cat.setUser(testUser);
        testCategory = categoryRepository.insert(cat);
    }

    private Budget buildBudget(BigDecimal amount, int month, int year) {
        Budget budget = new Budget();
        budget.setUser(testUser);
        budget.setCategory(testCategory);
        budget.setAmount(amount);
        budget.setMonth(month);
        budget.setYear(year);
        return budget;
    }

    @Test
    void insert_ShouldCreateBudgetWithGeneratedId() {
        // When
        Budget saved = budgetRepository.insert(buildBudget(new BigDecimal("500.00"), 1, 2024));

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("500.00");
        assertThat(saved.getMonth()).isEqualTo(1);
        assertThat(saved.getYear()).isEqualTo(2024);
    }

    @Test
    void save_NewBudget_ShouldInsert() {
        // When
        Budget saved = budgetRepository.save(buildBudget(new BigDecimal("300.00"), 2, 2024));

        // Then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_ExistingBudget_ShouldUpdate() {
        // Given
        Budget saved = budgetRepository.insert(buildBudget(new BigDecimal("400.00"), 3, 2024));
        saved.setAmount(new BigDecimal("600.00"));

        // When
        budgetRepository.save(saved);

        // Then
        Optional<Budget> found = budgetRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("600.00");
    }

    @Test
    void findById_ShouldReturnBudget_WhenExists() {
        // Given
        Budget saved = budgetRepository.insert(buildBudget(new BigDecimal("500.00"), 4, 2024));

        // When
        Optional<Budget> found = budgetRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("500.00");
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(found.get().getCategory().getId()).isEqualTo(testCategory.getId());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        assertThat(budgetRepository.findById(999999L)).isEmpty();
    }

    @Test
    void findByUserIdAndMonthAndYear_ShouldReturnMatchingBudgets() {
        // Given
        budgetRepository.insert(buildBudget(new BigDecimal("500.00"), 5, 2024));
        budgetRepository.insert(buildBudget(new BigDecimal("200.00"), 6, 2024)); // different month

        // When
        List<Budget> budgets = budgetRepository
                .findByUserIdAndMonthAndYearAndDeletedAtIsNull(testUser.getId(), 5, 2024);

        // Then
        assertThat(budgets).hasSize(1);
        assertThat(budgets.get(0).getMonth()).isEqualTo(5);
    }

    @Test
    void findByUserIdAndMonthAndYear_ShouldReturnEmpty_WhenNoMatch() {
        List<Budget> budgets = budgetRepository
                .findByUserIdAndMonthAndYearAndDeletedAtIsNull(testUser.getId(), 12, 1999);
        assertThat(budgets).isEmpty();
    }

    @Test
    void findByUserIdOrderedByYearDescMonthDesc_ShouldReturnSortedResults() {
        // Given
        budgetRepository.insert(buildBudget(new BigDecimal("100.00"), 1, 2023));
        budgetRepository.insert(buildBudget(new BigDecimal("200.00"), 3, 2024));
        budgetRepository.insert(buildBudget(new BigDecimal("300.00"), 1, 2024));

        // When
        List<Budget> budgets = budgetRepository
                .findByUserIdAndDeletedAtIsNullOrderByYearDescMonthDesc(testUser.getId());

        // Then
        assertThat(budgets).hasSizeGreaterThanOrEqualTo(3);
        // Most recent year+month should come first
        assertThat(budgets.get(0).getYear()).isGreaterThanOrEqualTo(budgets.get(budgets.size() - 1).getYear());
    }

    @Test
    void findByUserIdAndCategoryIdAndMonthAndYear_ShouldReturnBudget() {
        // Given
        budgetRepository.insert(buildBudget(new BigDecimal("500.00"), 7, 2024));

        // When
        Optional<Budget> found = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                        testUser.getId(), testCategory.getId(), 7, 2024);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void findByUserIdAndCategoryIdAndMonthAndYear_ShouldReturnEmpty_WhenNoMatch() {
        Optional<Budget> found = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYearAndDeletedAtIsNull(
                        testUser.getId(), testCategory.getId(), 1, 1999);
        assertThat(found).isEmpty();
    }

    @Test
    void update_ShouldPersistNewAmount() {
        // Given
        Budget saved = budgetRepository.insert(buildBudget(new BigDecimal("100.00"), 8, 2024));
        saved.setAmount(new BigDecimal("999.00"));

        // When
        budgetRepository.update(saved);

        // Then
        Optional<Budget> found = budgetRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void delete_ShouldSoftDelete() {
        // Given
        Budget saved = budgetRepository.insert(buildBudget(new BigDecimal("200.00"), 9, 2024));

        // When
        budgetRepository.delete(saved);

        // Then
        assertThat(budgetRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteById_ShouldSoftDelete() {
        // Given
        Budget saved = budgetRepository.insert(buildBudget(new BigDecimal("300.00"), 10, 2024));

        // When
        budgetRepository.deleteById(saved.getId());

        // Then
        assertThat(budgetRepository.findById(saved.getId())).isEmpty();
    }
}
