package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.CategoryDto;
import com.mayureshpatel.pfdataservice.model.Category;
import com.mayureshpatel.pfdataservice.model.User;
import com.mayureshpatel.pfdataservice.repository.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

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
        category.setUser(user);

        return mapToDto(categoryRepository.save(category));
    }

    private CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName()
        );
    }
}
