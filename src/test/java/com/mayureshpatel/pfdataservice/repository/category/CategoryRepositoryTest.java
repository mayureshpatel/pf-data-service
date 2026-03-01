package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("CategoryRepository Integration Tests")
class CategoryRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestDataFactory factory;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = factory.createUser("catrepo_" + System.currentTimeMillis());
    }

    @Test
    @DisplayName("save() should insert new category and assign ID")
    void save_shouldInsertNewCategory() {
        Category category = factory.createCategory(testUser, "Groceries", CategoryType.EXPENSE);

        assertThat(category.getId()).isNotNull();
    }

    @Test
    @DisplayName("findByUserId() should return all categories for user")
    void findByUserId_shouldReturnUserCategories() {
        factory.createCategory(testUser, "Food", CategoryType.EXPENSE);
        factory.createCategory(testUser, "Salary", CategoryType.INCOME);

        List<Category> categories = categoryRepository.findByUserId(testUser.getId());

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder("Food", "Salary");
    }

    @Test
    @DisplayName("findByUserId() should return empty when user has no categories")
    void findByUserId_shouldReturnEmpty() {
        User otherUser = factory.createUser("nocats_" + System.currentTimeMillis());

        List<Category> categories = categoryRepository.findByUserId(otherUser.getId());

        assertThat(categories).isEmpty();
    }

    @Test
    @DisplayName("update() should modify existing category")
    void update_shouldModifyCategory() {
        Category category = factory.createCategory(testUser, "Old Name", CategoryType.EXPENSE);

        category.setName("New Name");
        categoryRepository.update(category);

        // Verify via findByUserId since findById has a known bug (requires userId param)
        List<Category> categories = categoryRepository.findByUserId(testUser.getId());
        assertThat(categories).extracting(Category::getName).containsExactly("New Name");
    }

    @Test
    @DisplayName("delete() should remove category")
    void delete_shouldRemoveCategory() {
        Category category = factory.createCategory(testUser, "ToDelete", CategoryType.EXPENSE);

        categoryRepository.delete(category);

        List<Category> remaining = categoryRepository.findByUserId(testUser.getId());
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("findAllSubCategories() should return subcategories for user")
    void findAllSubCategories_shouldReturnSubCategories() {
        Category parent = factory.createCategory(testUser, "Parent", CategoryType.EXPENSE);

        Category child = new Category();
        child.setUser(testUser);
        child.setName("Child");
        child.setType(CategoryType.EXPENSE);
        child.setParent(parent);
        child.setIconography(new com.mayureshpatel.pfdataservice.domain.Iconography("icon", "color"));
        child.setAudit(new com.mayureshpatel.pfdataservice.domain.TimestampAudit());
        categoryRepository.save(child);

        List<Category> subCategories = categoryRepository.findAllSubCategories(testUser.getId());

        assertThat(subCategories).extracting(Category::getName).contains("Child");
    }

    @Test
    @DisplayName("findAllWIthParent() should return categories with parent info")
    void findAllWithParent_shouldReturnCategoriesWithParent() {
        Category parent = factory.createCategory(testUser, "Parent Cat", CategoryType.EXPENSE);

        Category child = new Category();
        child.setUser(testUser);
        child.setName("Child Cat");
        child.setType(CategoryType.EXPENSE);
        child.setParent(parent);
        child.setIconography(new com.mayureshpatel.pfdataservice.domain.Iconography("icon", "color"));
        child.setAudit(new com.mayureshpatel.pfdataservice.domain.TimestampAudit());
        categoryRepository.save(child);

        List<Category> categories = categoryRepository.findAllWIthParent(testUser.getId());

        assertThat(categories).isNotEmpty();
    }

    @Test
    @DisplayName("data isolation - user cannot see other user's categories")
    void dataIsolation_shouldIsolateByUser() {
        factory.createCategory(testUser, "User1 Cat", CategoryType.EXPENSE);

        User otherUser = factory.createUser("othercat_" + System.currentTimeMillis());
        factory.createCategory(otherUser, "User2 Cat", CategoryType.INCOME);

        List<Category> user1Cats = categoryRepository.findByUserId(testUser.getId());
        List<Category> user2Cats = categoryRepository.findByUserId(otherUser.getId());

        assertThat(user1Cats).extracting(Category::getName).containsExactly("User1 Cat");
        assertThat(user2Cats).extracting(Category::getName).containsExactly("User2 Cat");
    }
}
