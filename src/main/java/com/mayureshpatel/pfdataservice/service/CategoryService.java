package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryDto;
import com.mayureshpatel.pfdataservice.model.Category;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(Long userId, CategoryDto categoryDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Category category = new Category();
        category.setName(categoryDto.name());
        category.setColor(categoryDto.color());
        category.setIcon(categoryDto.icon());
        if (categoryDto.type() != null) {
            category.setType(categoryDto.type());
        }
        category.setUser(user);

        if (categoryDto.parentId() != null) {
            Category parent = categoryRepository.findById(categoryDto.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found"));
            if (!parent.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }
            category.setParent(parent);
        }

        return mapToDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto updateCategory(Long userId, Long categoryId, CategoryDto dto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        category.setName(dto.name());
        category.setColor(dto.color());
        category.setIcon(dto.icon());
        if (dto.type() != null) {
            category.setType(dto.type());
        }
        
        if (dto.parentId() != null) {
            // Prevent circular dependency (simple check: parent cannot be self)
            if (dto.parentId().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            
            Category parent = categoryRepository.findById(dto.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found"));
            if (!parent.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Access denied to parent category");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        
        return mapToDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        long transactionCount = transactionRepository.countByCategoryId(categoryId);
        if (transactionCount > 0) {
            throw new IllegalStateException("Cannot delete category with associated transactions. Please reassign or delete transactions first.");
        }
        
        // Prevent deleting if it has children? Or cascade?
        // JPA CascadeType.ALL on subCategories might handle it, but usually we want to prevent orphans.
        // Let's assume for now user must delete children first or we explicitly check.
        if (!category.getSubCategories().isEmpty()) {
             throw new IllegalStateException("Cannot delete category that has sub-categories.");
        }

        categoryRepository.delete(category);
    }

    private CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.getType(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getParent() != null ? category.getParent().getName() : null
        );
    }
}
