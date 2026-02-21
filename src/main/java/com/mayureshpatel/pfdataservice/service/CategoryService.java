package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(CategoryDto::mapToDto).toList();
    }

    @Transactional
    public CategoryDto createCategory(Long userId, CategoryDto categoryDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = new Category();
        category.setName(categoryDto.name());
        category.setIconography(categoryDto.iconography());
        if (categoryDto.categoryType() != null) {
            category.setType(categoryDto.categoryType());
        }
        category.setUser(user);

        if (categoryDto.parent() != null) {
            Category parent = categoryRepository.findById(categoryDto.parent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (!parent.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }
        }

        return CategoryDto.mapToDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto updateCategory(Long userId, Long categoryId, CategoryDto dto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        category.setName(dto.name());
        category.setIconography(dto.iconography());
        if (dto.categoryType() != null) {
            category.setType(dto.categoryType());
        }

        if (dto.parent() != null) {
            if (dto.parent().getId().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            Category parent = categoryRepository.findById(dto.parent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (!parent.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }
        }

        return CategoryDto.mapToDto(this.categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        long transactionCount = this.transactionRepository.countByCategoryId(categoryId);
        if (transactionCount > 0) {
            throw new IllegalStateException("Cannot delete category with associated transactions. Please reassign or delete transactions first.");
        }

        categoryRepository.delete(category);
    }

    /**
     * Get categories grouped by parent for dropdown display
     */
    @Transactional(readOnly = true)
    public Map<CategoryDto, List<CategoryDto>> getCategoriesGrouped(Long userId) {
        List<Category> allCategories = categoryRepository.findByUserId(userId);

        return allCategories.stream()
                .collect(Collectors.groupingBy(
                        CategoryDto::mapToDto,
                        Collectors.mapping(CategoryDto::mapToDto, Collectors.toList()))
                )
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
                .map(CategoryDto::mapToDto).toList();
    }
}
