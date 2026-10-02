package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.repository.budget.BudgetRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRuleRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for a user's category hierarchy (parent categories with optional subcategories). A
 * category can't be deleted while transactions still reference it, and can't be assigned to
 * itself as its own parent.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final BudgetRepository budgetRepository;

    /**
     * Returns all categories for a user, flat (not grouped by parent).
     *
     * @param userId the user id
     * @return the user's categories
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(CategoryDtoMapper::toDto).toList();
    }

    /**
     * Creates a new category, verifying the parent (if given) exists and belongs to the user.
     *
     * @param userId  the user id
     * @param request the category to create
     * @return the new category's generated id
     */
    @Transactional
    public int createCategory(Long userId, CategoryCreateRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // if has parent category, check if it exists and belongs to the user
        if (request.getParentId() != null && request.getParentId() != 0) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (!parent.getUserId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }
        }

        CategoryCreateRequest securedRequest = request.toBuilder()
                .userId(userId)
                .build();
        return categoryRepository.insert(securedRequest);
    }

    /**
     * Updates an existing category owned by the user. A category can't be made its own parent,
     * and a given parent (if changed) must exist and belong to the user.
     *
     * @param userId  the user id
     * @param request the category to update, including its id
     * @return the number of rows updated
     */
    @Transactional
    public int updateCategory(Long userId, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        validateNewParent(userId, request);

        CategoryUpdateRequest securedRequest = request.toBuilder()
                .userId(userId)
                .build();
        return this.categoryRepository.update(securedRequest);
    }

    /**
     * Validates the new parent, if {@code request} is actually changing it (PF-809: extracted
     * from {@link #updateCategory}): it can't be zero, can't be the category's own id, and must
     * exist and belong to {@code userId}.
     */
    private void validateNewParent(Long userId, CategoryUpdateRequest request) {
        if (request.getParentId() == null) {
            return;
        }

        if (request.getParentId() == 0) {
            throw new IllegalArgumentException("Parent category ID cannot be zero.");
        }

        if (request.getParentId().equals(request.getId())) {
            throw new IllegalArgumentException("Category cannot be its own parent");
        }

        Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        if (!parent.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to parent category");
        }
    }

    /**
     * Deletes a category owned by the user. Refuses to delete a category that still has
     * subcategories, transactions, category rules, or a budget assigned to it — each is a
     * dependent record that would otherwise be left pointing at a deleted category.
     *
     * @param userId     the user id
     * @param categoryId the category id to delete
     * @return the number of rows deleted
     * @throws IllegalStateException if the category still has subcategories, transactions,
     *                                category rules, or a budget assigned to it
     */
    @Transactional
    public int deleteCategory(Long userId, Long categoryId) {
        Category category = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        requireNoDependents(categoryId);

        return categoryRepository.delete(category);
    }

    /**
     * Refuses to delete a category that still has subcategories, transactions, category rules, or
     * a budget assigned to it (PF-809: extracted from {@link #deleteCategory}) -- each is a
     * dependent record that would otherwise be left pointing at a deleted category.
     */
    private void requireNoDependents(Long categoryId) {
        requireZeroCount(this.categoryRepository.countByParentId(categoryId),
                "Cannot delete category with subcategories. Please reassign or delete subcategories first.");
        requireZeroCount(this.transactionRepository.countByCategoryId(categoryId),
                "Cannot delete category with associated transactions. Please reassign or delete transactions first.");
        requireZeroCount(this.categoryRuleRepository.countByCategoryId(categoryId),
                "Cannot delete category with associated category rules. Please reassign or delete those rules first.");
        requireZeroCount(this.budgetRepository.countByCategoryIdAndDeletedAtIsNull(categoryId),
                "Cannot delete category with an associated budget. Please delete the budget first.");
    }

    private void requireZeroCount(long count, String errorMessage) {
        if (count > 0) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Gets a map of categories grouped by parent-child relationship.
     *
     * @param userId the user id to get categories for
     * @return list of {@link CategoryDto} grouped by parent-child relationship
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getParentCategories(Long userId) {
        return categoryRepository.findAllParentCategories(userId)
                .stream().map(CategoryDtoMapper::toDto).toList();
    }

    /**
     * Get all sub-categories for a given user.
     *
     * @param userId the user id to get categories for
     * @return list of {@link CategoryDto}
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getChildCategories(Long userId) {
        return this.categoryRepository.findAllSubCategories(userId).stream()
                .map(CategoryDtoMapper::toDto).toList();
    }
}
