package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CategoryRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        User newUser = new User();
        newUser.setUsername("cat_test_user");
        newUser.setEmail("cat_test@example.com");
        newUser.setPasswordHash("$2a$10$hashedpassword");
        testUser = userRepository.insert(newUser);
    }

    private Category buildCategory(String name, CategoryType type) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setIconography(new Iconography("cart", "#FF0000"));
        category.setUser(testUser);
        return category;
    }

    @Test
    void insert_ShouldCreateCategoryWithGeneratedId() {
        // When
        Category saved = categoryRepository.insert(buildCategory("Groceries", CategoryType.EXPENSE));

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Groceries");
        assertThat(saved.getType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void save_NewCategory_ShouldInsert() {
        // When
        Category saved = categoryRepository.save(buildCategory("Income", CategoryType.INCOME));

        // Then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_ExistingCategory_ShouldUpdate() {
        // Given
        Category saved = categoryRepository.insert(buildCategory("Old Name", CategoryType.EXPENSE));
        saved.setName("New Name");
        saved.setIconography(new Iconography("home", "#00FF00"));

        // When
        categoryRepository.save(saved);

        // Then
        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New Name");
        assertThat(found.get().getIconography().getIcon()).isEqualTo("home");
    }

    @Test
    void findById_ShouldReturnCategory_WhenExists() {
        // Given
        Category saved = categoryRepository.insert(buildCategory("Dining", CategoryType.EXPENSE));

        // When
        Optional<Category> found = categoryRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Dining");
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        assertThat(categoryRepository.findById(999999L)).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllCategories() {
        // Given
        long before = categoryRepository.count();
        categoryRepository.insert(buildCategory("Travel", CategoryType.EXPENSE));

        // When
        List<Category> all = categoryRepository.findAll();

        // Then
        assertThat(all).hasSizeGreaterThan((int) before);
    }

    @Test
    void findByUserId_ShouldReturnOnlyUserCategories() {
        // Given
        categoryRepository.insert(buildCategory("Cat A", CategoryType.EXPENSE));
        categoryRepository.insert(buildCategory("Cat B", CategoryType.INCOME));

        // When
        List<Category> categories = categoryRepository.findByUserId(testUser.getId());

        // Then
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Cat A", "Cat B");
    }

    @Test
    void update_ShouldPersistChanges() {
        // Given
        Category saved = categoryRepository.insert(buildCategory("Before", CategoryType.EXPENSE));
        saved.setName("After");
        saved.setType(CategoryType.INCOME);

        // When
        categoryRepository.update(saved);

        // Then
        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("After");
        assertThat(found.get().getType()).isEqualTo(CategoryType.INCOME);
    }

    @Test
    void delete_ShouldRemoveCategory() {
        // Given
        Category saved = categoryRepository.insert(buildCategory("ToDelete", CategoryType.EXPENSE));

        // When
        categoryRepository.delete(saved);

        // Then
        assertThat(categoryRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteById_ShouldRemoveCategory() {
        // Given
        Category saved = categoryRepository.insert(buildCategory("AlsoDelete", CategoryType.EXPENSE));

        // When
        categoryRepository.deleteById(saved.getId());

        // Then
        assertThat(categoryRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void count_ShouldReturnCorrectNumber() {
        // Given
        long before = categoryRepository.count();
        categoryRepository.insert(buildCategory("CountMe", CategoryType.EXPENSE));

        // When / Then
        assertThat(categoryRepository.count()).isEqualTo(before + 1);
    }
}
