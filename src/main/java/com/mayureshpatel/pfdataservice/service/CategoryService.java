package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.CategoryDtoMapper;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(CategoryDtoMapper::toDto).toList();
    }

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

    @Transactional
    public int updateCategory(Long userId, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.getParentId() != null) {
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

        CategoryUpdateRequest securedRequest = request.toBuilder()
                .userId(userId)
                .build();
        return this.categoryRepository.update(securedRequest);
    }

    @Transactional
    public int deleteCategory(Long userId, Long categoryId) {
        Category category = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        long transactionCount = this.transactionRepository.countByCategoryId(categoryId);
        if (transactionCount > 0) {
            throw new IllegalStateException("Cannot delete category with associated transactions. Please reassign or delete transactions first.");
        }

        return categoryRepository.delete(category);
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
