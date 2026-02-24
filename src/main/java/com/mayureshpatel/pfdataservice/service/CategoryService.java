package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.CategoryDto;
import com.mayureshpatel.pfdataservice.domain.user.User;
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
    public List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(com.mayureshpatel.pfdataservice.dto.category.CategoryDto::mapToDto).toList();
    }

    @Transactional
    public com.mayureshpatel.pfdataservice.dto.category.CategoryDto createCategory(Long userId, com.mayureshpatel.pfdataservice.dto.category.CategoryDto categoryDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CategoryDto category = new CategoryDto();
        category.setName(categoryDto.name());
        category.setIconography(categoryDto.iconography());
        category.setType(categoryDto.categoryType());
        category.setUser(user);

        if (categoryDto.parent() != null) {
            CategoryDto parent = categoryRepository.findById(categoryDto.parent().id())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (!parent.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }

            category.setParent(parent);
        }

        return com.mayureshpatel.pfdataservice.dto.category.CategoryDto.mapToDto(categoryRepository.save(category));
    }

    @Transactional
    public com.mayureshpatel.pfdataservice.dto.category.CategoryDto updateCategory(Long userId, Long categoryId, com.mayureshpatel.pfdataservice.dto.category.CategoryDto dto) {
        CategoryDto category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        category.setName(dto.name());
        category.setIconography(dto.iconography());
        category.setType(dto.categoryType());

        if (dto.parent() != null) {
            if (dto.parent().id().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            CategoryDto parent = categoryRepository.findById(dto.parent().id())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (!parent.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }

            category.setParent(parent);
        }

        return com.mayureshpatel.pfdataservice.dto.category.CategoryDto.mapToDto(this.categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        CategoryDto category = this.categoryRepository.findById(categoryId)
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
     * Gets a map of categories grouped by parent-child relationship.
     *
     * @param userId the user id to get categories for
     * @return map of {@link com.mayureshpatel.pfdataservice.dto.category.CategoryDto} grouped by parent-child relationship
     */
    @Transactional(readOnly = true)
    public Map<com.mayureshpatel.pfdataservice.dto.category.CategoryDto, List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto>> getCategoriesGrouped(Long userId) {
        List<CategoryDto> allCategories = categoryRepository.findByUserId(userId);

        return allCategories.stream()
                .collect(Collectors.groupingBy(
                        com.mayureshpatel.pfdataservice.dto.category.CategoryDto::mapToDto,
                        Collectors.mapping(com.mayureshpatel.pfdataservice.dto.category.CategoryDto::mapToDto, Collectors.toList()))
                )
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get all sub-categories for a given user.
     *
     * @param userId the user id to get categories for
     * @return list of {@link com.mayureshpatel.pfdataservice.dto.category.CategoryDto}
     */
    @Transactional(readOnly = true)
    public List<com.mayureshpatel.pfdataservice.dto.category.CategoryDto> getChildCategories(Long userId) {
        return this.categoryRepository.findAllSubCategories(userId).stream()
                .map(com.mayureshpatel.pfdataservice.dto.category.CategoryDto::mapToDto).toList();
    }
}
