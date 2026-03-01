package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryDto(
        Long id,
        Long userId,
        
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be less than 100 characters")
        String name,
        
        @NotNull(message = "Category type is required")
        CategoryType categoryType,
        
        CategoryDto parent,
        
        @Size(max = 50, message = "Icon name must be less than 50 characters")
        String icon,
        
        @Size(max = 50, message = "Color must be less than 50 characters")
        String color
) {
}
